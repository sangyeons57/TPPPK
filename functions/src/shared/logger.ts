import { randomUUID } from "crypto";

export interface LogContext {
  requestId: string;
  userId?: string;
  domain: string;
  operation: string;
  startTime: number;
}

export interface Logger {
  info(message: string, data?: unknown): void;
  error(message: string, error?: unknown): void;
  warn(message: string, data?: unknown): void;
  debug(message: string, data?: unknown): void;
}

export const createLogger = (context: Partial<LogContext>): Logger => {
  const logContext = {
    requestId: context.requestId || randomUUID(),
    domain: context.domain || "unknown",
    operation: context.operation || "unknown",
    startTime: context.startTime || Date.now(),
    userId: context.userId,
  };

  const formatLog = (level: string, message: string, data?: unknown) => {
    return {
      level,
      message,
      timestamp: new Date().toISOString(),
      requestId: logContext.requestId,
      domain: logContext.domain,
      operation: logContext.operation,
      userId: logContext.userId,
      duration: Date.now() - logContext.startTime,
      data: data || undefined,
    };
  };

  return {
    info: (message: string, data?: unknown) => {
      console.log(JSON.stringify(formatLog("INFO", message, data)));
    },
    error: (message: string, error?: unknown) => {
      console.error(JSON.stringify(formatLog("ERROR", message, error)));
    },
    warn: (message: string, data?: unknown) => {
      console.warn(JSON.stringify(formatLog("WARN", message, data)));
    },
    debug: (message: string, data?: unknown) => {
      console.debug(JSON.stringify(formatLog("DEBUG", message, data)));
    },
  };
};

export const getOrCreateRequestId = (request: any): string => {
  return request?.headers?.["x-request-id"] || randomUUID();
};

export const withTracing = async <T>(
  context: LogContext,
  operation: () => Promise<T>
): Promise<T> => {
  const logger = createLogger(context);
  logger.info(`Starting operation: ${context.operation}`);

  try {
    const result = await operation();
    logger.info(`Operation completed successfully: ${context.operation}`);
    return result;
  } catch (error) {
    logger.error(`Operation failed: ${context.operation}`, error);
    throw error;
  }
};