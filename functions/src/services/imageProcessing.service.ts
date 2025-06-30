/**
 * 이미지 처리를 담당하는 서비스
 * Sharp 라이브러리를 사용하여 이미지 리사이징, 최적화, 형식 변환을 수행합니다.
 */

import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import * as path from "path";
import * as os from "os";
import * as fs from "fs";
import sharp from "sharp";
import {STORAGE_ROOT, STORAGE_METADATA} from "../constants";

export class ImageProcessingService {
  private storage = admin.storage();

  /**
   * 사용자 프로필 이미지를 처리하고 저장합니다.
   *
   * @param {string} userId 사용자 ID
   * @param {string} sourceFilePath 원본 파일 경로 (Storage 내)
   * @return {Promise<string>} 처리된 이미지의 Storage 경로
   */
  async processAndSaveProfileImage(userId: string, sourceFilePath: string): Promise<string> {
    const requestId = `img-${Date.now()}`;

    logger.info("Starting profile image processing", {
      requestId,
      userId,
      sourceFilePath,
    });

    try {
      // 1. 임시 파일 경로 설정
      const bucket = this.storage.bucket();
      const fileName = path.basename(sourceFilePath);
      const tempFilePath = path.join(os.tmpdir(), fileName);
      const processedFileName = "profile.webp";
      const processedTempPath = path.join(os.tmpdir(), processedFileName);

      // 2. 원본 파일 다운로드
      await bucket.file(sourceFilePath).download({destination: tempFilePath});
      logger.info("Downloaded source file", {requestId, tempFilePath});

      // 3. Sharp로 이미지 처리
      await sharp(tempFilePath)
        .resize(400, 400, {
          fit: "cover",
          position: "center",
        })
        .webp({
          quality: 85,
          effort: 4,
        })
        .toFile(processedTempPath);

      logger.info("Image processing completed", {
        requestId,
        outputPath: processedTempPath,
      });

      // 4. 처리된 이미지를 Storage에 업로드
      const processedStoragePath = `${STORAGE_ROOT.USER_PROFILE_PROCESSED}/${userId}/${processedFileName}`;
      const metadata = {
        contentType: STORAGE_METADATA.PROFILE_IMAGE.CONTENT_TYPE,
        cacheControl: STORAGE_METADATA.PROFILE_IMAGE.CACHE_CONTROL,
        metadata: {
          processedAt: new Date().toISOString(),
          processedBy: "ImageProcessingService",
          userId: userId,
        },
      };

      await bucket.upload(processedTempPath, {
        destination: processedStoragePath,
        metadata,
      });

      logger.info("Processed image uploaded", {
        requestId,
        processedStoragePath,
      });

      // 5. 임시 파일 정리
      this.cleanupTempFiles([tempFilePath, processedTempPath]);

      return processedStoragePath;
    } catch (error) {
      logger.error("Image processing failed", {
        requestId,
        userId,
        sourceFilePath,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });
      throw error;
    }
  }

  /**
   * 이미지의 다운로드 URL을 생성합니다.
   *
   * @param {string} storagePath Storage 내 파일 경로
   * @return {Promise<string>} 공개 접근 가능한 다운로드 URL
   */
  async generateDownloadUrl(storagePath: string): Promise<string> {
    try {
      const bucket = this.storage.bucket();
      const file = bucket.file(storagePath);

      // 파일을 공개로 설정
      await file.makePublic();

      // 공개 URL 생성
      const publicUrl = `https://storage.googleapis.com/${bucket.name}/${storagePath}`;

      logger.info("Generated download URL", {
        storagePath,
        publicUrl,
      });

      return publicUrl;
    } catch (error) {
      logger.error("Failed to generate download URL", {
        storagePath,
        error: error instanceof Error ? error.message : String(error),
      });
      throw error;
    }
  }

  /**
   * 프로젝트 프로필 이미지를 처리하고 저장합니다.
   *
   * @param {string} projectId 프로젝트 ID
   * @param {string} sourceFilePath 원본 파일 경로 (Storage 내)
   * @return {Promise<string>} 처리된 이미지의 Storage 경로
   */
  async processAndSaveProjectProfileImage(projectId: string, sourceFilePath: string): Promise<string> {
    const requestId = `proj-img-${Date.now()}`;

    logger.info("Starting project profile image processing", {
      requestId,
      projectId,
      sourceFilePath,
    });

    try {
      // 1. 임시 파일 경로 설정
      const bucket = this.storage.bucket();
      const fileName = path.basename(sourceFilePath);
      const tempFilePath = path.join(os.tmpdir(), fileName);
      const processedFileName = "profile.webp";
      const processedTempPath = path.join(os.tmpdir(), processedFileName);

      // 2. 원본 파일 다운로드
      await bucket.file(sourceFilePath).download({destination: tempFilePath});
      logger.info("Downloaded source file", {requestId, tempFilePath});

      // 3. Sharp로 이미지 처리 (프로젝트 이미지는 500x500으로 더 크게)
      await sharp(tempFilePath)
        .resize(500, 500, {
          fit: "cover",
          position: "center",
        })
        .webp({
          quality: 85,
          effort: 4,
        })
        .toFile(processedTempPath);

      logger.info("Project image processing completed", {
        requestId,
        outputPath: processedTempPath,
      });

      // 4. 처리된 이미지를 Storage에 업로드
      const processedStoragePath = `${STORAGE_ROOT.PROJECT_PROFILE_PROCESSED}/${projectId}/${processedFileName}`;
      const metadata = {
        contentType: STORAGE_METADATA.PROFILE_IMAGE.CONTENT_TYPE,
        cacheControl: STORAGE_METADATA.PROFILE_IMAGE.CACHE_CONTROL,
        metadata: {
          processedAt: new Date().toISOString(),
          processedBy: "ImageProcessingService",
          projectId: projectId,
        },
      };

      await bucket.upload(processedTempPath, {
        destination: processedStoragePath,
        metadata,
      });

      logger.info("Processed project image uploaded", {
        requestId,
        processedStoragePath,
      });

      // 5. 임시 파일 정리
      this.cleanupTempFiles([tempFilePath, processedTempPath]);

      return processedStoragePath;
    } catch (error) {
      logger.error("Project image processing failed", {
        requestId,
        projectId,
        sourceFilePath,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });
      throw error;
    }
  }

  /**
   * 임시 파일들을 정리합니다.
   *
   * @param {string[]} filePaths 삭제할 파일 경로 배열
   */
  private cleanupTempFiles(filePaths: string[]): void {
    filePaths.forEach((filePath) => {
      try {
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath);
          logger.info("Cleaned up temp file", {filePath});
        }
      } catch (error) {
        logger.warn("Failed to cleanup temp file", {
          filePath,
          error: error instanceof Error ? error.message : String(error),
        });
      }
    });
  }
}

