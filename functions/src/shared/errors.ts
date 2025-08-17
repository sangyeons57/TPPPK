import { HttpsError } from "firebase-functions/v2/https";

export class AppError extends Error {
  constructor(
    public code: string,
    message: string,
    public details?: unknown
  ) {
    super(message);
    this.name = "AppError";
  }
}

export const handleError = (error: unknown, context: string): HttpsError => {
  console.error(`Error in ${context}:`, error);

  if (error instanceof AppError) {
    return new HttpsError(error.code as any, error.message, error.details);
  }

  if (error instanceof HttpsError) {
    return error;
  }

  if (error instanceof Error) {
    return new HttpsError("internal", error.message);
  }

  return new HttpsError("internal", "Unknown error occurred");
};

// Basic validation moved to validation.ts to avoid conflicts