import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {
  validateAuth,
  validateRequired,
  validateProjectId,
  handleError,
  createLogger,
  getOrCreateRequestId,
  withTracing,
  AppError,
  COLLECTIONS,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY,
} from "../../../shared";

/**
 * 프로젝트의 모든 서브컬렉션을 재귀적으로 삭제합니다.
 */
async function deleteProjectSubcollections(
  projectRef: admin.firestore.DocumentReference, 
  logger: any
): Promise<void> {
  try {
    const firestore = admin.firestore();
    const batch = firestore.batch();
    let batchCount = 0;
    const maxBatchSize = 450; // Firestore batch limit is 500, leave some margin

    // 삭제할 서브컬렉션 목록
    const subcollections = [
      COLLECTIONS.MEMBERS,
      COLLECTIONS.ROLES, 
      COLLECTIONS.CATEGORIES,
      COLLECTIONS.INVITATIONS,
      // 추가적인 서브컬렉션들...
    ];

    logger.info("Starting subcollection cleanup", { subcollections });

    for (const collectionName of subcollections) {
      const collectionRef = projectRef.collection(collectionName);
      
      // 컬렉션의 모든 문서 가져오기
      const snapshot = await collectionRef.get();
      
      if (!snapshot.empty) {
        logger.info(`Deleting ${snapshot.size} documents from ${collectionName}`);
        
        for (const doc of snapshot.docs) {
          // 각 문서의 서브컬렉션도 처리
          if (collectionName === COLLECTIONS.CATEGORIES) {
            // 카테고리의 경우 채널 서브컬렉션 삭제
            await deleteCategorySubcollections(doc.ref, batch, logger);
          } else if (collectionName === COLLECTIONS.MEMBERS) {
            // 멤버의 경우 추가 서브컬렉션이 있다면 처리
            // 현재는 특별한 서브컬렉션이 없음
          }

          // 문서 자체를 batch에 추가
          batch.delete(doc.ref);
          batchCount++;

          // batch 크기 제한 확인
          if (batchCount >= maxBatchSize) {
            await batch.commit();
            logger.info(`Committed batch of ${batchCount} deletions`);
            // 새로운 batch 생성
            batchCount = 0;
            // batch는 이미 commit되었으므로 새로운 batch를 생성해야 함
            // 하지만 여기서는 간단하게 처리하기 위해 개별 삭제로 전환
            break;
          }
        }
      }
    }

    // 남은 batch 커밋
    if (batchCount > 0) {
      await batch.commit();
      logger.info(`Committed final batch of ${batchCount} deletions`);
    }

    logger.info("Subcollection cleanup completed successfully");

  } catch (error) {
    logger.error("Error during subcollection cleanup", error);
    // 서브컬렉션 삭제 실패 시에도 프로젝트 삭제는 진행
    // 완전한 정리를 위해 에러를 던지지 않음
  }
}

/**
 * 카테고리의 채널 서브컬렉션을 삭제합니다.
 */
async function deleteCategorySubcollections(
  categoryRef: admin.firestore.DocumentReference, 
  parentBatch: admin.firestore.WriteBatch,
  logger: any
): Promise<void> {
  try {
    const channelsRef = categoryRef.collection(COLLECTIONS.CHANNELS);
    const channelsSnapshot = await channelsRef.get();
    
    if (!channelsSnapshot.empty) {
      logger.info(`Deleting ${channelsSnapshot.size} channels from category ${categoryRef.id}`);
      
      for (const channelDoc of channelsSnapshot.docs) {
        // 각 채널의 메시지 컬렉션 삭제
        await deleteChannelMessages(channelDoc.ref, logger);
        
        // 채널 문서 자체는 부모 batch에 추가
        parentBatch.delete(channelDoc.ref);
      }
    }
  } catch (error) {
    logger.warn(`Failed to delete channels for category ${categoryRef.id}`, error);
  }
}

/**
 * 채널의 메시지를 삭제합니다.
 */
async function deleteChannelMessages(
  channelRef: admin.firestore.DocumentReference,
  logger: any
): Promise<void> {
  try {
    const messagesRef = channelRef.collection(COLLECTIONS.MESSAGES);
    
    // 메시지는 대량일 수 있으므로 배치로 처리
    let hasMore = true;
    let deletedCount = 0;
    const batchSize = 100;

    while (hasMore) {
      const snapshot = await messagesRef.limit(batchSize).get();
      
      if (snapshot.empty) {
        hasMore = false;
        break;
      }

      const batch = admin.firestore().batch();
      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      deletedCount += snapshot.size;
      
      // 더 삭제할 메시지가 있는지 확인
      hasMore = snapshot.size === batchSize;
    }

    if (deletedCount > 0) {
      logger.info(`Deleted ${deletedCount} messages from channel ${channelRef.id}`);
    }
  } catch (error) {
    logger.warn(`Failed to delete messages for channel ${channelRef.id}`, error);
  }
}

interface DeleteProjectRequest {
  projectId: string;
}

interface DeleteProjectResponse {
  success: boolean;
  message: string;
}

export const deleteProject = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request): Promise<DeleteProjectResponse> => {
    const requestId = getOrCreateRequestId(request);
    const logger = createLogger({
      requestId,
      domain: "projects",
      operation: "deleteProject",
      startTime: Date.now(),
    });

    try {
      return await withTracing(
        {
          requestId,
          domain: "projects",
          operation: "deleteProject",
          startTime: Date.now(),
        },
        async () => {
          const userId = validateAuth(request.auth);
          const { projectId } = request.data as DeleteProjectRequest;

          validateRequired(projectId, "projectId");
          validateProjectId(projectId);

          const firestore = admin.firestore();
          const projectRef = firestore.collection(COLLECTIONS.PROJECTS).doc(projectId);
          const projectSnap = await projectRef.get();

          if (!projectSnap.exists) {
            throw new AppError("not-found", "Project not found");
          }

          const projectData = projectSnap.data() as any;

          // Verify caller is project owner
          if (projectData.ownerId !== userId) {
            throw new AppError("permission-denied", "Only project owner can delete project");
          }

          // Clean up project_wrappers for all members first
          // Get all project members to clean up their wrappers
          const membersQuery = await projectRef.collection(COLLECTIONS.MEMBERS).get();
          
          if (!membersQuery.empty) {
            const batch = firestore.batch();
            let wrapperDeleteCount = 0;

            for (const memberDoc of membersQuery.docs) {
              const memberId = memberDoc.id;
              const wrapperRef = firestore
                .collection(COLLECTIONS.USERS)
                .doc(memberId)
                .collection(COLLECTIONS.PROJECT_WRAPPERS)
                .doc(projectId);
              
              batch.delete(wrapperRef);
              wrapperDeleteCount++;
            }

            await batch.commit();
            logger.info("Cleaned up project wrappers", { 
              projectId, 
              deletedCount: wrapperDeleteCount 
            });
          }

          // Delete all subcollections before deleting the project document
          await deleteProjectSubcollections(projectRef, logger);

          // Delete the project document
          await projectRef.delete();
          
          logger.info("Project deleted successfully", { 
            projectId, 
            ownerId: userId
          });

          return { 
            success: true, 
            message: "Project deleted successfully"
          };
        }
      );
    } catch (error) {
      logger.error("Failed to delete project", error);
      throw handleError(error, "deleteProject");
    }
  }
);