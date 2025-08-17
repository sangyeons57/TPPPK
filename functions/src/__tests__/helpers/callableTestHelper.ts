import { CallableRequest } from "firebase-functions/v2/https";

/**
 * Wrapper for testing Firebase onCall functions
 * TypeScript incorrectly infers onCall functions as HTTP handlers, 
 * but they're actually callable functions that take only a request parameter.
 * This wrapper provides proper typing for tests.
 */
export const testCallable = async <T, R>(
  callableFunction: any,
  request: CallableRequest<T>
): Promise<R> => {
  // @ts-ignore - Firebase onCall functions have incorrect TypeScript definitions in test environment
  return await callableFunction(request);
};