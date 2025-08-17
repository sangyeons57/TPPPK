import { onCall } from "firebase-functions/v2/https";
import { 
  createLogger,
  RUNTIME_CONFIG,
  FUNCTION_MEMORY
} from "../../../shared";

export const helloWorld = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: FUNCTION_MEMORY.SMALL as any,
    timeoutSeconds: 30,
  },
  async (request) => {
    const logger = createLogger({
      domain: "system",
      operation: "helloWorld",
      startTime: Date.now(),
    });

    logger.info("Hello World function called", { 
      authUid: request.auth?.uid,
      data: request.data 
    });

    return {
      message: "Hello from simplified Firebase Functions!",
      timestamp: new Date().toISOString(),
      version: "2.0.0-simplified",
      auth: !!request.auth?.uid,
      region: RUNTIME_CONFIG.REGION,
      architecture: "domains + shared",
    };
  }
);