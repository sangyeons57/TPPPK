/**
 * Firestore 업데이트를 담당하는 서비스
 * 사용자 프로필 이미지 URL 변경 시 관련된 모든 문서를 일괄 업데이트합니다.
 */

import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

export class FirestoreUpdateService {
  private firestore = admin.firestore();

  /**
   * 사용자 이미지 URL이 변경될 때 관련된 모든 Firestore 문서를 업데이트합니다.
   *
   * @param {string} userId 사용자 ID
   * @param {string} newImageUrl 새로운 이미지 URL
   */
  async updateAllUserImageReferences(userId: string, newImageUrl: string): Promise<void> {
    const requestId = `firestore-update-${Date.now()}`;

    logger.info("Starting Firestore batch update", {
      requestId,
      userId,
      newImageUrl,
    });

    try {
      const batch = this.firestore.batch();
      let updateCount = 0;

      // 1. 사용자 자신의 문서 업데이트
      const userRef = this.firestore.collection("users").doc(userId);
      batch.update(userRef, {
        profileImageUrl: newImageUrl,
        updatedAt: admin.firestore.Timestamp.now(),
      });
      updateCount++;

      logger.info("Added user document to batch", {requestId, userId});

      // 2. 사용자가 참여한 프로젝트의 멤버 정보 업데이트
      const memberQuery = await this.firestore.collectionGroup("members")
        .where("userId", "==", userId)
        .get();

      memberQuery.docs.forEach((doc) => {
        batch.update(doc.ref, {
          profileImageUrl: newImageUrl,
          updatedAt: admin.firestore.Timestamp.now(),
        });
        updateCount++;
      });

      logger.info("Added member documents to batch", {
        requestId,
        memberCount: memberQuery.docs.length,
      });

      // 3. 사용자가 보낸 메시지들의 프로필 이미지 업데이트
      const messageQuery = await this.firestore.collectionGroup("messages")
        .where("senderId", "==", userId)
        .limit(500) // 성능을 위해 제한
        .get();

      messageQuery.docs.forEach((doc) => {
        batch.update(doc.ref, {
          senderProfileImageUrl: newImageUrl,
          updatedAt: admin.firestore.Timestamp.now(),
        });
        updateCount++;
      });

      logger.info("Added message documents to batch", {
        requestId,
        messageCount: messageQuery.docs.length,
      });

      // 4. 친구 관계 문서 업데이트
      const friendQuery = await this.firestore.collectionGroup("friends")
        .where("friendId", "==", userId)
        .get();

      friendQuery.docs.forEach((doc) => {
        batch.update(doc.ref, {
          friendProfileImageUrl: newImageUrl,
          updatedAt: admin.firestore.Timestamp.now(),
        });
        updateCount++;
      });

      logger.info("Added friend documents to batch", {
        requestId,
        friendCount: friendQuery.docs.length,
      });

      // 5. DM 채널의 사용자 정보 업데이트
      const dmQuery = await this.firestore.collectionGroup("dmChannels")
        .where("participants", "array-contains", userId)
        .get();

      dmQuery.docs.forEach((doc) => {
        const data = doc.data();
        const participantInfo = data.participantInfo || {};

        // participantInfo에서 해당 사용자의 이미지 URL 업데이트
        if (participantInfo[userId]) {
          participantInfo[userId].profileImageUrl = newImageUrl;

          batch.update(doc.ref, {
            participantInfo: participantInfo,
            updatedAt: admin.firestore.Timestamp.now(),
          });
          updateCount++;
        }
      });

      logger.info("Added DM channel documents to batch", {
        requestId,
        dmCount: dmQuery.docs.length,
      });

      // 6. 배치 실행
      if (updateCount > 0) {
        await batch.commit();

        logger.info("Batch update completed successfully", {
          requestId,
          userId,
          newImageUrl,
          totalUpdates: updateCount,
        });
      } else {
        logger.info("No documents to update", {requestId, userId});
      }
    } catch (error) {
      logger.error("Firestore batch update failed", {
        requestId,
        userId,
        newImageUrl,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });
      throw error;
    }
  }

  /**
   * 사용자 문서에 새로운 프로필 이미지 URL을 설정합니다.
   *
   * @param {string} userId 사용자 ID
   * @param {string} imageUrl 이미지 URL
   */
  async updateUserProfileImage(userId: string, imageUrl: string): Promise<void> {
    try {
      const userRef = this.firestore.collection("users").doc(userId);

      await userRef.update({
        profileImageUrl: imageUrl,
        updatedAt: admin.firestore.Timestamp.now(),
      });

      logger.info("User profile image updated", {
        userId,
        imageUrl,
      });
    } catch (error) {
      logger.error("Failed to update user profile image", {
        userId,
        imageUrl,
        error: error instanceof Error ? error.message : String(error),
      });
      throw error;
    }
  }

  /**
   * 사용자의 현재 프로필 이미지 URL을 가져옵니다.
   *
   * @param {string} userId 사용자 ID
   * @return {Promise<string | null>} 현재 프로필 이미지 URL (없으면 null)
   */
  async getCurrentProfileImageUrl(userId: string): Promise<string | null> {
    try {
      const userDoc = await this.firestore.collection("users").doc(userId).get();

      if (userDoc.exists) {
        const userData = userDoc.data();
        return userData?.profileImageUrl || null;
      }

      return null;
    } catch (error) {
      logger.error("Failed to get current profile image URL", {
        userId,
        error: error instanceof Error ? error.message : String(error),
      });
      throw error;
    }
  }
}

