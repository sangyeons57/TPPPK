import {ImageRepository, ImageStorageService, ProcessedImageEntity} from "../../../core/services/imageProcessing.service";
import {ImageType, ImageUrl, ImageSize, ImageFormat, ImageMetadata} from "../../../core/types/image.types";
import {CustomResult, Result} from "../../../core/types";
import {InternalError} from "../../../core/errors";
import {IMAGE_PROCESSING, FIRESTORE_COLLECTIONS} from "../../../core/constants";
import {getFirestore, FieldValue} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";

interface ImageData {
  id: string;
  originalUrl: string;
  thumbnailUrl?: string;
  size: number;
  format: string;
  type: ImageType;
  ownerId: string;
  width?: number;
  height?: number;
  createdAt: FirebaseFirestore.Timestamp;
  updatedAt: FirebaseFirestore.Timestamp;
}

export class FirestoreImageDataSource implements ImageRepository {
  private readonly db = getFirestore();
  private readonly collection = this.db.collection(FIRESTORE_COLLECTIONS.IMAGES);

  async findById(id: string): Promise<CustomResult<ProcessedImageEntity | null>> {
    try {
      const doc = await this.collection.doc(id).get();
      if (!doc.exists) {
        return Result.success(null);
      }

      const data = doc.data() as ImageData;
      return Result.success(this.mapToEntity(data));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find image by id: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async findByOwnerId(ownerId: string): Promise<CustomResult<ProcessedImageEntity[]>> {
    try {
      const query = await this.collection
        .where("ownerId", "==", ownerId)
        .orderBy("createdAt", "desc")
        .get();

      const images = query.docs.map((doc) => this.mapToEntity(doc.data() as ImageData));
      return Result.success(images);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find images by owner: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async findByType(type: ImageType): Promise<CustomResult<ProcessedImageEntity[]>> {
    try {
      const query = await this.collection
        .where("type", "==", type)
        .orderBy("createdAt", "desc")
        .get();

      const images = query.docs.map((doc) => this.mapToEntity(doc.data() as ImageData));
      return Result.success(images);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find images by type: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async findByOwnerIdAndType(ownerId: string, type: ImageType): Promise<CustomResult<ProcessedImageEntity[]>> {
    try {
      const query = await this.collection
        .where("ownerId", "==", ownerId)
        .where("type", "==", type)
        .orderBy("createdAt", "desc")
        .get();

      const images = query.docs.map((doc) => this.mapToEntity(doc.data() as ImageData));
      return Result.success(images);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to find images by owner and type: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async save(image: ProcessedImageEntity): Promise<CustomResult<ProcessedImageEntity>> {
    try {
      const data = this.mapToData(image);
      await this.collection.doc(image.id).set(data);
      return Result.success(image);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to save image: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async update(image: ProcessedImageEntity): Promise<CustomResult<ProcessedImageEntity>> {
    try {
      const data = this.mapToData(image);
      data.updatedAt = FieldValue.serverTimestamp() as any;
      await this.collection.doc(image.id).update(data as any);
      return Result.success(image);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to update image: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.collection.doc(id).delete();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete image: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async exists(id: string): Promise<CustomResult<boolean>> {
    try {
      const doc = await this.collection.doc(id).get();
      return Result.success(doc.exists);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to check image existence: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async deleteByUrl(url: ImageUrl): Promise<CustomResult<void>> {
    try {
      const query = await this.collection.where("originalUrl", "==", url.value).get();
      const batch = this.db.batch();

      query.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to delete image by URL: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  private mapToEntity(data: ImageData): ProcessedImageEntity {
    return new ProcessedImageEntity(
      data.id,
      new ImageUrl(data.originalUrl),
      new ImageSize(data.size),
      new ImageFormat(data.format),
      data.type,
      data.ownerId,
      data.createdAt.toDate(),
      data.updatedAt.toDate(),
      data.thumbnailUrl ? new ImageUrl(data.thumbnailUrl) : undefined,
      data.width,
      data.height
    );
  }

  private mapToData(entity: ProcessedImageEntity): ImageData {
    return {
      id: entity.id,
      originalUrl: entity.originalUrl.value,
      thumbnailUrl: entity.thumbnailUrl?.value,
      size: entity.size.value,
      format: entity.format.value,
      type: entity.type,
      ownerId: entity.ownerId,
      width: entity.width,
      height: entity.height,
      createdAt: entity.createdAt as any,
      updatedAt: entity.updatedAt as any,
    };
  }
}

export class FirebaseStorageService implements ImageStorageService {
  private readonly storage = getStorage();

  async uploadImage(file: Buffer, path: string, contentType: string): Promise<CustomResult<ImageUrl>> {
    try {
      const bucket = this.storage.bucket();
      const fileRef = bucket.file(path);

      await fileRef.save(file, {
        metadata: {
          contentType,
          cacheControl: "public, max-age=31536000",
        },
      });

      await fileRef.makePublic();
      const publicUrl = `https://storage.googleapis.com/${bucket.name}/${path}`;

      return Result.success(new ImageUrl(publicUrl));
    } catch (error) {
      return Result.failure(new InternalError(`Failed to upload image: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async deleteImage(url: ImageUrl): Promise<CustomResult<void>> {
    try {
      const bucket = this.storage.bucket();
      const fileName = this.extractFileNameFromUrl(url.value);

      if (fileName) {
        const file = bucket.file(fileName);
        await file.delete();
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new InternalError(`Failed to delete image: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  async generateThumbnail(imageBuffer: Buffer, width: number, height: number): Promise<CustomResult<Buffer>> {
    try {
      const sharp = await import("sharp");

      const thumbnailBuffer = await sharp.default(imageBuffer)
        .resize(width, height, {
          fit: "cover",
          position: "center",
        })
        .jpeg({quality: IMAGE_PROCESSING.QUALITY})
        .toBuffer();

      return Result.success(thumbnailBuffer);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to generate thumbnail: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async validateImageFormat(contentType: string): Promise<CustomResult<boolean>> {
    try {
      const format = contentType.split("/")[1];
      const isValid = IMAGE_PROCESSING.SUPPORTED_FORMATS.includes(format as "jpg" | "jpeg" | "png" | "webp");
      return Result.success(isValid);
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to validate image format: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  async getImageMetadata(imageBuffer: Buffer): Promise<CustomResult<ImageMetadata>> {
    try {
      const sharp = await import("sharp");
      const metadata = await sharp.default(imageBuffer).metadata();

      return Result.success({
        width: metadata.width || 0,
        height: metadata.height || 0,
        format: metadata.format || "unknown",
        size: imageBuffer.length,
      });
    } catch (error) {
      return Result.failure(
        new InternalError(`Failed to get image metadata: ${error instanceof Error ? error.message : "Unknown error"}`)
      );
    }
  }

  private extractFileNameFromUrl(url: string): string | null {
    try {
      const urlParts = url.split("/");
      return urlParts[urlParts.length - 1];
    } catch {
      return null;
    }
  }
}
