/**
 * User identifier value object
 * Provides type safety and validation for user IDs
 */

import { ValueObject } from '../../../shared/types/common';
import { FunctionError, FunctionErrorCode } from '../../../shared/types/common';

export class UserId implements ValueObject {
  private static readonly MAX_LENGTH = 128;
  private static readonly UNKNOWN_USER_ID = 'UNKNOWN_USER';
  private static readonly EMPTY_ID = '';

  public static readonly UNKNOWN_USER = new UserId(UserId.UNKNOWN_USER_ID);
  public static readonly EMPTY = new UserId(UserId.EMPTY_ID);

  private constructor(private readonly internalValue: string) {
    this.validate();
  }

  public get value(): string {
    return this.isEmpty() ? '' : this.internalValue;
  }

  public static from(value: string): UserId {
    return new UserId(value);
  }

  public static fromDocumentId(documentId: string): UserId {
    return new UserId(documentId);
  }

  public equals(other: ValueObject): boolean {
    return other instanceof UserId && this.value === other.value;
  }

  public isEmpty(): boolean {
    return this.internalValue === UserId.EMPTY_ID;
  }

  public isNotEmpty(): boolean {
    return !this.isEmpty();
  }

  public isBlank(): boolean {
    return this.value.trim() === '';
  }

  public isNotBlank(): boolean {
    return !this.isBlank();
  }

  public isUnknown(): boolean {
    return this.internalValue === UserId.UNKNOWN_USER_ID;
  }

  public toString(): string {
    return this.value;
  }

  public toJSON(): string {
    return this.value;
  }

  private validate(): void {
    if (this.internalValue.length > UserId.MAX_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `User ID cannot exceed ${UserId.MAX_LENGTH} characters`,
        { maxLength: UserId.MAX_LENGTH, actualLength: this.internalValue.length }
      );
    }

    // Allow empty string for EMPTY constant
    if (this.internalValue !== UserId.EMPTY_ID && this.internalValue.trim() === '') {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'User ID cannot be blank',
        { value: this.internalValue }
      );
    }
  }
}