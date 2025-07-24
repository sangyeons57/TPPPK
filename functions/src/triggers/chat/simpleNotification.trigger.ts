import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

/**
 * Simple FCM notification function for testing
 */
export const sendCustomNotification = functions.onCall({
  region: "asia-northeast3"
}, async (request) => {
  // Verify authentication
  if (!request.auth) {
    throw new functions.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const { userId, title, body, data: customData } = request.data;

  if (!userId || !title || !body) {
    throw new functions.HttpsError(
      "invalid-argument",
      "Missing required parameters: userId, title, body"
    );
  }

  try {
    // Get user's FCM token
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(userId)
      .get();

    if (!userDoc.exists) {
      throw new functions.HttpsError(
        "not-found",
        `User ${userId} not found`
      );
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      throw new functions.HttpsError(
        "failed-precondition",
        `No FCM token found for user ${userId}`
      );
    }

    // Create notification payload
    const notificationPayload = {
      notification: {
        title: title,
        body: body,
        icon: "ic_stat_ic_notification",
        sound: "default"
      },
      data: customData || {}
    };

    // Send notification
    await admin.messaging().sendToDevice(fcmToken, notificationPayload);
    
    console.log(`Sent custom notification to user ${userId}`);
    return { success: true, message: "Notification sent successfully" };

  } catch (error) {
    console.error("Error sending custom notification:", error);
    throw new functions.HttpsError(
      "internal",
      "Failed to send notification"
    );
  }
});