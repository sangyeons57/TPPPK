import { onObjectFinalized } from "firebase-functions/v2/storage";
import * as admin from "firebase-admin";
import { createLogger, withTracing } from "../../../shared";

export const onUserProfileImageUpload = onObjectFinalized(
  {
    region: "asia-northeast3",
    memory: "512MiB",
    timeoutSeconds: 120,
  },
  async (event) => {
    const logger = createLogger({
      domain: "users",
      operation: "processProfileImageUpload",
      startTime: Date.now(),
    });

    try {
      await withTracing(
        {
          requestId: `storage-${Date.now()}`,
          domain: "users",
          operation: "processProfileImageUpload",
          startTime: Date.now(),
        },
        async () => {
          const { bucket, name, contentType } = event.data;

          if (!name || !contentType) {
            logger.info("Missing file name or content type", { bucket, name, contentType });
            return;
          }

          // Only process image files
          if (!contentType.startsWith("image/")) {
            logger.info("Skipping non-image file", { contentType, name });
            return;
          }

          // Only process files from user_profile_images directory
          const pathParts = name.split("/");
          if (pathParts.length < 2 || pathParts[0] !== "user_profile_images") {
            logger.debug("Skipping file from other directory", { name, pathParts });
            return;
          }

          const userId = pathParts[1];
          if (!userId) {
            logger.error("Could not extract userId from file path", { name });
            return;
          }

          logger.info("Processing profile image upload", { userId, name, contentType });

          const storage = admin.storage();
          const originalFile = storage.bucket(bucket).file(name);
          
          // Create fixed file path in user_profiles directory
          const processedFilePath = `user_profiles/${userId}/profile.webp`;
          const processedFile = storage.bucket(bucket).file(processedFilePath);

          // Delete existing profile image if it exists
          try {
            const [exists] = await processedFile.exists();
            if (exists) {
              await processedFile.delete();
              logger.info("Deleted existing profile image", { processedFilePath });
            }
          } catch (deleteError) {
            logger.warn("Failed to delete existing file", { error: deleteError, processedFilePath });
          }

          // Copy the original file to the processed location
          await originalFile.copy(processedFile);
          logger.info("Copied file to processed location", { from: name, to: processedFilePath });

          // Set cache-control headers for optimization
          await processedFile.setMetadata({
            cacheControl: "public, max-age=604800", // 1 week cache
            contentType: "image/webp",
          });

          // Update user document to notify clients that processing is complete
          const firestore = admin.firestore();
          await firestore
            .collection("users")
            .doc(userId)
            .update({
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

          logger.info("Updated user document with new timestamp", { userId });

          // Clean up the original file
          try {
            await originalFile.delete();
            logger.info("Cleaned up original file", { name });
          } catch (cleanupError) {
            logger.warn("Failed to cleanup original file", { error: cleanupError, name });
          }

          logger.info("Profile image processing completed successfully", { 
            userId, 
            processedFilePath 
          });
        }
      );
    } catch (error) {
      logger.error("Failed to process user profile image upload", error);
      // Don't throw error to prevent retry loops for invalid files
    }
  }
);