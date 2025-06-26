/**
 * Sign up function handler
 * Handles user registration with the new architecture
 */

import {onCall, CallableRequest} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import {container} from "../../infrastructure/di/Container";
import {FunctionError, FunctionErrorCode, ErrorResponse} from "../../shared/types/common";
import {SignUpRequest} from "../../domain/usecases/auth/registration/SignUpUseCase";

interface SignUpResponse {
  userId: string;
  success: boolean;
  message: string;
}

export const signUp = onCall<SignUpRequest, SignUpResponse | ErrorResponse>(
  {
    region: "asia-northeast3",
    timeoutSeconds: 60,
    memory: "512MiB",
  },
  async (request: CallableRequest<SignUpRequest>) => {
    const requestId = `signup-${Date.now()}`;
    const startTime = new Date();

    logger.info("SignUp function called", {
      requestId,
      email: request.data?.email,
      name: request.data?.name,
      structuredData: true,
    });

    try {
      // Validate required fields
      if (!request.data?.email || !request.data?.password || !request.data?.name) {
        return {
          success: false,
          timestamp: new Date(),
          error: {
            code: FunctionErrorCode.VALIDATION_ERROR,
            message: "Email, password, and name are required",
            details: {
              missingFields: [
                !request.data?.email && "email",
                !request.data?.password && "password",
                !request.data?.name && "name",
              ].filter(Boolean),
            },
          },
        } as ErrorResponse;
      }

      // Get use cases from container
      const authRegistrationUseCases = container.getAuthRegistrationUseCases();

      const result = await authRegistrationUseCases.signUpUseCase.execute(
        request.data,
        {
          requestId,
          metadata: {
            userAgent: request.rawRequest.get("user-agent"),
            ipAddress: request.rawRequest.ip,
          },
        }
      );

      if (!result.isSuccess) {
        const error = result.getOrNull();
        logger.error("SignUp function error", {
          requestId,
          email: request.data.email,
          error: error?.message,
          stack: error?.stack,
        });

        if (error instanceof FunctionError) {
          return error.toResponse();
        }

        return {
          success: false,
          timestamp: new Date(),
          error: {
            code: FunctionErrorCode.INTERNAL_ERROR,
            message: "Registration failed",
          },
        } as ErrorResponse;
      }

      const userId = result.getOrThrow();
      const endTime = new Date();
      const duration = endTime.getTime() - startTime.getTime();

      logger.info("SignUp function completed", {
        requestId,
        userId,
        email: request.data.email,
        duration,
      });

      return {
        userId,
        success: true,
        message: "Registration successful. Please check your email for verification.",
      } as SignUpResponse;
    } catch (error) {
      logger.error("SignUp function unexpected error", {
        requestId,
        email: request.data?.email,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });

      return {
        success: false,
        timestamp: new Date(),
        error: {
          code: FunctionErrorCode.INTERNAL_ERROR,
          message: "An unexpected error occurred during registration",
        },
      } as ErrorResponse;
    }
  }
);
