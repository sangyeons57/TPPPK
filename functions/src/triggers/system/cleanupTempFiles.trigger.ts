import {onSchedule} from "firebase-functions/v2/scheduler";
import {logger} from "firebase-functions";
import {RUNTIME_CONFIG, STORAGE_BUCKETS} from "../../core/constants";
import * as admin from "firebase-admin";

/**
 * Scheduled function (daily) to remove temporary image files older than 24 hours.
 *
 * Directories cleaned:
 * 1. user_profile_images/{userId}/...
 * 2. project_profile_images/{projectId}/...
 */
export const cleanupTempFiles = onSchedule(
  {
    // 매일 새벽 2시 (Asia/Seoul)
    schedule: "0 2 * * *",
    timeZone: "Asia/Seoul",
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async () => {
    try {
      logger.info("🧹 Starting daily cleanup of temporary image files...");

      const storage = admin.storage();
      const bucket = storage.bucket(STORAGE_BUCKETS);

      // 24시간 전 시간 계산
      const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);

      let totalDeletedFiles = 0;

      // Cleanup helper
      const cleanupDirectory = async (prefix: string) => {
        logger.info(`🔍 Checking ${prefix} directory...`);
        try {
          const [files] = await bucket.getFiles({prefix: `${prefix}/`});
          let deletedCount = 0;

          for (const file of files) {
            try {
              const [metadata] = await file.getMetadata();
              const timeCreated = metadata.timeCreated;
              if (!timeCreated) continue;

              const createdTime = new Date(timeCreated);
              if (createdTime < twentyFourHoursAgo) {
                await file.delete();
                deletedCount++;
                logger.info(`🗑️ Deleted old temp file: ${file.name}`);
              }
            } catch (fileError) {
              logger.warn(`⚠️ Failed to process file ${file.name}:`, fileError);
            }
          }

          logger.info(`✅ Cleaned up ${deletedCount} old files in ${prefix}`);
          return deletedCount;
        } catch (cleanupError) {
          logger.error(`❌ Error cleaning up ${prefix}:`, cleanupError);
          return 0;
        }
      };

      totalDeletedFiles += await cleanupDirectory("user_profile_images");
      totalDeletedFiles += await cleanupDirectory("project_profile_images");

      logger.info(`🎉 Cleanup completed. Total files deleted: ${totalDeletedFiles}`);
    } catch (error) {
      logger.error("❌ Error during temp files cleanup:", error);
    }
  },
);

/**
 * On-demand cleanup (hourly) to remove temporary image files older than 1 hour.
 * This is more aggressive and can be triggered manually if needed.
 */
export const cleanupTempFilesNow = onSchedule(
  {
    schedule: "every 1 hours",
    timeZone: "Asia/Seoul",
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async () => {
    try {
      logger.info("🧹 Starting aggressive cleanup of temporary image files...");

      const storage = admin.storage();
      const bucket = storage.bucket(STORAGE_BUCKETS);

      // 1시간 전 시간 계산
      const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);

      let totalDeletedFiles = 0;

      // Re-use the same helper with a different threshold
      const cleanupDirectory = async (prefix: string) => {
        try {
          const [files] = await bucket.getFiles({prefix: `${prefix}/`});
          let deletedCount = 0;
          for (const file of files) {
            try {
              const [metadata] = await file.getMetadata();
              const timeCreated = metadata.timeCreated;
              if (!timeCreated) continue;

              const createdTime = new Date(timeCreated);
              if (createdTime < oneHourAgo) {
                await file.delete();
                deletedCount++;
                logger.info(`🗑️ Deleted old temp file: ${file.name}`);
              }
            } catch (fileError) {
              logger.warn(`⚠️ Failed to process file ${file.name}:`, fileError);
            }
          }
          return deletedCount;
        } catch (cleanupError) {
          logger.error(`❌ Error cleaning up ${prefix}:`, cleanupError);
          return 0;
        }
      };

      totalDeletedFiles += await cleanupDirectory("user_profile_images");
      totalDeletedFiles += await cleanupDirectory("project_profile_images");

      if (totalDeletedFiles > 0) {
        logger.info(`🎉 Aggressive cleanup completed. Total files deleted: ${totalDeletedFiles}`);
      } else {
        logger.info("🤖 No temporary files needed deletion in this run.");
      }
    } catch (error) {
      logger.error("❌ Error during aggressive temp files cleanup:", error);
    }
  },
);
