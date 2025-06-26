/**
 * Input validation utilities for Firebase Functions
 * Inspired by the Android domain validation patterns
 */

import { ValidationResult, ValidationError, FunctionError, FunctionErrorCode } from '../types/common';

export class Validator {
  private errors: ValidationError[] = [];

  static create(): Validator {
    return new Validator();
  }

  // Email validation
  email(value: string, field: string = 'email'): this {
    if (!value) {
      this.addError(field, 'REQUIRED', 'Email is required');
      return this;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      this.addError(field, 'INVALID_FORMAT', 'Invalid email format');
    }

    if (value.length > 254) {
      this.addError(field, 'TOO_LONG', 'Email must be less than 254 characters');
    }

    return this;
  }

  // Password validation
  password(value: string, field: string = 'password'): this {
    if (!value) {
      this.addError(field, 'REQUIRED', 'Password is required');
      return this;
    }

    if (value.length < 8) {
      this.addError(field, 'TOO_SHORT', 'Password must be at least 8 characters');
    }

    if (value.length > 128) {
      this.addError(field, 'TOO_LONG', 'Password must be less than 128 characters');
    }

    // Check for at least one uppercase letter
    if (!/[A-Z]/.test(value)) {
      this.addError(field, 'MISSING_UPPERCASE', 'Password must contain at least one uppercase letter');
    }

    // Check for at least one lowercase letter
    if (!/[a-z]/.test(value)) {
      this.addError(field, 'MISSING_LOWERCASE', 'Password must contain at least one lowercase letter');
    }

    // Check for at least one number
    if (!/\d/.test(value)) {
      this.addError(field, 'MISSING_NUMBER', 'Password must contain at least one number');
    }

    // Check for at least one special character
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(value)) {
      this.addError(field, 'MISSING_SPECIAL', 'Password must contain at least one special character');
    }

    return this;
  }

  // Username/name validation
  username(value: string, field: string = 'username'): this {
    if (!value) {
      this.addError(field, 'REQUIRED', 'Username is required');
      return this;
    }

    if (value.length < 2) {
      this.addError(field, 'TOO_SHORT', 'Username must be at least 2 characters');
    }

    if (value.length > 50) {
      this.addError(field, 'TOO_LONG', 'Username must be less than 50 characters');
    }

    // Allow letters, numbers, underscores, and hyphens
    const usernameRegex = /^[a-zA-Z0-9_-]+$/;
    if (!usernameRegex.test(value)) {
      this.addError(field, 'INVALID_FORMAT', 'Username can only contain letters, numbers, underscores, and hyphens');
    }

    return this;
  }

  // Required field validation
  required(value: any, field: string): this {
    if (value === null || value === undefined || value === '') {
      this.addError(field, 'REQUIRED', `${field} is required`);
    }
    return this;
  }

  // String length validation
  stringLength(value: string, min: number, max: number, field: string): this {
    if (!value) {
      return this;
    }

    if (value.length < min) {
      this.addError(field, 'TOO_SHORT', `${field} must be at least ${min} characters`);
    }

    if (value.length > max) {
      this.addError(field, 'TOO_LONG', `${field} must be less than ${max} characters`);
    }

    return this;
  }

  // Number range validation
  numberRange(value: number, min: number, max: number, field: string): this {
    if (value < min) {
      this.addError(field, 'TOO_SMALL', `${field} must be at least ${min}`);
    }

    if (value > max) {
      this.addError(field, 'TOO_LARGE', `${field} must be at most ${max}`);
    }

    return this;
  }

  // Custom validation
  custom(predicate: boolean, field: string, code: string, message: string): this {
    if (!predicate) {
      this.addError(field, code, message);
    }
    return this;
  }

  // UUID validation
  uuid(value: string, field: string = 'id'): this {
    if (!value) {
      this.addError(field, 'REQUIRED', `${field} is required`);
      return this;
    }

    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    if (!uuidRegex.test(value)) {
      this.addError(field, 'INVALID_FORMAT', `${field} must be a valid UUID`);
    }

    return this;
  }

  // Date validation
  date(value: string | Date, field: string = 'date'): this {
    if (!value) {
      this.addError(field, 'REQUIRED', `${field} is required`);
      return this;
    }

    const date = typeof value === 'string' ? new Date(value) : value;
    if (isNaN(date.getTime())) {
      this.addError(field, 'INVALID_FORMAT', `${field} must be a valid date`);
    }

    return this;
  }

  // Array validation
  array(value: any[], min: number, max: number, field: string): this {
    if (!Array.isArray(value)) {
      this.addError(field, 'INVALID_TYPE', `${field} must be an array`);
      return this;
    }

    if (value.length < min) {
      this.addError(field, 'TOO_FEW', `${field} must contain at least ${min} items`);
    }

    if (value.length > max) {
      this.addError(field, 'TOO_MANY', `${field} must contain at most ${max} items`);
    }

    return this;
  }

  private addError(field: string, code: string, message: string, value?: any): void {
    this.errors.push({
      field,
      code,
      message,
      value,
    });
  }

  getResult(): ValidationResult {
    return {
      isValid: this.errors.length === 0,
      errors: [...this.errors],
    };
  }

  throwIfInvalid(): void {
    const result = this.getResult();
    if (!result.isValid) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Validation failed',
        { errors: result.errors }
      );
    }
  }

  reset(): this {
    this.errors = [];
    return this;
  }
}

// Utility functions for common validations
export const validateEmail = (email: string): ValidationResult => {
  return Validator.create().email(email).getResult();
};

export const validatePassword = (password: string): ValidationResult => {
  return Validator.create().password(password).getResult();
};

export const validateUsername = (username: string): ValidationResult => {
  return Validator.create().username(username).getResult();
};