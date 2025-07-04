import { ValidationError } from "../errors";
import { IMAGE_PROCESSING } from "../constants";

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
    if (value > IMAGE_PROCESSING.MAX_SIZE) {
      throw new ValidationError("imageSize", `Image size must be less than ${IMAGE_PROCESSING.MAX_SIZE} bytes`);
    }
  }

  equals(other: ImageSize): boolean {
    return this.value === other.value;
  }
}

export type SupportedImageFormat = "jpg" | "jpeg" | "png" | "webp";

export class ImageFormat implements ValueObject<string> {
  constructor(public readonly value: string) {
    const supportedFormats = IMAGE_PROCESSING.SUPPORTED_FORMATS as readonly string[];
    if (!supportedFormats.includes(value.toLowerCase())) {
      throw new ValidationError("imageFormat", `Image format must be one of: ${supportedFormats.join(", ")}`);
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
 