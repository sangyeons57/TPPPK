/**
 * Session management function handler
 * Handles session validation and authentication status
 */

import {onCall, CallableRequest} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import {container} from "../../infrastructure/di/Container";
import {FunctionError, FunctionErrorCode, ErrorResponse} from "../../shared/types/common";
import {FUNCTION_REGION, FUNCTION_MEMORY, FUNCTION_TIMEOUT} from "../../constants";

interface SessionRequest {
  action: "check" | "status" | "logout";
}

interface SessionResponse {
  isAuthenticated: boolean;
  session?: {
    userId: string;
    email: string;
    isEmailVerified: boolean;
    loginAt: string;
    expiresAt: string;
  };
  message?: string;
}

export const session = onCall<SessionRequest, SessionResponse | ErrorResponse>(
  {
    region: FUNCTION_REGION,
    timeoutSeconds: FUNCTION_TIMEOUT.STANDARD,
    memory: FUNCTION_MEMORY.SMALL,
  },
  async (request: CallableRequest<SessionRequest>) => {
    const requestId = `session-${Date.now()}`;
    const startTime = new Date();

    logger.info("Session function called", {
      requestId,
      action: request.data?.action,
      userId: request.auth?.uid,
      structuredData: true,
    });

    try {
      const action = request.data?.action || "check";

      // Get use cases from container
      const authSessionUseCases = container.getAuthSessionUseCases();

      switch (action) {
      case "check":
        // Validate current session
        const sessionResult = await authSessionUseCases.checkSessionUseCase.execute({
          requestId,
          authContext: request.auth ? {
            userId: request.auth.uid,
            email: request.auth.token.email || "",
            isEmailVerified: request.auth.token.email_verified || false,
            customClaims: request.auth.token,
          } : undefined,
        });

        if (!sessionResult.isSuccess) {
          return {
            isAuthenticated: false,
            message: "Session validation failed",
          } as SessionResponse;
        }

        const session = sessionResult.getOrThrow();
        return {
          isAuthenticated: true,
          session: {
            userId: session.userId.value,
            email: session.email.value,
            isEmailVerified: session.isEmailVerified,
            loginAt: session.loginAt.toISOString(),
            expiresAt: session.expiresAt.toISOString(),
          },
        } as SessionResponse;

      case "status":
        // Check authentication status
        const statusResult = await authSessionUseCases.checkAuthenticationStatusUseCase.execute({
          requestId,
          authContext: request.auth ? {
            userId: request.auth.uid,
            email: request.auth.token.email || "",
            isEmailVerified: request.auth.token.email_verified || false,
            customClaims: request.auth.token,
          } : undefined,
        });

        if (!statusResult.isSuccess) {
          logger.error("Session status check error", {
            requestId,
            error: statusResult.getOrNull()?.message,
          });

          return {
            isAuthenticated: false,
            message: "Status check failed",
          } as SessionResponse;
        }

        return {
          isAuthenticated: statusResult.getOrThrow(),
          message: statusResult.getOrThrow() ? "User is authenticated" : "User is not authenticated",
        } as SessionResponse;

      case "logout":
        // Logout user
        const logoutResult = await authSessionUseCases.logoutUseCase.execute({
          requestId,
          authContext: request.auth ? {
            userId: request.auth.uid,
            email: request.auth.token.email || "",
            isEmailVerified: request.auth.token.email_verified || false,
            customClaims: request.auth.token,
          } : undefined,
        });

        if (!logoutResult.isSuccess) {
          logger.error("Logout error", {
            requestId,
            userId: request.auth?.uid,
            error: logoutResult.getOrNull()?.message,
          });

          return {
            success: false,
            timestamp: new Date(),
            error: {
              code: FunctionErrorCode.INTERNAL_ERROR,
              message: "Logout failed",
            },
          } as ErrorResponse;
        }

        logger.info("User logged out", {
          requestId,
          userId: request.auth?.uid,
        });

        return {
          isAuthenticated: false,
          message: "Logout successful",
        } as SessionResponse;

      default:
        return {
          success: false,
          timestamp: new Date(),
          error: {
            code: FunctionErrorCode.BAD_REQUEST,
            message: "Invalid action",
            details: {validActions: ["check", "status", "logout"]},
          },
        } as ErrorResponse;
      }
    } catch (error) {
      logger.error("Session function unexpected error", {
        requestId,
        action: request.data?.action,
        userId: request.auth?.uid,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });

      return {
        success: false,
        timestamp: new Date(),
        error: {
          code: FunctionErrorCode.INTERNAL_ERROR,
          message: "An unexpected error occurred",
        },
      } as ErrorResponse;
    }
  }
);
