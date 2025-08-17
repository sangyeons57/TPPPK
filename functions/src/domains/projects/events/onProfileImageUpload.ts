import { onObjectFinalized } from "firebase-functions/v2/storage";
import * as admin from "firebase-admin";
import { 
  createLogger, 
  withTracing,
  STORAGE_BUCKETS,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY,
  COLLECTIONS,
  STORAGE_ROOT
} from "../../../shared";

export const onProjectProfileImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.MEDIUM as any,
    timeoutSeconds: 120,
    bucket: STORAGE_BUCKETS,
  },
  async (event) => {
    const logger = createLogger({
      domain: "projects",
      operation: "processProfileImageUpload",
      startTime: Date.now(),
    });

    try {
      await withTracing(
        {
          requestId: `storage-${Date.now()}`,
          domain: "projects",
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

          // Only process files from project_profile_images directory
          const pathParts = name.split("/");
          if (pathParts.length < 2 || pathParts[0] !== STORAGE_ROOT.PROJECT_PROFILE_ORIGIN) {
            logger.debug("Skipping file from other directory", { name, pathParts });
            return;
          }

          const projectId = pathParts[1];
          if (!projectId) {
            logger.error("Could not extract projectId from file path", { name });
            return;
          }

          logger.info("Processing project profile image upload", { projectId, name, contentType });

          const storage = admin.storage();
          const originalFile = storage.bucket(bucket).file(name);
          
          // Create fixed file path in project_profiles directory
          const processedFilePath = `${STORAGE_ROOT.PROJECT_PROFILE_PROCESSED}/${projectId}/profile.webp`;
          const processedFile = storage.bucket(bucket).file(processedFilePath);

          // Delete existing profile image if it exists
          try {
            const [exists] = await processedFile.exists();
            if (exists) {
              await processedFile.delete();
              logger.info("Deleted existing project profile image", { processedFilePath });
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

          // Update project document to notify clients that processing is complete
          const firestore = admin.firestore();
          await firestore
            .collection(COLLECTIONS.PROJECTS)
            .doc(projectId)
            .update({
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });

          logger.info("Updated project document with new timestamp", { projectId });

          // Clean up the original file
          try {
            await originalFile.delete();
            logger.info("Cleaned up original file", { name });
          } catch (cleanupError) {
            logger.warn("Failed to cleanup original file", { error: cleanupError, name });
          }

          logger.info("Project profile image processing completed successfully", { 
            projectId, 
            processedFilePath 
          });
        }
      );
    } catch (error) {
      logger.error("Failed to process project profile image upload", error);
      // Don't throw error to prevent retry loops for invalid files
    }
  }
);