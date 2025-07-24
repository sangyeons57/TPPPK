import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {RUNTIME_CONFIG} from "../../core/constants";

/**
 * Legacy Gen1 FCM notification function for compatibility
 * This ensures the Android app's FCM test functionality works
 */
export const sendCustomNotificationLegacy = functions
  .region(RUNTIME_CONFIG.REGION)
  .https.onCall(async (data, context) => {
    // Verify authentication
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'The function must be called while authenticated.'
      );
    }

    const { userId, title, body, data: customData } = data;

    if (!userId || !title || !body) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Missing required parameters: userId, title, body'
      );
    }

    try {
      // Get user's FCM token
      const userDoc = await admin.firestore()
        .collection("users")
        .doc(userId)
        .get();

      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          'not-found',
          `User ${userId} not found`
        );
      }

      const userData = userDoc.data();
      const fcmToken = userData?.fcmToken;

      if (!fcmToken) {
        throw new functions.https.HttpsError(
          'failed-precondition',
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
      throw new functions.https.HttpsError(
        'internal',
        'Failed to send notification'
      );
    }
  });