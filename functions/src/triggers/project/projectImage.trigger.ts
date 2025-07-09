import {onObjectFinalized} from "firebase-functions/v2/storage";
import {logger} from "firebase-functions";
import {RUNTIME_CONFIG} from "../../core/constants";
import {STORAGE_BUCKETS} from "../../core/constants";
import * as admin from "firebase-admin";

/**
 * Storage trigger for project profile images
 * Processes uploaded images and stores them at fixed paths (project_profiles/{projectId}/profile.webp)
 * No Firestore updates needed - client loads images directly via fixed paths
 */
export const onProjectProfileImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
    bucket: STORAGE_BUCKETS.USER_PROFILES, // Same bucket as user profiles
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

      // Only process files from project_profile_images directory (ignore processed files)
      const pathParts = name.split("/");
      if (pathParts.length < 2 || pathParts[0] !== "project_profile_images") {
        // Silently ignore files from other directories (like project_profiles)
        return;
      }

      const projectId = pathParts[1];
      if (!projectId) {
        logger.error("Could not extract projectId from file path");
        return;
      }

      logger.info(`🚀 Processing project profile image upload for project: ${projectId}, file: ${name}`);

      // Get Firebase Storage instance
      const storage = admin.storage();

      try {
        // Get the original file
        const originalFile = storage.bucket(bucket).file(name);

        // Create fixed file path in project_profiles directory (always use profile.webp)
        const processedFilePath = `project_profiles/${projectId}/profile.webp`;
        const processedFile = storage.bucket(bucket).file(processedFilePath);

        // Check if the processed file already exists and delete it first
        try {
          const [exists] = await processedFile.exists();
          if (exists) {
            await processedFile.delete();
            logger.info(`🗑️ Deleted existing project profile image: ${processedFilePath}`);
          }
        } catch (deleteError) {
          logger.warn(`No existing file to delete or delete failed: ${(deleteError as Error).message}`);
        }

        // Copy the original file to the processed location (with fixed filename)
        await originalFile.copy(processedFile);
        logger.info(`📁 Copied ${name} to ${processedFilePath}`);

        // No Firestore update needed; client loads image directly via Storage path.
        // The fixed path system eliminates the need for URL storage in Firestore.

        logger.info(`✅ Processed project profile image stored at ${processedFilePath}`);

        // Clean up the original file in project_profile_images after successful processing
        try {
          await originalFile.delete();
          logger.info("🗑️ Cleaned up original file:", name);
        } catch (cleanupError) {
          logger.warn("⚠️ Failed to cleanup original file:", (cleanupError as Error).message);
        }
      } catch (copyError) {
        logger.error(`Error processing file from ${name} to project_profiles:`, copyError);
      }
    } catch (error) {
      logger.error("Error processing project profile image upload:", error);
    }
  }
);