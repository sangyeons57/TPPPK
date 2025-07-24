import * as functions from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {RUNTIME_CONFIG} from "../../core/constants";


export const onMessageMentionNotification = functions.onDocumentCreated({
  document: "messages/{messageId}",
  region: RUNTIME_CONFIG.REGION,
  memory: RUNTIME_CONFIG.MEMORY,
  timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
}, async (event) => {
  const messageId = event.params?.messageId;
  const messageData = event.data?.data();

  if (!messageData || !messageId) {
    console.log(`No data found for message ${messageId}`);
    return null;
  }

  const mentions = messageData.mentions || [];
  if (mentions.length === 0) {
    console.log(`No mentions found in message ${messageId}`);
    return null;
  }

  console.log(`Processing ${mentions.length} mentions for message ${messageId}`);

  try {
    const notificationPromises = mentions.map(async (mention: any) => {
      return processMention(mention, messageData, messageId);
    });

    await Promise.all(notificationPromises);

    console.log(`Successfully processed all mentions for message ${messageId}`);
    return null;
  } catch (error) {
    console.error(`Error processing mentions for message ${messageId}:`, error);
    throw error;
  }
});

async function processMention(
  mention: any,
  messageData: any,
  messageId: string
): Promise<void> {
  const mentionType = mention.type;
  const mentionId = mention.id;
  const displayName = mention.displayName;

  console.log(`Processing mention: ${mentionType}:${mentionId} (${displayName})`);

  try {
    if (mentionType === "USER") {
      await procesUserMention(mentionId, messageData, messageId, displayName);
    } else if (mentionType === "ROLE") {
      await processRoleMention(mentionId, messageData, messageId, displayName);
    } else {
      console.warn(`Unknown mention type: ${mentionType}`);
    }
  } catch (error) {
    console.error(`Error processing mention ${mentionType}:${mentionId}:`, error);
    // Don't throw here to prevent other mentions from failing
  }
}

async function procesUserMention(
  userId: string,
  messageData: any,
  messageId: string,
  displayName: string
): Promise<void> {
  console.log(`Processing user mention for userId: ${userId}`);

  if (userId === messageData.senderId) {
    console.log(`User ${userId} mentioned themselves, skipping notification`);
    return;
  }

  try {
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

    const senderDoc = await admin.firestore()
      .collection("users")
      .doc(messageData.senderId)
      .get();

    const senderData = senderDoc.exists ? senderDoc.data() : null;
    const senderName = senderData?.name || "Someone";

    const notificationPayload = {
      notification: {
        title: `${senderName}님이 회원님을 멘션했습니다`,
        body: truncateMessage(messageData.content, 100),
        icon: "ic_stat_ic_notification",
        sound: "default",
      },
      data: {
        type: "mention",
        messageId: messageId,
        senderId: messageData.senderId,
        senderName: senderName,
        channelId: messageData.channelId || "",
        projectId: messageData.projectId || "",
        mentionType: "USER",
        mentionId: userId,
      },
    };

    await admin.messaging().sendToDevice(fcmToken, notificationPayload);
    console.log(`Sent mention notification to user ${userId}`);
  } catch (error) {
    console.error(`Error sending notification to user ${userId}:`, error);
    throw error;
  }
}

async function processRoleMention(
  roleId: string,
  messageData: any,
  messageId: string,
  displayName: string
): Promise<void> {
  console.log(`Processing role mention for roleId: ${roleId}`);

  try {
    if (roleId === "everyone") {
      await processEveryoneMention(messageData, messageId, displayName);
      return;
    }

    const projectId = messageData.projectId;
    if (!projectId) {
      console.warn(`No projectId found for role mention ${roleId}`);
      return;
    }

    const membersQuery = await admin.firestore()
      .collection("projects")
      .doc(projectId)
      .collection("members")
      .where("roleIds", "array-contains", roleId)
      .get();

    if (membersQuery.empty) {
      console.log(`No members found with role ${roleId} in project ${projectId}`);
      return;
    }

    const memberNotificationPromises = membersQuery.docs.map(async (memberDoc) => {
      const memberData = memberDoc.data();
      const userId = memberData.userId;

      if (userId === messageData.senderId) {
        return;
      }

      return sendRoleNotificationToUser(userId, roleId, displayName, messageData, messageId);
    });

    await Promise.all(memberNotificationPromises);
    console.log(`Sent role mention notifications for role ${roleId}`);
  } catch (error) {
    console.error(`Error processing role mention ${roleId}:`, error);
    throw error;
  }
}

async function processEveryoneMention(
  messageData: any,
  messageId: string,
  displayName: string
): Promise<void> {
  console.log("Processing @everyone mention");

  const projectId = messageData.projectId;
  if (!projectId) {
    console.warn("No projectId found for @everyone mention");
    return;
  }

  try {
    const membersQuery = await admin.firestore()
      .collection("projects")
      .doc(projectId)
      .collection("members")
      .get();

    if (membersQuery.empty) {
      console.log(`No members found in project ${projectId}`);
      return;
    }

    const memberNotificationPromises = membersQuery.docs.map(async (memberDoc) => {
      const memberData = memberDoc.data();
      const userId = memberData.userId;

      if (userId === messageData.senderId) {
        return;
      }

      return sendRoleNotificationToUser(userId, "everyone", displayName, messageData, messageId);
    });

    await Promise.all(memberNotificationPromises);
    console.log("Sent @everyone mention notifications");
  } catch (error) {
    console.error("Error processing @everyone mention:", error);
    throw error;
  }
}

async function sendRoleNotificationToUser(
  userId: string,
  roleId: string,
  roleName: string,
  messageData: any,
  messageId: string
): Promise<void> {
  try {
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(userId)
      .get();

    if (!userDoc.exists) {
      console.warn(`User ${userId} not found for role notification`);
      return;
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      console.log(`No FCM token found for user ${userId} (role mention)`);
      return;
    }

    const senderDoc = await admin.firestore()
      .collection("users")
      .doc(messageData.senderId)
      .get();

    const senderData = senderDoc.exists ? senderDoc.data() : null;
    const senderName = senderData?.name || "Someone";

    const notificationPayload = {
      notification: {
        title: `${senderName}님이 @${roleName}을 멘션했습니다`,
        body: truncateMessage(messageData.content, 100),
        icon: "ic_stat_ic_notification",
        sound: "default",
      },
      data: {
        type: "mention",
        messageId: messageId,
        senderId: messageData.senderId,
        senderName: senderName,
        channelId: messageData.channelId || "",
        projectId: messageData.projectId || "",
        mentionType: "ROLE",
        mentionId: roleId,
        roleName: roleName,
      },
    };

    await admin.messaging().sendToDevice(fcmToken, notificationPayload);
    console.log(`Sent role mention notification to user ${userId} for role ${roleId}`);
  } catch (error) {
    console.error(`Error sending role notification to user ${userId}:`, error);
  }
}

function truncateMessage(content: string, maxLength: number): string {
  if (!content) return "";

  if (content.length <= maxLength) {
    return content;
  }

  return content.substring(0, maxLength - 3) + "...";
}
