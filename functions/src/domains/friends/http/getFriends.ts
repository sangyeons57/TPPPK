import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { 
  validateAuth, 
  handleError, 
  createLogger, 
  getOrCreateRequestId,
  withTracing
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

          // 사용자의 친구 목록 조회
          const friendshipsQuery = await firestore
            .collection("friends")
            .where("userId", "==", userId)
            .where("status", "==", "accepted")
            .get();

          if (friendshipsQuery.empty) {
            logger.info("No friends found", { userId });
            return { friends: [] };
          }

          // 친구들의 ID 추출
          const friendIds = friendshipsQuery.docs.map(doc => doc.data().friendId);

          // 친구들의 사용자 정보 조회
          const friendsData = await Promise.all(
            friendIds.map(async (friendId) => {
              try {
                const userDoc = await firestore.collection("users").doc(friendId).get();
                if (userDoc.exists) {
                  const userData = userDoc.data()!;
                  return {
                    id: friendId,
                    name: userData.name || "Unknown User",
                    profileImageUrl: userData.profileImageUrl,
                    isOnline: userData.isOnline || false,
                  };
                }
                return null;
              } catch (error) {
                logger.warn("Failed to fetch friend data", { friendId, error });
                return null;
              }
            })
          );

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