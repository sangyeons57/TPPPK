import {onCall, HttpsError} from "firebase-functions/v2/https";
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
      const {
        projectId,
        deletedBy,
      } = request.data as DeleteProjectRequest;

      if (!projectId || !deletedBy) {
        throw new HttpsError("invalid-argument", "Project ID and deletedBy are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.deleteProjectUseCase.execute({
        projectId,
        deletedBy,
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in deleteProject:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
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
