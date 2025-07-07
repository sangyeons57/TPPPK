import {onDocumentWritten, onDocumentDeleted} from "firebase-functions/v2/firestore";
import {logger} from "firebase-functions";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

/**
 * Project collection의 문서 변경을 감지하여 모든 멤버의 ProjectWrapper를 동기화
 */
export const onProjectChange = onDocumentWritten(
  {
    document: "projects/{projectId}",
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
  },
  async (event) => {
    const projectId = event.params.projectId;
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();

    logger.info(`Project change detected for projectId: ${projectId}`);

    try {
      // 문서가 삭제된 경우
      if (!afterData) {
        logger.info(`Project ${projectId} was deleted, skipping wrapper sync`);
        return;
      }

      // 새로 생성된 문서인 경우 (동기화할 기존 wrapper가 없음)
      if (!beforeData) {
        logger.info(`Project ${projectId} was created, no existing wrappers to sync`);
        return;
      }

      // 변경된 필드 확인
      const updates: { name?: string; imageUrl?: string } = {};
      let hasChanges = false;

      if (beforeData.name !== afterData.name) {
        updates.name = afterData.name;
        hasChanges = true;
        logger.info(`Project name changed: ${beforeData.name} -> ${afterData.name}`);
      }

      if (beforeData.imageUrl !== afterData.imageUrl) {
        updates.imageUrl = afterData.imageUrl;
        hasChanges = true;
        logger.info(`Project image changed: ${beforeData.imageUrl} -> ${afterData.imageUrl}`);
      }

      // 변경사항이 없으면 동기화하지 않음
      if (!hasChanges) {
        logger.info("No relevant changes detected, skipping wrapper sync");
        return;
      }

      // ProjectWrapper 동기화 UseCase 실행
      const projectWrapperProvider = Providers.getProjectWrapperProvider();
      const projectWrapperUseCases = projectWrapperProvider.create();

      const result = await projectWrapperUseCases.syncProjectWrapperUseCase.execute({
        projectId,
        updates,
      });

      if (result.success) {
        logger.info(`Successfully synced ${result.data.updatedMemberCount} project wrappers for project ${projectId}`);

        if (result.data.errors && result.data.errors.length > 0) {
          logger.warn("Some errors occurred during sync:", result.data.errors);
        }
      } else {
        logger.error(`Failed to sync project wrappers for project ${projectId}:`, result.error);
      }
    } catch (error) {
      logger.error(`Unexpected error in project sync trigger for ${projectId}:`, error);
    }
  }
);

/**
 * Project collection의 문서 삭제를 감지하여 관련 ProjectWrapper들을 정리
 */
export const onProjectDelete = onDocumentDeleted(
  {
    document: "projects/{projectId}",
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
  },
  async (event) => {
    const projectId = event.params.projectId;

    logger.info(`Project delete detected for projectId: ${projectId}`);

    try {
      // ProjectWrapper 정리
      const projectWrapperProvider = Providers.getProjectWrapperProvider();
      const projectWrapperUseCases = projectWrapperProvider.create();

      const deleteResult = await projectWrapperUseCases.projectWrapperRepository.deleteAllByProjectId(projectId);

      if (deleteResult.success) {
        logger.info(`Successfully cleaned up project wrappers for deleted project ${projectId}`);
      } else {
        logger.error(`Failed to clean up project wrappers for deleted project ${projectId}:`, deleteResult.error);
      }
    } catch (error) {
      logger.error(`Unexpected error in project delete trigger for ${projectId}:`, error);
    }
  }
);
