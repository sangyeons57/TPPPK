import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing,
  FRIEND_SUBCOLLECTION_STATUS
} from "../../../shared";

interface GetFriendsResponse {
  friends: Array<{
    id: string;
    name: string;
    profileImageUrl?: string;
    isOnline?: boolean;
  }>;
}

export const getFriends = onCall(
  {
    region: "asia-northeast3",
    memory: "256MiB",
    timeoutSeconds: 30,
  },
  async (request): Promise<GetFriendsResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "friends",
      operation: "getFriends",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "friends",
          operation: "getFriends",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);

          const firestore = admin.firestore();

          // Subcollection에서 직접 친구 목록 조회
          const friendsQuery = await firestore
            .collection(`users/${userId}/friends`)
            .where("status", "==", FRIEND_SUBCOLLECTION_STATUS.ACCEPTED)
            .get();

          if (friendsQuery.empty) {
            logger.info("No friends found", { userId });
            return { friends: [] };
          }

          // 친구 문서에서 직접 정보 추출 (사용자 정보 재조회 불필요)
          const friendsData = friendsQuery.docs.map(doc => {
            try {
              const friendData = doc.data();
              return {
                id: doc.id, // 친구의 userId
                name: friendData.name || "Unknown User",
                profileImageUrl: friendData.profileImageUrl || undefined,
                isOnline: false, // 온라인 상태는 별도 조회 필요 시 추가
              };
            } catch (error) {
              logger.warn("Failed to process friend data", { friendId: doc.id, error });
              return null;
            }
          });

          // null 값 필터링 및 타입 보장
          const validFriends = friendsData.filter((friend): friend is NonNullable<typeof friend> => friend !== null);

          logger.info("Friends retrieved successfully", { 
            userId, 
            friendCount: validFriends.length 
          });

          return {
            friends: validFriends,
          };
        }
      );
    } catch (error) {
      logger.error("Failed to get friends", error);
      throw handleError(error, "getFriends");
    }
  }
);