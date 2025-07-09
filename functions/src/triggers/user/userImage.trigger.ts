import {onObjectFinalized} from "firebase-functions/v2/storage";
import {logger} from "firebase-functions";
import {UpdateUserImageUseCase} from "../../business/user/usecases/updateUserImage.usecase";
import {RUNTIME_CONFIG} from "../../core/constants";
import {STORAGE_BUCKETS} from "../../core/constants";
import {Providers} from "../../config/dependencies";
import * as admin from "firebase-admin";

/**
 * Simplified Storage trigger for user profile images
 * Updates User entity's profileImageUrl when image is uploaded
 */
export const onUserProfileImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
    bucket: STORAGE_BUCKETS.USER_PROFILES,
  },
  async (event) => {
    try {
      const {bucket, name, contentType} = event.data;

      if (!name || !contentType) {
        logger.info("Missing file name or content type");
        return;
      }

      // Only process image files
      if (!contentType.startsWith("image/")) {
        logger.info(`Skipping non-image file: ${contentType}`);
        return;
      }

      // Only process files from user_profile_images directory (ignore processed files)
      const pathParts = name.split("/");
      if (pathParts.length < 2 || pathParts[0] !== "user_profile_images") {
        // Silently ignore files from other directories (like user_profiles)
        return;
      }

      const userId = pathParts[1];
      if (!userId) {
        logger.error("Could not extract userId from file path");
        return;
      }

      logger.info(`🚀 Processing profile image upload for user: ${userId}, file: ${name}`);

      // Get Firebase Storage instance
      const storage = admin.storage();

      try {
        // Get the original file
        const originalFile = storage.bucket(bucket).file(name);

        // Create fixed file path in user_profiles directory (always use profile.webp)
        const processedFilePath = `user_profiles/${userId}/profile.webp`;
        const processedFile = storage.bucket(bucket).file(processedFilePath);

        // Check if the processed file already exists and delete it first
        try {
          const [exists] = await processedFile.exists();
          if (exists) {
            await processedFile.delete();
            logger.info(`🗑️ Deleted existing profile image: ${processedFilePath}`);
          }
        } catch (deleteError) {
          logger.warn(`No existing file to delete or delete failed: ${(deleteError as Error).message}`);
        }

        // Copy the original file to the processed location (with fixed filename)
        await originalFile.copy(processedFile);
        logger.info(`📁 Copied ${name} to ${processedFilePath}`);

        // Generate signed URL for secure access (expires in 10 years)
        const [signedUrl] = await processedFile.getSignedUrl({
          action: "read",
          expires: new Date(Date.now() + 10 * 365 * 24 * 60 * 60 * 1000), // 10년 후 만료
        });

        // Add cache buster to signed URL
        const timestamp = Date.now();
        const processedPublicUrl = `${signedUrl}&v=${timestamp}`;

        // No Firestore update needed; client loads image directly via Storage path.

        logger.info(`✅ Processed profile image stored at ${processedFilePath}`);

        // Clean up the original file in user_profile_images after successful processing
        try {
          await originalFile.delete();
          logger.info("🗑️ Cleaned up original file:", name);
        } catch (cleanupError) {
          logger.warn("⚠️ Failed to cleanup original file:", (cleanupError as Error).message);
        }
      } catch (copyError) {
        logger.error(`Error processing file from ${name} to user_profiles:`, copyError);
      }
    } catch (error) {
      logger.error("Error processing user profile image upload:", error);
    }
  }
);
