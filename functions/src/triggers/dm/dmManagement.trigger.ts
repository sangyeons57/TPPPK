import {onCall, HttpsError} from "firebase-functions/v2/https";
import {RUNTIME_CONFIG} from "../../core/constants";
import {Providers} from "../../config/dependencies";

// Create DM Channel Function
interface CreateDMChannelRequest {
  currentUserId: string;
  targetUserName: string;
}

export const createDMChannelFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        currentUserId,
        targetUserName,
      } = request.data as CreateDMChannelRequest;

      if (!currentUserId || !targetUserName) {
        throw new HttpsError("invalid-argument", "Current user ID and target user name are required");
      }

      const dmUseCases = Providers.getDmProvider().create();

      const result = await dmUseCases.createDMChannelUseCase.execute({
        currentUserId,
        targetUserName,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("already exists")) {
          throw new HttpsError("already-exists", result.error.message);
        }
        if (result.error.message.includes("Cannot create DM with yourself")) {
          throw new HttpsError("invalid-argument", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in createDMChannel:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

// Block DM Channel Function
interface BlockDMChannelRequest {
  currentUserId: string;
  channelId: string;
}

// Unblock DM Channel Function
interface UnblockDMChannelRequest {
  currentUserId: string;
  channelId: string;
}

export const blockDMChannelFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        currentUserId,
        channelId,
      } = request.data as BlockDMChannelRequest;

      if (!currentUserId || !channelId) {
        throw new HttpsError("invalid-argument", "Current user ID and channel ID are required");
      }

      const dmUseCases = Providers.getDmProvider().create();

      const result = await dmUseCases.blockDMChannelUseCase.execute({
        currentUserId,
        channelId,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("already blocked")) {
          throw new HttpsError("already-exists", result.error.message);
        }
        if (result.error.message.includes("not a participant")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in blockDMChannel:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);

export const unblockDMChannelFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request) => {
    try {
      const {
        currentUserId,
        channelId,
      } = request.data as UnblockDMChannelRequest;

      if (!currentUserId || !channelId) {
        throw new HttpsError("invalid-argument", "Current user ID and channel ID are required");
      }

      const dmUseCases = Providers.getDmProvider().create();

      const result = await dmUseCases.unblockDMChannelUseCase.execute({
        currentUserId,
        channelId,
      });

      if (!result.success) {
        if (result.error.message.includes("not found")) {
          throw new HttpsError("not-found", result.error.message);
        }
        if (result.error.message.includes("not_blocked")) {
          throw new HttpsError("failed-precondition", result.error.message);
        }
        if (result.error.message.includes("not a participant")) {
          throw new HttpsError("permission-denied", result.error.message);
        }
        throw new HttpsError("internal", result.error.message);
      }

      return {success: true, data: result.data};
    } catch (error) {
      console.error("Error in unblockDMChannel:", error);
      if (error instanceof HttpsError) {
        throw error;
      }
      throw new HttpsError("internal", "Internal server error");
    }
  }
);
