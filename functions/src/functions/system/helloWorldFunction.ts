/**
 * Hello World function handler
 * Demonstrates the new modular architecture
 */

import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import {FUNCTION_REGION, FUNCTION_MEMORY, FUNCTION_TIMEOUT} from "../../constants";

interface HelloWorldRequest {
  message?: string;
}


export const helloWorld = onCall<HelloWorldRequest>(
  {
    region: FUNCTION_REGION,
    timeoutSeconds: FUNCTION_TIMEOUT.STANDARD,
    memory: FUNCTION_MEMORY.SMALL,
  },
  async (request) => {
    const requestId = `hello-${Date.now()}`;

    logger.info("HelloWorld function called", {
      requestId,
      data: request.data,
      auth: request.auth?.uid,
      structuredData: true,
    });

    try {
      const message = request.data?.message;
      let responseMessage = "Hello from Firebase!";

      if (message) {
        responseMessage = `Hello, ${message}!`;
      }

      logger.info("HelloWorld function completed", {
        requestId,
        responseMessage,
      });

      return {
        message: responseMessage,
      };
    } catch (error) {
      logger.error("HelloWorld function unexpected error", {
        requestId,
        error: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      });
      // Re-throw the error to be handled by the Functions framework
      throw new HttpsError("internal", "An unexpected error occurred.");
    }
  }
);
