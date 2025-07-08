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
      const {projectId, userId, removedBy} = request.data as RemoveMemberRequest;

      if (!projectId || !userId || !removedBy) {
        throw new HttpsError("invalid-argument", "Project ID, user ID, and removed by are required");
      }

      const memberUseCases = Providers.getMemberProvider().create({projectId});

      const result = await memberUseCases.removeMemberUseCase.execute({
        projectId,
        userId,
        removedBy
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

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to remove member: ${error instanceof Error ? error.message : "Unknown error"}`);
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
      const {projectId, userId, blockedBy} = request.data as BlockMemberRequest;

      if (!projectId || !userId || !blockedBy) {
        throw new HttpsError("invalid-argument", "Project ID, user ID, and blocked by are required");
      }

      const memberUseCases = Providers.getMemberProvider().create({projectId});

      const result = await memberUseCases.blockMemberUseCase.execute({
        projectId,
        userId,
        blockedBy
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

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to block member: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);

// Leave Member Function (Self-removal)
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
      const {projectId, userId} = request.data as LeaveMemberRequest;

      if (!projectId || !userId) {
        throw new HttpsError("invalid-argument", "Project ID and user ID are required");
      }

      const memberUseCases = Providers.getMemberProvider().create({projectId});

      const result = await memberUseCases.leaveMemberUseCase.execute({
        projectId,
        userId
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to leave project: ${error instanceof Error ? error.message : "Unknown error"}`);
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
      const {projectId, deletedBy} = request.data as DeleteProjectRequest;

      if (!projectId || !deletedBy) {
        throw new HttpsError("invalid-argument", "Project ID and deleted by are required");
      }

      const memberUseCases = Providers.getMemberProvider().create({projectId});

      const result = await memberUseCases.deleteProjectUseCase.execute({
        projectId,
        deletedBy
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("permission") || result.error.message.includes("Only")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to delete project: ${error instanceof Error ? error.message : "Unknown error"}`);
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
      const {projectId, inviterId, expiresInHours = 24, maxUses} = request.data as GenerateInviteLinkRequest;

      if (!projectId || !inviterId) {
        throw new HttpsError("invalid-argument", "Project ID and inviter ID are required");
      }

      if (expiresInHours <= 0) {
        throw new HttpsError("invalid-argument", "Expires in hours must be greater than 0");
      }

      if (maxUses !== undefined && maxUses <= 0) {
        throw new HttpsError("invalid-argument", "Max uses must be greater than 0");
      }

      const memberUseCases = Providers.getMemberProvider().create({projectId});

      const result = await memberUseCases.generateInviteLinkUseCase.execute({
        projectId,
        inviterId,
        expiresInHours,
        maxUses
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("permission") || result.error.message.includes("not an active member")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to generate invite link: ${error instanceof Error ? error.message : "Unknown error"}`);
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
      const {inviteCode, userId} = request.data as ValidateInviteCodeRequest;

      if (!inviteCode) {
        throw new HttpsError("invalid-argument", "Invite code is required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.validateInviteCodeUseCase.execute({
        inviteCode,
        userId
      });

      if (!result.success) {
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to validate invite code: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);

// Join Project With Invite Function
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
      const {inviteCode, userId} = request.data as JoinProjectWithInviteRequest;

      if (!inviteCode || !userId) {
        throw new HttpsError("invalid-argument", "Invite code and user ID are required");
      }

      const memberUseCases = Providers.getMemberProvider().create();

      const result = await memberUseCases.joinProjectWithInviteUseCase.execute({
        inviteCode,
        userId
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("already a member") || result.error.message.includes("already has an active project wrapper")) {
          throw new HttpsError("already-exists", result.error.message);
        }
        if (result.error.message.includes("expired") || result.error.message.includes("revoked") || result.error.message.includes("maximum")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return result.data;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", `Failed to join project with invite: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  }
);