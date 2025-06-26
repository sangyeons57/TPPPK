/**
 * Project identifier value object
 * Provides type safety and validation for project IDs
 */

import { ValueObject } from '../../../shared/types/common';
import { FunctionError, FunctionErrorCode } from '../../../shared/types/common';

export class ProjectId implements ValueObject {
  private static readonly MAX_LENGTH = 128;
  private static readonly EMPTY_ID = '';

  public static readonly EMPTY = new ProjectId(ProjectId.EMPTY_ID);

  private constructor(private readonly internalValue: string) {
    this.validate();
  }

  public get value(): string {
    return this.isEmpty() ? '' : this.internalValue;
  }

  public static from(value: string): ProjectId {
    return new ProjectId(value);
  }

  public static fromDocumentId(documentId: string): ProjectId {
    return new ProjectId(documentId);
  }

  public equals(other: ValueObject): boolean {
    return other instanceof ProjectId && this.value === other.value;
  }

  public isEmpty(): boolean {
    return this.internalValue === ProjectId.EMPTY_ID;
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

  public toString(): string {
    return this.value;
  }

  public toJSON(): string {
    return this.value;
  }

  private validate(): void {
    if (this.internalValue.length > ProjectId.MAX_LENGTH) {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        `Project ID cannot exceed ${ProjectId.MAX_LENGTH} characters`,
        { maxLength: ProjectId.MAX_LENGTH, actualLength: this.internalValue.length }
      );
    }

    // Allow empty string for EMPTY constant
    if (this.internalValue !== ProjectId.EMPTY_ID && this.internalValue.trim() === '') {
      throw new FunctionError(
        FunctionErrorCode.VALIDATION_ERROR,
        'Project ID cannot be blank',
        { value: this.internalValue }
      );
    }
  }
}