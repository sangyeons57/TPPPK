/**
 * User email value object
 * Provides type safety and validation for user email addresses
 */

import { ValueObject } from '../../../shared/types/common';
import { FunctionError, FunctionErrorCode } from '../../../shared/types/common';
import { VALIDATION } from '../../../shared/constants';

export class UserEmail implements ValueObject {
  private static readonly EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  private constructor(private readonly emailValue: string) {
    this.validate();
  }

  public get value(): string {
    return this.emailValue;
  }

  public static from(email: string): UserEmail {
    return new UserEmail(email);
  }

  public equals(other: ValueObject): boolean {
    return other instanceof UserEmail && this.value.toLowerCase() === other.value.toLowerCase();
  }

  public getDomain(): string {
    return this.emailValue.split('@')[1];
  }

  public getLocalPart(): string {
    return this.emailValue.split('@')[0];
  }

  public toString(): string {
    return this.value;
  }

  public toJSON(): string {
    return this.value;
  }

  private validate(): void {
    if (!this.emailValue) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Email is required',
        { field: 'email', code: 'REQUIRED' }
      );
    }

    if (this.emailValue.length > VALIDATION.EMAIL_MAX_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `Email cannot exceed ${VALIDATION.EMAIL_MAX_LENGTH} characters`,
        { 
          field: 'email', 
          code: 'TOO_LONG',
          maxLength: VALIDATION.EMAIL_MAX_LENGTH,
          actualLength: this.emailValue.length
        }
      );
    }

    if (!UserEmail.EMAIL_REGEX.test(this.emailValue)) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Invalid email format',
        { field: 'email', code: 'INVALID_FORMAT', value: this.emailValue }
      );
    }

    // Check for common email format issues
    if (this.emailValue.includes('..')) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Email cannot contain consecutive dots',
        { field: 'email', code: 'INVALID_FORMAT', value: this.emailValue }
      );
    }

    if (this.emailValue.startsWith('.') || this.emailValue.endsWith('.')) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Email cannot start or end with a dot',
        { field: 'email', code: 'INVALID_FORMAT', value: this.emailValue }
      );
    }
  }
}