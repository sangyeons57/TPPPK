/**
 * User name value object
 * Provides type safety and validation for user display names
 */

import { ValueObject } from '../../../shared/types/common';
import { FunctionError, FunctionErrorCode } from '../../../shared/types/common';
import { VALIDATION } from '../../../shared/constants';

export class UserName implements ValueObject {
  private static readonly NAME_REGEX = /^[a-zA-Z0-9가-힣\s_-]+$/;
  private static readonly FORBIDDEN_WORDS = [
    'admin', 'administrator', 'root', 'system', 'null', 'undefined',
    'test', 'guest', 'anonymous', 'user', 'default'
  ];

  private constructor(private readonly nameValue: string) {
    this.validate();
  }

  public get value(): string {
    return this.nameValue;
  }

  public static from(name: string): UserName {
    return new UserName(name.trim());
  }

  public equals(other: ValueObject): boolean {
    return other instanceof UserName && this.value.toLowerCase() === other.value.toLowerCase();
  }

  public getInitials(): string {
    const words = this.nameValue.trim().split(/\s+/);
    if (words.length === 1) {
      return words[0].substring(0, 2).toUpperCase();
    }
    return words.slice(0, 2).map(word => word.charAt(0).toUpperCase()).join('');
  }

  public getFirstName(): string {
    return this.nameValue.trim().split(/\s+/)[0];
  }

  public getLastName(): string {
    const words = this.nameValue.trim().split(/\s+/);
    return words.length > 1 ? words[words.length - 1] : '';
  }

  public toString(): string {
    return this.value;
  }

  public toJSON(): string {
    return this.value;
  }

  private validate(): void {
    if (!this.nameValue || this.nameValue.trim() === '') {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Name is required',
        { field: 'name', code: 'REQUIRED' }
      );
    }

    const trimmedName = this.nameValue.trim();

    if (trimmedName.length < VALIDATION.USERNAME_MIN_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `Name must be at least ${VALIDATION.USERNAME_MIN_LENGTH} characters`,
        { 
          field: 'name', 
          code: 'TOO_SHORT',
          minLength: VALIDATION.USERNAME_MIN_LENGTH,
          actualLength: trimmedName.length
        }
      );
    }

    if (trimmedName.length > VALIDATION.USERNAME_MAX_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `Name cannot exceed ${VALIDATION.USERNAME_MAX_LENGTH} characters`,
        { 
          field: 'name', 
          code: 'TOO_LONG',
          maxLength: VALIDATION.USERNAME_MAX_LENGTH,
          actualLength: trimmedName.length
        }
      );
    }

    if (!UserName.NAME_REGEX.test(trimmedName)) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Name can only contain letters, numbers, Korean characters, spaces, underscores, and hyphens',
        { field: 'name', code: 'INVALID_FORMAT', value: trimmedName }
      );
    }

    // Check for forbidden words
    const lowerName = trimmedName.toLowerCase();
    if (UserName.FORBIDDEN_WORDS.some(word => lowerName.includes(word))) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Name contains forbidden words',
        { field: 'name', code: 'FORBIDDEN_WORD', value: trimmedName }
      );
    }

    // Check for excessive spaces
    if (trimmedName.includes('  ')) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Name cannot contain multiple consecutive spaces',
        { field: 'name', code: 'INVALID_FORMAT', value: trimmedName }
      );
    }

    // Check for leading/trailing special characters
    if (/^[_-]|[_-]$/.test(trimmedName)) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Name cannot start or end with underscore or hyphen',
        { field: 'name', code: 'INVALID_FORMAT', value: trimmedName }
      );
    }
  }
}