import {onCall, HttpsError} from "firebase-functions/v2/https";
import {logger} from "firebase-functions/v2";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

// Remove Member Function
interface RemoveMemberRequest {
  projectId: string;
  userId: string;
  removedBy: string;
}

export const removeMemberFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        projectId,
        userId,
        removedBy,
      } = request.data as RemoveMemberRequest;

      if (!projectId || !userId || !removedBy) {
        throw new HttpsError("invalid-argument", "Project ID, user ID, and removedBy are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.removeMemberUseCase.execute({
        projectId,
        userId,
        removedBy,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("permission") || result.error.message.includes("Only")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        if (result.error.message.includes("Cannot remove yourself")) {
          throw new HttpsError("invalid-argument", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in removeMember:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Block Member Function
interface BlockMemberRequest {
  projectId: string;
  userId: string;
  blockedBy: string;
}

export const blockMemberFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        projectId,
        userId,
        blockedBy,
      } = request.data as BlockMemberRequest;

      if (!projectId || !userId || !blockedBy) {
        throw new HttpsError("invalid-argument", "Project ID, user ID, and blockedBy are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.blockMemberUseCase.execute({
        projectId,
        userId,
        blockedBy,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("permission") || result.error.message.includes("Only")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        if (result.error.message.includes("Cannot block yourself")) {
          throw new HttpsError("invalid-argument", result.error.message);
        }
        if (result.error.message.includes("already blocked")) {
          throw new HttpsError("failed-precondition", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in blockMember:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Leave Member Function
interface LeaveMemberRequest {
  projectId: string;
  userId: string;
}

export const leaveMemberFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        projectId,
        userId,
      } = request.data as LeaveMemberRequest;

      if (!projectId || !userId) {
        throw new HttpsError("invalid-argument", "Project ID and user ID are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.leaveMemberUseCase.execute({
        projectId,
        userId,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in leaveMember:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Delete Project Function
interface DeleteProjectRequest {
  projectId: string;
  deletedBy: string;
}

export const deleteProjectFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      logger.info("🚀 Starting deleteProject function");

      const {
        projectId,
        deletedBy,
      } = request.data as DeleteProjectRequest;

      logger.info(`📝 Request data: projectId=${projectId}, deletedBy=${deletedBy}`);

      if (!projectId || !deletedBy) {
        logger.error(`❌ Missing required parameters: projectId=${projectId}, deletedBy=${deletedBy}`);
        throw new HttpsError("invalid-argument", "Project ID and deletedBy are required");
      }

      logger.info(`🔧 Creating member use cases provider for projectId=${projectId}`);
      const memberUseCases = Providers.getMemberProvider().create({
        projectId: projectId,
      });

      logger.info(`🔍 Checking member existence before deletion for user=${deletedBy} in project=${projectId}, expectedPath=projects/${projectId}/members`);

      // 먼저 멤버가 존재하는지 확인
      const memberCheckResult = await memberUseCases.memberRepository.findByUserId(deletedBy);

      logger.info(`👤 Member existence check result: success=${memberCheckResult?.success}, error=${!memberCheckResult.success ? memberCheckResult.error?.message : "none"}, projectId=${projectId}, userId=${deletedBy}`);

      logger.info("⚙️ Executing deleteProjectUseCase");
      const result = await memberUseCases.deleteProjectUseCase.execute({
        projectId,
        deletedBy,
      });

      logger.info(`📊 UseCase result: success=${result.success}`);

      if (!result.success) {
        // Check for specific error types to provide better error messages
        const errorMessage = result.error?.message || "Unknown error";
        const errorName = result.error?.name || "UnknownError";
        logger.error(`💥 UseCase error details: name=${errorName}, message=${errorMessage}`);
        logger.error(`❌ Delete project failed: ${errorMessage}`);

        // Map specific error types to appropriate HTTP errors
        if (errorMessage.includes("not found")) {
          throw new HttpsError("not-found", errorMessage);
        }
        if (errorMessage.includes("permission") || errorMessage.includes("Only project members")) {
          throw new HttpsError("permission-denied", errorMessage);
        }
        if (errorMessage.includes("validation") || errorMessage.includes("required")) {
          throw new HttpsError("invalid-argument", errorMessage);
        }

        throw new HttpsError("internal", `Delete project failed: ${errorMessage}`);
      }

      logger.info("✅ Delete project completed successfully");
      return {success: true, data: result.data};
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Unknown internal error";
      const errorName = error instanceof Error ? error.name : "UnknownError";
      logger.error(`💥 Error in deleteProject function: name=${errorName}, message=${errorMessage}`);
      if (error instanceof HttpsError) {
        throw error;
      }
      // Provide more specific error information
      logger.error(`🔥 Unhandled error: ${errorMessage}`);
      throw new HttpsError("internal", `Internal server error: ${errorMessage}`);
    }
  }
);

// Generate Invite Link Function
interface GenerateInviteLinkRequest {
  projectId: string;
  inviterId: string;
  expiresInHours?: number;
  maxUses?: number;
}

export const generateInviteLinkFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        projectId,
        inviterId,
        expiresInHours = 24,
        maxUses,
      } = request.data as GenerateInviteLinkRequest;

      if (!projectId || !inviterId) {
        throw new HttpsError("invalid-argument", "Project ID and inviter ID are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.generateInviteLinkUseCase.execute({
        projectId,
        inviterId,
        expiresInHours,
        maxUses,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in generateInviteLink:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Validate Invite Code Function
interface ValidateInviteCodeRequest {
  inviteCode: string;
  userId?: string;
}

export const validateInviteCodeFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        inviteCode,
        userId,
      } = request.data as ValidateInviteCodeRequest;

      if (!inviteCode) {
        throw new HttpsError("invalid-argument", "Invite code is required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.validateInviteCodeUseCase.execute({
        inviteCode,
        userId,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in validateInviteCode:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Join Project with Invite Function
interface JoinProjectWithInviteRequest {
  inviteCode: string;
  userId: string;
}

export const joinProjectWithInviteFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        inviteCode,
        userId,
      } = request.data as JoinProjectWithInviteRequest;

      if (!inviteCode || !userId) {
        throw new HttpsError("invalid-argument", "Invite code and user ID are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.joinProjectWithInviteUseCase.execute({
        inviteCode,
        userId,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in joinProjectWithInvite:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);
