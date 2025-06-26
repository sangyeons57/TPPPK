/**
 * 사용자 프로필 이미지 업로드 처리 Function
 * Storage에 업로드된 프로필 이미지를 자동으로 처리하고 Firestore를 업데이트합니다.
 */

import {onObjectFinalized} from "firebase-functions/v2/storage";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import {ImageProcessingService} from "../../services/imageProcessing.service";
import {FirestoreUpdateService} from "../../services/firestoreUpdate.service";

// Firebase Admin 초기화 (이미 다른 곳에서 초기화되었다면 생략)
if (!admin.apps.length) {
  admin.initializeApp();
}

export const onUserProfileImageUpload = onObjectFinalized({
  region: "asia-northeast3",
  memory: "512MiB",
  timeoutSeconds: 540,
}, async (event) => {
  const requestId = `profile-upload-${Date.now()}`;
  const filePath = event.data.name;
  const contentType = event.data.contentType;

  logger.info("Profile image upload detected", {
    requestId,
    filePath,
    contentType,
    bucket: event.data.bucket,
  });

  try {
    // 1. 파일 경로 유효성 검사
    if (!filePath || !filePath.startsWith("user_profile_uploads/")) {
      logger.info("Ignoring non-profile upload", {requestId, filePath});
      return;
    }

    // 2. 이미 처리된 파일인지 확인 (무한 루프 방지)
    if (filePath.includes("_processed") || filePath.includes("user_profiles/")) {
      logger.info("Ignoring already processed file", {requestId, filePath});
      return;
    }

    // 3. 이미지 파일인지 확인
    if (!contentType || !contentType.startsWith("image/")) {
      logger.info("Ignoring non-image file", {requestId, filePath, contentType});
      return;
    }

    // 4. 사용자 ID 추출
    const pathParts = filePath.split("/");
    if (pathParts.length < 3) {
      logger.warn("Invalid file path structure", {requestId, filePath});
      return;
    }

    const userId = pathParts[1];
    if (!userId) {
      logger.warn("Could not extract user ID from path", {requestId, filePath});
      return;
    }

    logger.info("Processing profile image", {
      requestId,
      userId,
      originalPath: filePath,
    });

    // 5. 서비스 인스턴스 생성
    const imageProcessingService = new ImageProcessingService();
    const firestoreUpdateService = new FirestoreUpdateService();

    // 6. 이미지 처리
    const processedImagePath = await imageProcessingService.processAndSaveProfileImage(
      userId,
      filePath
    );

    logger.info("Image processing completed", {
      requestId,
      userId,
      processedImagePath,
    });

    // 7. 다운로드 URL 생성
    const downloadUrl = await imageProcessingService.generateDownloadUrl(processedImagePath);

    logger.info("Download URL generated", {
      requestId,
      userId,
      downloadUrl,
    });

    // 8. Firestore 업데이트
    await firestoreUpdateService.updateAllUserImageReferences(userId, downloadUrl);

    logger.info("Firestore update completed", {
      requestId,
      userId,
      downloadUrl,
    });

    // 9. 원본 파일 삭제
    try {
      const bucket = admin.storage().bucket();
      await bucket.file(filePath).delete();

      logger.info("Original file deleted", {
        requestId,
        originalPath: filePath,
      });
    } catch (deleteError) {
      // 원본 파일 삭제 실패는 치명적이지 않으므로 로그만 남김
      logger.warn("Failed to delete original file", {
        requestId,
        originalPath: filePath,
        error: deleteError instanceof Error ? deleteError.message : String(deleteError),
      });
    }

    logger.info("Profile image upload processing completed successfully", {
      requestId,
      userId,
      originalPath: filePath,
      processedPath: processedImagePath,
      finalUrl: downloadUrl,
    });
  } catch (error) {
    logger.error("Profile image upload processing failed", {
      requestId,
      filePath,
      contentType,
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
    });

    // 에러가 발생해도 Firebase Functions가 재시도하지 않도록 에러를 다시 던지지 않음
    // 대신 에러 로그만 남기고 정상 종료
  }
});

