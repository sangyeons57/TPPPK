/**
 * 사용자 프로필 업데이트 Function
 * 사용자의 이름, 메모 등의 프로필 정보를 업데이트합니다.
 */

import {onCall, HttpsError} from "firebase-functions/v2/https";
import {FUNCTION_REGION, FUNCTION_MEMORY, FUNCTION_TIMEOUT} from "../../constants";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

// Firebase Admin 초기화 (이미 다른 곳에서 초기화되었다면 생략)
if (!admin.apps.length) {
  admin.initializeApp();
}

interface UpdateUserProfileRequest {
  name?: string;
  memo?: string;
}

interface UpdateUserProfileResponse {
  success: boolean;
  message: string;
  updatedFields?: string[];
}

export const updateUserProfile = onCall({
  region: FUNCTION_REGION,
  memory: FUNCTION_MEMORY.SMALL,
  timeoutSeconds: FUNCTION_TIMEOUT.LONG,
}, async (request) => {
  const requestId = `update-profile-${Date.now()}`;
  
  logger.info("User profile update requested", {
    requestId,
    auth: {
      uid: request.auth?.uid,
      email: request.auth?.token?.email
    },
    data: request.data
  });

  try {
    // 1. 인증 확인
    if (!request.auth) {
      logger.warn("Unauthenticated update profile request", {requestId});
      throw new HttpsError("unauthenticated", "사용자 인증이 필요합니다.");
    }

    const userId = request.auth.uid;
    const requestData = request.data as UpdateUserProfileRequest;

    // 2. 요청 데이터 유효성 검사
    if (!requestData || (requestData.name === undefined && requestData.memo === undefined)) {
      logger.warn("Invalid update profile request data", {requestId, data: requestData});
      throw new HttpsError("invalid-argument", "업데이트할 프로필 정보가 필요합니다.");
    }

    // 3. 이름 유효성 검사
    if (requestData.name !== undefined) {
      if (typeof requestData.name !== "string") {
        throw new HttpsError("invalid-argument", "이름은 문자열이어야 합니다.");
      }
      if (requestData.name.trim().length === 0) {
        throw new HttpsError("invalid-argument", "이름은 비어있을 수 없습니다.");
      }
      if (requestData.name.length > 50) {
        throw new HttpsError("invalid-argument", "이름은 50자를 초과할 수 없습니다.");
      }
    }

    // 4. 메모 유효성 검사
    if (requestData.memo !== undefined) {
      if (typeof requestData.memo !== "string") {
        throw new HttpsError("invalid-argument", "메모는 문자열이어야 합니다.");
      }
      if (requestData.memo.length > 200) {
        throw new HttpsError("invalid-argument", "메모는 200자를 초과할 수 없습니다.");
      }
    }

    // 5. Firestore 업데이트 데이터 준비
    const updateData: any = {
      updatedAt: admin.firestore.Timestamp.now()
    };
    const updatedFields: string[] = [];

    if (requestData.name !== undefined) {
      updateData.name = requestData.name.trim();
      updatedFields.push("name");
    }

    if (requestData.memo !== undefined) {
      updateData.memo = requestData.memo.trim();
      updatedFields.push("memo");
    }

    logger.info("Updating user profile", {
      requestId,
      userId,
      updatedFields,
      updateData: {...updateData, updatedAt: "[SERVER_TIMESTAMP]"}
    });

    // 6. Firestore 업데이트
    const firestore = admin.firestore();
    const userDocRef = firestore.collection("users").doc(userId);

    // 사용자 문서 존재 확인
    const userDoc = await userDocRef.get();
    if (!userDoc.exists) {
      logger.warn("User document not found", {requestId, userId});
      throw new HttpsError("not-found", "사용자 정보를 찾을 수 없습니다.");
    }

    // 7. 배치 업데이트 실행
    const batch = firestore.batch();
    
    // 사용자 문서 업데이트
    batch.update(userDocRef, updateData);
    
    // TODO: 향후 필요시 프로젝트 멤버, 메시지 등의 관련 문서도 업데이트
    // 현재는 이름 변경의 경우 프로필 이미지만큼 광범위한 업데이트가 필요하지 않음
    
    await batch.commit();

    logger.info("User profile update completed", {
      requestId,
      userId,
      updatedFields
    });

    const response: UpdateUserProfileResponse = {
      success: true,
      message: "프로필이 성공적으로 업데이트되었습니다.",
      updatedFields
    };

    return response;

  } catch (error) {
    logger.error("User profile update failed", {
      requestId,
      userId: request.auth?.uid,
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    });

    // HttpsError는 그대로 전파
    if (error instanceof HttpsError) {
      throw error;
    }

    // 기타 에러는 internal 에러로 변환
    throw new HttpsError("internal", "프로필 업데이트 중 오류가 발생했습니다.");
  }
});