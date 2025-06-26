/**
 * Common types used across the Firebase Functions application
 */

// Base interface for all domain entities
export interface Entity {
  readonly id: string;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

// Base interface for value objects
export interface ValueObject {
  readonly value: string | number | boolean;
  equals(other: ValueObject): boolean;
}

// Request/Response base types
export interface BaseRequest {
  readonly timestamp?: Date;
  readonly requestId?: string;
}

export interface BaseResponse {
  readonly success: boolean;
  readonly timestamp: Date;
  readonly message?: string;
}

export interface ErrorResponse extends BaseResponse {
  readonly success: false;
  readonly error: {
    readonly code: string;
    readonly message: string;
    readonly details?: Record<string, any>;
  };
}

export interface SuccessResponse<T = any> extends BaseResponse {
  readonly success: true;
  readonly data: T;
}

// Pagination types
export interface PaginationRequest {
  readonly page?: number;
  readonly size?: number;
  readonly sortBy?: string;
  readonly sortDirection?: 'asc' | 'desc';
}

export interface PaginationResponse<T> {
  readonly items: T[];
  readonly totalItems: number;
  readonly totalPages: number;
  readonly currentPage: number;
  readonly pageSize: number;
  readonly hasNext: boolean;
  readonly hasPrevious: boolean;
}

// Authentication context
export interface AuthContext {
  readonly userId: string;
  readonly email: string;
  readonly isEmailVerified: boolean;
  readonly customClaims?: Record<string, any>;
}

// Repository context for dependency injection
export interface RepositoryContext {
  readonly authContext?: AuthContext;
  readonly transactionId?: string;
  readonly metadata?: Record<string, any>;
}

// Use case context
export interface UseCaseContext {
  readonly authContext?: AuthContext;
  readonly requestId?: string;
  readonly metadata?: Record<string, any>;
}

// Function handler context
export interface FunctionContext {
  readonly authContext?: AuthContext;
  readonly requestId: string;
  readonly startTime: Date;
  readonly metadata?: Record<string, any>;
}

// Validation result
export interface ValidationResult {
  readonly isValid: boolean;
  readonly errors: ValidationError[];
}

export interface ValidationError {
  readonly field: string;
  readonly code: string;
  readonly message: string;
  readonly value?: any;
}

// Domain events
export interface DomainEvent {
  readonly eventId: string;
  readonly eventType: string;
  readonly aggregateId: string;
  readonly aggregateType: string;
  readonly version: number;
  readonly occurredAt: Date;
  readonly data: Record<string, any>;
  readonly metadata?: Record<string, any>;
}

// Function error types
export enum FunctionErrorCode {
  // Client errors (4xx)
  BAD_REQUEST = 'BAD_REQUEST',
  UNAUTHORIZED = 'UNAUTHORIZED',
  FORBIDDEN = 'FORBIDDEN',
  NOT_FOUND = 'NOT_FOUND',
  CONFLICT = 'CONFLICT',
  VALIDATION_ERROR = 'VALIDATION_ERROR',

  // Server errors (5xx)
  INTERNAL_ERROR = 'INTERNAL_ERROR',
  SERVICE_UNAVAILABLE = 'SERVICE_UNAVAILABLE',
  TIMEOUT = 'TIMEOUT',
  DATABASE_ERROR = 'DATABASE_ERROR',
  EXTERNAL_SERVICE_ERROR = 'EXTERNAL_SERVICE_ERROR',
}

export class FunctionError extends Error {
  constructor(
    public readonly code: FunctionErrorCode,
    message: string,
    public readonly details?: Record<string, any>,
    public readonly cause?: Error
  ) {
    super(message);
    this.name = 'FunctionError';
  }

  toResponse(): ErrorResponse {
    return {
      success: false,
      timestamp: new Date(),
      error: {
        code: this.code,
        message: this.message,
        details: this.details,
      },
    };
  }
}

// Utility types
export type Optional<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;
export type RequireAtLeastOne<T, Keys extends keyof T = keyof T> = 
  Pick<T, Exclude<keyof T, Keys>> & {
    [K in Keys]-?: Required<Pick<T, K>> & Partial<Pick<T, Exclude<Keys, K>>>;
  }[Keys];

// Configuration types
export interface AppConfig {
  readonly environment: 'development' | 'staging' | 'production';
  readonly projectId: string;
  readonly region: string;
  readonly database: {
    readonly host: string;
    readonly port: number;
  };
  readonly auth: {
    readonly tokenExpiration: number;
    readonly refreshTokenExpiration: number;
  };
  readonly rateLimit: {
    readonly maxRequests: number;
    readonly windowMs: number;
  };
}