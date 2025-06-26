/**
 * Authentication token value object
 * Provides type safety and validation for JWT tokens
 */

import { ValueObject } from '../../../shared/types/common';
import { FunctionError, FunctionErrorCode } from '../../../shared/types/common';

export class Token implements ValueObject {
  private static readonly MIN_LENGTH = 10;
  private static readonly MAX_LENGTH = 2048;
  private static readonly EMPTY_TOKEN = '';

  public static readonly EMPTY = new Token(Token.EMPTY_TOKEN);

  private constructor(private readonly tokenValue: string) {
    this.validate();
  }

  public get value(): string {
    return this.tokenValue;
  }

  public static from(token: string): Token {
    return new Token(token);
  }

  public equals(other: ValueObject): boolean {
    return other instanceof Token && this.value === other.value;
  }

  public isEmpty(): boolean {
    return this.tokenValue === Token.EMPTY_TOKEN;
  }

  public isNotEmpty(): boolean {
    return !this.isEmpty();
  }

  public isExpired(currentTime: Date = new Date()): boolean {
    try {
      const payload = this.getPayload();
      if (!payload.exp) {
        return false; // No expiration claim
      }
      return currentTime.getTime() / 1000 >= payload.exp;
    } catch {
      return true; // Invalid token is considered expired
    }
  }

  public getPayload(): any {
    if (this.isEmpty()) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Cannot decode empty token',
        { field: 'token', code: 'EMPTY' }
      );
    }

    try {
      const parts = this.tokenValue.split('.');
      if (parts.length !== 3) {
        throw new Error('Invalid JWT format');
      }

      const payload = parts[1];
      const decoded = Buffer.from(payload, 'base64url').toString('utf8');
      return JSON.parse(decoded);
    } catch (error) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Invalid token format',
        { field: 'token', code: 'INVALID_FORMAT', cause: error }
      );
    }
  }

  public getHeader(): any {
    if (this.isEmpty()) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Cannot decode empty token',
        { field: 'token', code: 'EMPTY' }
      );
    }

    try {
      const parts = this.tokenValue.split('.');
      if (parts.length !== 3) {
        throw new Error('Invalid JWT format');
      }

      const header = parts[0];
      const decoded = Buffer.from(header, 'base64url').toString('utf8');
      return JSON.parse(decoded);
    } catch (error) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Invalid token format',
        { field: 'token', code: 'INVALID_FORMAT', cause: error }
      );
    }
  }

  public getUserId(): string {
    const payload = this.getPayload();
    return payload.sub || payload.uid || payload.user_id || '';
  }

  public getEmail(): string {
    const payload = this.getPayload();
    return payload.email || '';
  }

  public getRoles(): string[] {
    const payload = this.getPayload();
    return payload.roles || payload.permissions || [];
  }

  public toString(): string {
    return this.value;
  }

  public toJSON(): string {
    return this.value;
  }

  private validate(): void {
    // Allow empty string for EMPTY constant
    if (this.tokenValue === Token.EMPTY_TOKEN) {
      return;
    }

    if (!this.tokenValue || this.tokenValue.trim() === '') {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Token cannot be blank',
        { field: 'token', code: 'BLANK' }
      );
    }

    if (this.tokenValue.length < Token.MIN_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `Token must be at least ${Token.MIN_LENGTH} characters`,
        { 
          field: 'token', 
          code: 'TOO_SHORT',
          minLength: Token.MIN_LENGTH,
          actualLength: this.tokenValue.length
        }
      );
    }

    if (this.tokenValue.length > Token.MAX_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `Token cannot exceed ${Token.MAX_LENGTH} characters`,
        { 
          field: 'token', 
          code: 'TOO_LONG',
          maxLength: Token.MAX_LENGTH,
          actualLength: this.tokenValue.length
        }
      );
    }

    // Basic JWT format validation (3 parts separated by dots)
    const parts = this.tokenValue.split('.');
    if (parts.length !== 3) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Token must be a valid JWT format',
        { field: 'token', code: 'INVALID_FORMAT', value: this.tokenValue }
      );
    }

    // Validate base64url encoding of parts
    parts.forEach((part, index) => {
      if (!part || !/^[A-Za-z0-9_-]+$/.test(part)) {
        throw new FunctionError(
          FunctionErrorCode.VALIDATION_ERROR,
          `Token part ${index + 1} is not valid base64url`,
          { field: 'token', code: 'INVALID_ENCODING', part: index + 1 }
        );
      }
    });
  }
}