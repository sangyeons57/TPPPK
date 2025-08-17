import * as admin from "firebase-admin";
import { createLogger } from "./logger";

export class IdempotencyManager {
  private static readonly COLLECTION = "idempotency_keys";
  private static readonly DEFAULT_TTL_MINUTES = 60;

  static async checkAndMark(
    key: string,
    operation: string,
    ttlMinutes: number = this.DEFAULT_TTL_MINUTES
  ): Promise<boolean> {
    const logger = createLogger({ 
      domain: "idempotency", 
      operation: "checkAndMark" 
    });

    try {
      const docRef = admin
        .firestore()
        .collection(this.COLLECTION)
        .doc(key);

      const doc = await docRef.get();

      if (doc.exists) {
        const data = doc.data();
        if (data?.operation === operation && data?.status === "completed") {
          logger.info(`Operation already completed for key: ${key}`);
          return false; // 이미 처리됨
        }

        if (data?.operation === operation && data?.status === "processing") {
          const createdAt = data.createdAt?.toDate();
          if (createdAt && Date.now() - createdAt.getTime() < ttlMinutes * 60 * 1000) {
            logger.warn(`Operation still processing for key: ${key}`);
            return false; // 아직 처리 중
          }
        }
      }

      // 처리 중 마커 생성
      await docRef.set({
        operation,
        status: "processing",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: new Date(Date.now() + ttlMinutes * 60 * 1000),
      });

      logger.info(`Marked operation as processing for key: ${key}`);
      return true; // 처리 진행
    } catch (error) {
      logger.error(`Failed to check/mark idempotency key: ${key}`, error);
      throw error;
    }
  }

  static async markCompleted(key: string): Promise<void> {
    const logger = createLogger({ 
      domain: "idempotency", 
      operation: "markCompleted" 
    });

    try {
      await admin
        .firestore()
        .collection(this.COLLECTION)
        .doc(key)
        .update({
          status: "completed",
          completedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

      logger.info(`Marked operation as completed for key: ${key}`);
    } catch (error) {
      logger.error(`Failed to mark completed for key: ${key}`, error);
      throw error;
    }
  }

  static async markFailed(key: string, error: unknown): Promise<void> {
    const logger = createLogger({ 
      domain: "idempotency", 
      operation: "markFailed" 
    });

    try {
      await admin
        .firestore()
        .collection(this.COLLECTION)
        .doc(key)
        .update({
          status: "failed",
          failedAt: admin.firestore.FieldValue.serverTimestamp(),
          error: error instanceof Error ? error.message : String(error),
        });

      logger.info(`Marked operation as failed for key: ${key}`);
    } catch (updateError) {
      logger.error(`Failed to mark failed for key: ${key}`, updateError);
    }
  }

  static generateKey(userId: string, operation: string, ...params: string[]): string {
    return `${userId}:${operation}:${params.join(":")}`;
  }

  static async cleanupExpired(): Promise<void> {
    const logger = createLogger({ 
      domain: "idempotency", 
      operation: "cleanup" 
    });

    try {
      const now = new Date();
      const expiredDocs = await admin
        .firestore()
        .collection(this.COLLECTION)
        .where("expiresAt", "<", now)
        .limit(100)
        .get();

      if (expiredDocs.empty) {
        logger.info("No expired idempotency keys to clean up");
        return;
      }

      const batch = admin.firestore().batch();
      expiredDocs.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      logger.info(`Cleaned up ${expiredDocs.size} expired idempotency keys`);
    } catch (error) {
      logger.error("Failed to cleanup expired idempotency keys", error);
      throw error;
    }
  }
}