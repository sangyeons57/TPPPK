import { onDocumentWritten, onDocumentDeleted } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { 
  createLogger, 
  withTracing,
  COLLECTIONS,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY
} from "../../../shared";

export const onProjectChange = onDocumentWritten(
  {
    document: `${COLLECTIONS.PROJECTS}/{projectId}`,
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
  },
  async (event) => {
    const logger = createLogger({
      domain: "projects",
      operation: "onProjectChange",
      startTime: Date.now(),
    });

    try {
      await withTracing(
        {
          requestId: `firestore-${Date.now()}`,
          domain: "projects",
          operation: "onProjectChange",
          startTime: Date.now(),
        },
        async () => {
          const projectId = event.params.projectId;
          const beforeData = event.data?.before?.data();
          const afterData = event.data?.after?.data();

          logger.info("Project change detected", { projectId });

          // 문서가 삭제된 경우
          if (!afterData) {
            logger.info("Project was deleted, skipping wrapper sync", { projectId });
            return;
          }

          // 새로 생성된 문서인 경우
          if (!beforeData) {
            logger.info("Project was created, no existing wrappers to sync", { projectId });
            return;
          }

          // 변경된 필드 확인
          const updates: any = {};
          let hasChanges = false;

          if (beforeData.name !== afterData.name) {
            updates.name = afterData.name;
            hasChanges = true;
            logger.info("Project name changed", { 
              projectId, 
              oldName: beforeData.name, 
              newName: afterData.name 
            });
          }

          if (beforeData.imageUrl !== afterData.imageUrl) {
            updates.imageUrl = afterData.imageUrl;
            hasChanges = true;
            logger.info("Project image changed", { 
              projectId, 
              oldImage: beforeData.imageUrl, 
              newImage: afterData.imageUrl 
            });
          }

          // 변경사항이 없으면 동기화하지 않음
          if (!hasChanges) {
            logger.info("No relevant changes detected, skipping wrapper sync", { projectId });
            return;
          }

          // ProjectWrapper 동기화
          const firestore = admin.firestore();
          const projectWrappersQuery = await firestore
            .collection(COLLECTIONS.PROJECT_WRAPPERS)
            .where("projectId", "==", projectId)
            .get();

          if (projectWrappersQuery.empty) {
            logger.info("No project wrappers found to sync", { projectId });
            return;
          }

          // 배치 업데이트
          const batch = firestore.batch();
          let updateCount = 0;

          projectWrappersQuery.docs.forEach(doc => {
            batch.update(doc.ref, {
              ...updates,
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            updateCount++;
          });

          await batch.commit();

          logger.info("Successfully synced project wrappers", { 
            projectId, 
            updatedCount: updateCount 
          });
        }
      );
    } catch (error) {
      logger.error("Failed to process project change", error);
    }
  }
);

export const onProjectDelete = onDocumentDeleted(
  {
    document: `${COLLECTIONS.PROJECTS}/{projectId}`,
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
  },
  async (event) => {
    const logger = createLogger({
      domain: "projects",
      operation: "onProjectDelete",
      startTime: Date.now(),
    });

    try {
      await withTracing(
        {
          requestId: `firestore-${Date.now()}`,
          domain: "projects",
          operation: "onProjectDelete",
          startTime: Date.now(),
        },
        async () => {
          const projectId = event.params.projectId;

          logger.info("Project delete detected", { projectId });

          const firestore = admin.firestore();

          // ProjectWrapper 정리
          // Wrappers are stored under users/{uid}/projects_wrapper/{projectId}
          // Use collection group query to find all wrapper docs with ID == projectId
          const projectWrappersQuery = await firestore
            .collectionGroup(COLLECTIONS.PROJECT_WRAPPERS)
            .where(admin.firestore.FieldPath.documentId(), "==", projectId)
            .get();

          if (projectWrappersQuery.empty) {
            logger.info("No project wrappers to clean up", { projectId });
            return;
          }

          // 배치 삭제
          const batch = firestore.batch();
          let deleteCount = 0;

          projectWrappersQuery.docs.forEach(doc => {
            batch.delete(doc.ref);
            deleteCount++;
          });

          await batch.commit();

          logger.info("Successfully cleaned up project wrappers", { 
            projectId, 
            deletedCount: deleteCount 
          });
        }
      );
    } catch (error) {
      logger.error("Failed to process project deletion", error);
    }
  }
);
