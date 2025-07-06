export class DomainError extends Error {
  constructor(message: string, public readonly code: string) {
    super(message);
    this.name = "DomainError";
  }
}

export class ValidationError extends DomainError {
  constructor(field: string, message: string) {
    super(`Validation failed for ${field}: ${message}`, "VALIDATION_ERROR");
    this.name = "ValidationError";
  }
}

export class NotFoundError extends DomainError {
  constructor(resource: string, id: string) {
    super(`${resource} with id ${id} not found`, "NOT_FOUND");
    this.name = "NotFoundError";
  }
}

export class UnauthorizedError extends DomainError {
  constructor(message = "Unauthorized access") {
    super(message, "UNAUTHORIZED");
    this.name = "UnauthorizedError";
  }
}

export class DatabaseError extends DomainError {
  constructor(message: string, details?: string) {
    super(details ? `${message}: ${details}` : message, "DATABASE_ERROR");
    this.name = "DatabaseError";
  }
}

export class ConflictError extends DomainError {
  constructor(resource: string, field: string, value: string) {
    super(`${resource} with ${field} ${value} already exists`, "CONFLICT");
    this.name = "ConflictError";
  }
}

export class InternalError extends DomainError {
  constructor(message = "Internal server error") {
    super(message, "INTERNAL_ERROR");
    this.name = "InternalError";
  }
}
