import * as functions from "firebase-functions/v2/firestore";
import * as httpsV2 from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {RUNTIME_CONFIG} from "../../core/constants";

/**
 * Firebase Function for general message notifications
 * Sends notifications for new messages (not just mentions)
 * @param {any} messageData
 * @param {string} messageId
 */
export const onNewMessageNotification = functions.onDocumentCreated(
  {
    document: "messages/{messageId}",
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (event) => {
    const messageId = event.params?.messageId;
    const messageData = event.data?.data();

    if (!messageData || !messageId) {
      console.log(`No data found for message ${messageId}`);
      return null;
    }

    console.log(`Processing new message notification for ${messageId}`);

    try {
      // Skip if this is a DM - handle differently
      if (messageData.type === "dm") {
        await processDMNotification(messageData, messageId);
      } else {
        // Handle project channel messages
        await processChannelNotification(messageData, messageId);
      }

      console.log(`Successfully processed message notification for ${messageId}`);
      return null;
    } catch (error) {
      console.error(`Error processing message notification for ${messageId}:`, error);
      throw error;
    }
  });

async function processDMNotification(
  messageData: any,
  messageId: string
): Promise<void> {
  console.log(`Processing DM notification for message ${messageId}`);

  const senderId = messageData.senderId;
  const channelId = messageData.channelId;

  if (!channelId) {
    console.warn(`No channelId found for DM message ${messageId}`);
    return;
  }

  try {
    // Get DM channel participants
    const dmChannelDoc = await admin.firestore()
      .collection("dm_channels")
      .doc(channelId)
      .get();

    if (!dmChannelDoc.exists) {
      console.warn(`DM channel ${channelId} not found`);
      return;
    }

    const dmChannelData = dmChannelDoc.data();
    const participants = dmChannelData?.participants || [];

    // Find the recipient (not the sender)
    const recipientId = participants.find((id: string) => id !== senderId);

    if (!recipientId) {
      console.warn(`No recipient found for DM ${channelId}`);
      return;
    }

    // Send notification to recipient
    await sendMessageNotification(recipientId, messageData, messageId, "dm");
  } catch (error) {
    console.error("Error processing DM notification:", error);
    throw error;
  }
}

async function processChannelNotification(
  messageData: any,
  messageId: string
): Promise<void> {
  console.log(`Processing channel notification for message ${messageId}`);

  const projectId = messageData.projectId;
  const channelId = messageData.channelId;
  const senderId = messageData.senderId;

  if (!projectId || !channelId) {
    console.warn(`Missing projectId or channelId for message ${messageId}`);
    return;
  }

  try {
    // Get all project members (except sender)
    const membersQuery = await admin.firestore()
      .collection("projects")
      .doc(projectId)
      .collection("members")
      .where("userId", "!=", senderId)
      .get();

    if (membersQuery.empty) {
      console.log(`No other members found in project ${projectId}`);
      return;
    }

    // Send notifications to all members
    const notificationPromises = membersQuery.docs.map(async (memberDoc) => {
      const memberData = memberDoc.data();
      const userId = memberData.userId;

      return sendMessageNotification(userId, messageData, messageId, "channel");
    });

    await Promise.all(notificationPromises);
    console.log(`Sent channel notifications for message ${messageId}`);
  } catch (error) {
    console.error("Error processing channel notification:", error);
    throw error;
  }
}

async function sendMessageNotification(
  userId: string,
  messageData: any,
  messageId: string,
  type: "dm" | "channel"
): Promise<void> {
  try {
    // Get user's FCM token
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(userId)
      .get();

    if (!userDoc.exists) {
      console.warn(`User ${userId} not found`);
      return;
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      console.log(`No FCM token found for user ${userId}`);
      return;
    }

    // Get sender information
    const senderDoc = await admin.firestore()
      .collection("users")
      .doc(messageData.senderId)
      .get();

    const senderData = senderDoc.exists ? senderDoc.data() : null;
    const senderName = senderData?.name || "Someone";

    // Create notification payload based on type
    const title = type === "dm" ? `${senderName}님으로부터 메시지` : `${senderName}님이 메시지를 보냈습니다`;

    const notificationPayload = {
      notification: {
        title: title,
        body: truncateMessage(messageData.content, 100),
        icon: "ic_stat_ic_notification",
        sound: "default",
      },
      data: {
        type: type === "dm" ? "dm_message" : "channel_message",
        messageId: messageId,
        senderId: messageData.senderId,
        senderName: senderName,
        channelId: messageData.channelId || "",
        projectId: messageData.projectId || "",
      },
    };

    // Send notification
    await admin.messaging().sendToDevice(fcmToken, notificationPayload);
    console.log(`Sent ${type} message notification to user ${userId}`);
  } catch (error) {
    console.error(`Error sending ${type} notification to user ${userId}:`, error);
    // Don't throw here to prevent other notifications from failing
  }
}


export const updateFcmToken = httpsV2.onCall({
  region: RUNTIME_CONFIG.REGION,
}, async (request) => {
  // Verify authentication
  if (!request.auth) {
    throw new httpsV2.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const {token} = request.data;
  const userId = request.auth.uid;

  if (!token) {
    throw new httpsV2.HttpsError(
      "invalid-argument",
      "Missing required parameter: token"
    );
  }

  try {
    // Update user's FCM token in Firestore
    await admin.firestore()
      .collection("users")
      .doc(userId)
      .update({
        fcmToken: token,
        fcmTokenUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

    console.log(`Updated FCM token for user ${userId}`);
    return {success: true, message: "FCM token updated successfully"};
  } catch (error) {
    console.error(`Error updating FCM token for user ${userId}:`, error);
    throw new httpsV2.HttpsError(
      "internal",
      "Failed to update FCM token"
    );
  }
});

function truncateMessage(content: string, maxLength: number): string {
  if (!content) return "";

  if (content.length <= maxLength) {
    return content;
  }

  return content.substring(0, maxLength - 3) + "...";
}
