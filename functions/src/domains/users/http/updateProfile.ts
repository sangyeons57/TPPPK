import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  validateRequired, 
  validateUsername,
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing 
} from "../../../shared";

interface UpdateUserProfileRequest {
  name?: string;
  memo?: string;
}

interface UpdateUserProfileResponse {
  userProfile: {
    id: string;
    name: string;
    memo?: string;
    updatedAt: string;
  };
}

export const updateUserProfile = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 60,
  },
  async (request): Promise<UpdateUserProfileResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "users",
      operation: "updateProfile",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "users", 
          operation: "updateProfile",
          startTime: Date.now(),
        },
        async () => {
          // 인증 검증
          const userId = validateAuth(request.auth);
          logger.info("User authenticated", { userId });

          const { name, memo } = request.data as UpdateUserProfileRequest;

          // 입력 검증
          if (name !== undefined) {
            validateRequired(name, "name");
            validateUsername(name);
          }

          const firestore = admin.firestore();
          const userRef = firestore.collection("users").doc(userId);

          // 현재 사용자 조회
          const userDoc = await userRef.get();
          if (!userDoc.exists) {
            throw new Error("User not found");
          }

          // 사용자명 중복 검사 (변경하는 경우에만)
          if (name && name !== userDoc.data()?.name) {
            const existingUserQuery = await firestore
              .collection("users")
              .where("name", "==", name)
              .limit(1)
              .get();

            if (!existingUserQuery.empty) {
              throw new Error("Username already exists");
            }
          }

          // 업데이트할 데이터 준비
          const updates: any = {
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          };

          if (name !== undefined) {
            updates.name = name;
          }
          if (memo !== undefined) {
            updates.memo = memo;
          }

          // 데이터 업데이트
          await userRef.update(updates);

          // 업데이트된 데이터 조회
          const updatedDoc = await userRef.get();
          const updatedData = updatedDoc.data()!;

          logger.info("User profile updated successfully", { userId, updates });

          return {
            userProfile: {
              id: userId,
              name: updatedData.name,
              memo: updatedData.memo,
              updatedAt: updatedData.updatedAt?.toDate()?.toISOString() || new Date().toISOString(),
            },
          };
        }
      );
    } catch (error) {
      logger.error("Failed to update user profile", error);
      throw handleError(error, "updateUserProfile");
    }
  }
);