import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

/**
 * Firebase Function trigger for handling mention notifications
 * Triggers when a new message is created and processes any mentions to send notifications
 */
export const onMessageMentionNotification = functions.firestore
  .document("messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const messageId = context.params.messageId;
    const messageData = snapshot.data();
    
    if (!messageData) {
      console.log(`No data found for message ${messageId}`);
      return null;
    }

    // Extract mentions from the message
    const mentions = messageData.mentions || [];
    if (mentions.length === 0) {
      console.log(`No mentions found in message ${messageId}`);
      return null;
    }

    console.log(`Processing ${mentions.length} mentions for message ${messageId}`);

    try {
      // Process each mention
      const notificationPromises = mentions.map(async (mention: any) => {
        return processMention(mention, messageData, messageId);
      });

      // Wait for all notification processing to complete
      await Promise.all(notificationPromises);
      
      console.log(`Successfully processed all mentions for message ${messageId}`);
      return null;
    } catch (error) {
      console.error(`Error processing mentions for message ${messageId}:`, error);
      throw error;
    }
  });

/**
 * Process a single mention and send appropriate notifications
 */
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

/**
 * Process a user mention - send notification to the mentioned user
 */
async function procesUserMention(
  userId: string,
  messageData: any,
  messageId: string,
  displayName: string
): Promise<void> {
  console.log(`Processing user mention for userId: ${userId}`);

  // Don't send notification if user mentions themselves
  if (userId === messageData.senderId) {
    console.log(`User ${userId} mentioned themselves, skipping notification`);
    return;
  }

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

    // Create notification payload
    const notificationPayload = {
      notification: {
        title: `${senderName}님이 회원님을 멘션했습니다`,
        body: truncateMessage(messageData.content, 100),
        icon: "ic_stat_ic_notification",
        sound: "default"
      },
      data: {
        type: "mention",
        messageId: messageId,
        senderId: messageData.senderId,
        senderName: senderName,
        channelId: messageData.channelId || "",
        projectId: messageData.projectId || "",
        mentionType: "USER",
        mentionId: userId
      }
    };

    // Send notification
    await admin.messaging().sendToDevice(fcmToken, notificationPayload);
    console.log(`Sent mention notification to user ${userId}`);

  } catch (error) {
    console.error(`Error sending notification to user ${userId}:`, error);
    throw error;
  }
}

/**
 * Process a role mention - send notifications to all users with the mentioned role
 */
async function processRoleMention(
  roleId: string,
  messageData: any,
  messageId: string,
  displayName: string
): Promise<void> {
  console.log(`Processing role mention for roleId: ${roleId}`);

  try {
    // Handle special @everyone role
    if (roleId === "everyone") {
      await processEveryoneMention(messageData, messageId, displayName);
      return;
    }

    // Get project members with the specific role
    const projectId = messageData.projectId;
    if (!projectId) {
      console.warn(`No projectId found for role mention ${roleId}`);
      return;
    }

    // Query members with this role using array-contains since roleIds is an array
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

    // Get FCM tokens for all members with this role
    const memberNotificationPromises = membersQuery.docs.map(async (memberDoc) => {
      const memberData = memberDoc.data();
      const userId = memberData.userId;

      // Don't notify the sender
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

/**
 * Process @everyone mention - send notifications to all project members
 */
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
    // Get all project members
    const membersQuery = await admin.firestore()
      .collection("projects")
      .doc(projectId)
      .collection("members")
      .get();

    if (membersQuery.empty) {
      console.log(`No members found in project ${projectId}`);
      return;
    }

    // Send notifications to all members (except sender)
    const memberNotificationPromises = membersQuery.docs.map(async (memberDoc) => {
      const memberData = memberDoc.data();
      const userId = memberData.userId;

      // Don't notify the sender
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

/**
 * Send role mention notification to a specific user
 */
async function sendRoleNotificationToUser(
  userId: string,
  roleId: string,
  roleName: string,
  messageData: any,
  messageId: string
): Promise<void> {
  try {
    // Get user's FCM token
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

    // Get sender information
    const senderDoc = await admin.firestore()
      .collection("users")
      .doc(messageData.senderId)
      .get();
    
    const senderData = senderDoc.exists ? senderDoc.data() : null;
    const senderName = senderData?.name || "Someone";

    // Create notification payload
    const notificationPayload = {
      notification: {
        title: `${senderName}님이 @${roleName}을 멘션했습니다`,
        body: truncateMessage(messageData.content, 100),
        icon: "ic_stat_ic_notification",
        sound: "default"
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
        roleName: roleName
      }
    };

    // Send notification
    await admin.messaging().sendToDevice(fcmToken, notificationPayload);
    console.log(`Sent role mention notification to user ${userId} for role ${roleId}`);

  } catch (error) {
    console.error(`Error sending role notification to user ${userId}:`, error);
    // Don't throw here to prevent other notifications from failing
  }
}

/**
 * Truncate message content for notification body
 */
function truncateMessage(content: string, maxLength: number): string {
  if (!content) return "";
  
  if (content.length <= maxLength) {
    return content;
  }
  
  return content.substring(0, maxLength - 3) + "...";
}