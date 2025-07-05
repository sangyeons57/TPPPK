import {ValidationError} from "../errors";

export interface ValueObject<T> {
  readonly value: T;
  equals(other: ValueObject<T>): boolean;
}

export class ImageUrl implements ValueObject<string> {
  constructor(public readonly value: string) {
    if (!value.startsWith("https://")) {
      throw new ValidationError("imageUrl", "Image URL must be a valid HTTPS URL");
    }
  }

  equals(other: ImageUrl): boolean {
    return this.value === other.value;
  }
}

export class ImageSize implements ValueObject<number> {
  constructor(public readonly value: number) {
    if (value <= 0) {
      throw new ValidationError("imageSize", "Image size must be positive");
    }
    // 최대 크기 검증 (10MB)
    if (value > 10 * 1024 * 1024) {
      throw new ValidationError("imageSize", "Image size must be less than 10MB");
    }
  }

  equals(other: ImageSize): boolean {
    return this.value === other.value;
  }
}

export type SupportedImageFormat = "jpg" | "jpeg" | "png" | "webp";

export class ImageFormat implements ValueObject<string> {
  private static readonly SUPPORTED_FORMATS: SupportedImageFormat[] = ["jpg", "jpeg", "png", "webp"];

  constructor(public readonly value: string) {
    if (!ImageFormat.SUPPORTED_FORMATS.includes(value.toLowerCase() as SupportedImageFormat)) {
      throw new ValidationError("imageFormat", `Image format must be one of: ${ImageFormat.SUPPORTED_FORMATS.join(", ")}`);
    }
  }

  equals(other: ImageFormat): boolean {
    return this.value.toLowerCase() === other.value.toLowerCase();
  }
}

export enum ImageType {
  USER_PROFILE = "user_profile",
  PROJECT_IMAGE = "project_image",
}

export interface ImageMetadata {
  width: number;
  height: number;
  format: string;
  size: number;
}
 