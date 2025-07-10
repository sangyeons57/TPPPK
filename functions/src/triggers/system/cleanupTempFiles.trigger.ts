import {onSchedule} from "firebase-functions/v2/scheduler";
import {logger} from "firebase-functions";
import {RUNTIME_CONFIG} from "../../core/constants";
import {STORAGE_BUCKETS} from "../../core/constants";
import * as admin from "firebase-admin";

/**
 * Scheduled function to clean up old temporary image files
 * Runs daily to clean up files older than 24 hours in:
 * - user_profile_images/ (user temporary uploads)
 * - project_profile_images/ (project temporary uploads)
 */
export const cleanupTempFiles = onSchedule(
  {
    schedule: "0 2 * * *", // 매일 새벽 2시에 실행
    timeZone: "Asia/Seoul",
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (event) => {
    try {
      logger.info("🧹 Starting cleanup of temporary image files...");

      const storage = admin.storage();
      const bucket = storage.bucket(STORAGE_BUCKETS.USER_PROFILES);
      
      // 24시간 전 시간 계산
      const twentyFourHoursAgo = new Date();
      twentyFourHoursAgo.setHours(twentyFourHoursAgo.getHours() - 24);
      
      let totalDeletedFiles = 0;

      // 1. 사용자 프로필 임시 이미지 정리 (user_profile_images/)
      logger.info("🔍 Checking user_profile_images/ directory...");
      
      try {
        const [userFiles] = await bucket.getFiles({
          prefix: "user_profile_images/",
        });

        let deletedUserFiles = 0;
        for (const file of userFiles) {
          try {
            const [metadata] = await file.getMetadata();
            const timeCreated = metadata.timeCreated;
            if (!timeCreated) continue;
            
            const createdTime = new Date(timeCreated);
            
            // 24시간 이전에 생성된 파일만 삭제
            if (createdTime < twentyFourHoursAgo) {
              await file.delete();
              deletedUserFiles++;
              logger.info(`🗑️ Deleted old user temp file: ${file.name}`);
            }
          } catch (fileError) {
            logger.warn(`⚠️ Failed to process user temp file ${file.name}:`, fileError);
          }
        }
        
        logger.info(`✅ Cleaned up ${deletedUserFiles} old user temporary files`);
        totalDeletedFiles += deletedUserFiles;
        
      } catch (userCleanupError) {
        logger.error("❌ Error cleaning up user_profile_images:", userCleanupError);
      }

      // 2. 프로젝트 프로필 임시 이미지 정리 (project_profile_images/)
      logger.info("🔍 Checking project_profile_images/ directory...");
      
      try {
        const [projectFiles] = await bucket.getFiles({
          prefix: "project_profile_images/",
        });

        let deletedProjectFiles = 0;
        for (const file of projectFiles) {
          try {
            const [metadata] = await file.getMetadata();
            const timeCreated = metadata.timeCreated;
            if (!timeCreated) continue;
            
            const createdTime = new Date(timeCreated);
            
            // 24시간 이전에 생성된 파일만 삭제
            if (createdTime < twentyFourHoursAgo) {
              await file.delete();
              deletedProjectFiles++;
              logger.info(`🗑️ Deleted old project temp file: ${file.name}`);
            }
          } catch (fileError) {
            logger.warn(`⚠️ Failed to process project temp file ${file.name}:`, fileError);
          }
        }
        
        logger.info(`✅ Cleaned up ${deletedProjectFiles} old project temporary files`);
        totalDeletedFiles += deletedProjectFiles;
        
      } catch (projectCleanupError) {
        logger.error("❌ Error cleaning up project_profile_images:", projectCleanupError);
      }

      logger.info(`🎉 Cleanup completed. Total files deleted: ${totalDeletedFiles}`);
      
    } catch (error) {
      logger.error("❌ Error during temp files cleanup:", error);
    }
  }
);

/**
 * Manual cleanup function that can be called on-demand
 * More aggressive cleanup - removes files older than 1 hour
 */
export const cleanupTempFilesNow = onSchedule(
  {
    schedule: "every 1 hours", // 매시간 실행
    timeZone: "Asia/Seoul", 
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (event) => {
    try {
      logger.info("🧹 Starting aggressive cleanup of temporary image files...");

      const storage = admin.storage();
      const bucket = storage.bucket(STORAGE_BUCKETS.USER_PROFILES);
      
      // 1시간 전 시간 계산 (더 빠른 정리)
      const oneHourAgo = new Date();
      oneHourAgo.setHours(oneHourAgo.getHours() - 1);
      
      let totalDeletedFiles = 0;

              // 사용자 프로필 임시 이미지 정리
        try {
          const [userFiles] = await bucket.getFiles({
            prefix: "user_profile_images/",
          });

          let deletedUserFiles = 0;
          for (const file of userFiles) {
            try {
              const [metadata] = await file.getMetadata();
              const timeCreated = metadata.timeCreated;
              if (!timeCreated) continue;
              
              const createdTime = new Date(timeCreated);
              
              if (createdTime < oneHourAgo) {
              await file.delete();
              deletedUserFiles++;
              logger.info(`🗑️ Deleted old user temp file: ${file.name}`);
            }
          } catch (fileError) {
            logger.warn(`⚠️ Failed to process user temp file ${file.name}:`, fileError);
          }
        }
        
        totalDeletedFiles += deletedUserFiles;
        
      } catch (userCleanupError) {
        logger.error("❌ Error cleaning up user_profile_images:", userCleanupError);
      }

              // 프로젝트 프로필 임시 이미지 정리  
        try {
          const [projectFiles] = await bucket.getFiles({
            prefix: "project_profile_images/",
          });

          let deletedProjectFiles = 0;
          for (const file of projectFiles) {
            try {
              const [metadata] = await file.getMetadata();
              const timeCreated = metadata.timeCreated;
              if (!timeCreated) continue;
              
              const createdTime = new Date(timeCreated);
              
              if (createdTime < oneHourAgo) {
              await file.delete();
              deletedProjectFiles++;
              logger.info(`🗑️ Deleted old project temp file: ${file.name}`);
            }
          } catch (fileError) {
            logger.warn(`⚠️ Failed to process project temp file ${file.name}:`, fileError);
          }
        }
        
        totalDeletedFiles += deletedProjectFiles;
        
      } catch (projectCleanupError) {
        logger.error("❌ Error cleaning up project_profile_images:", projectCleanupError);
      }

      if (totalDeletedFiles > 0) {
        logger.info(`🎉 Aggressive cleanup completed. Total files deleted: ${totalDeletedFiles}`);
      }
      
    } catch (error) {
      logger.error("❌ Error during aggressive temp files cleanup:", error);
    }
  }
); 