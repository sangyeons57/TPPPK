import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { RUNTIME_CONFIG } from '../../core/constants';

interface HelloWorldRequest {
  name?: string;
}

interface HelloWorldResponse {
  message: string;
  timestamp: string;
}

export const helloWorldFunction = onCall(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
  },
  async (request): Promise<HelloWorldResponse> => {
    try {
      const { name } = request.data as HelloWorldRequest;
      
      const message = name ? `Hello, ${name}!` : 'Hello, World!';
      
      return {
        message,
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      throw new HttpsError('internal', `Hello World failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }
);