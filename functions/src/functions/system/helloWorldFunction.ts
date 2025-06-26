/**
 * Hello World function handler
 * Demonstrates the new modular architecture
 */

import { onCall, CallableRequest } from 'firebase-functions/v2/https';
import * as logger from 'firebase-functions/logger';
import { container } from '../../infrastructure/di/Container';
import { FunctionError, FunctionErrorCode, SuccessResponse, ErrorResponse } from '../../shared/types/common';

interface HelloWorldRequest {
  message?: string;
}

interface HelloWorldResponse {
  message: string;
}

export const helloWorld = onCall<HelloWorldRequest, HelloWorldResponse | ErrorResponse>(
  {
    region: 'asia-northeast3',
    timeoutSeconds: 30,
    memory: '256MiB',
  },
  async (request: CallableRequest<HelloWorldRequest>) => {
    const requestId = `hello-${Date.now()}`;
    const startTime = new Date();

    logger.info('HelloWorld function called', {
      requestId,
      data: request.data,
      auth: request.auth?.uid,
      structuredData: true,
    });

    try {
      // Get use cases from container
      const functionsUseCases = container.getFunctionsUseCases();

      let result;
      
      if (request.data?.message) {
        // Call with custom message
        result = await functionsUseCases.helloWorldUseCase.executeWithCustomMessage(
          request.data.message,
          {
            requestId,
            authContext: request.auth ? {
              userId: request.auth.uid,
              email: request.auth.token.email || '',
              isEmailVerified: request.auth.token.email_verified || false,
              customClaims: request.auth.token,
            } : undefined,
          }
        );
      } else {
        // Call basic hello world
        result = await functionsUseCases.helloWorldUseCase.execute({
          requestId,
          authContext: request.auth ? {
            userId: request.auth.uid,
            email: request.auth.token.email || '',
            isEmailVerified: request.auth.token.email_verified || false,
            customClaims: request.auth.token,
          } : undefined,
        });
      }

      if (!result.isSuccess) {
        const error = result.getOrNull();
        logger.error('HelloWorld function error', {
          requestId,
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
            message: 'Internal server error',
          },
        } as ErrorResponse;
      }

      const message = result.getOrThrow();
      const endTime = new Date();
      const duration = endTime.getTime() - startTime.getTime();

      logger.info('HelloWorld function completed', {
        requestId,
        duration,
        message,
      });

      return {
        message,
      } as HelloWorldResponse;

    } catch (error) {
      logger.error('HelloWorld function unexpected error', {
        requestId,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });

      return {
        success: false,
        timestamp: new Date(),
        error: {
          code: FunctionErrorCode.INTERNAL_ERROR,
          message: 'An unexpected error occurred',
        },
      } as ErrorResponse;
    }
  }
);