import {onObjectFinalized} from "firebase-functions/v2/storage";
import {UpdateUserImageUseCase} from "../../business/user/usecases/updateUserImage.usecase";
import {RUNTIME_CONFIG} from "../../core/constants";
import {STORAGE_BUCKETS} from "../../core/constants";
import {Providers} from "../../config/dependencies";
import * as admin from "firebase-admin";

/**
 * Simplified Storage trigger for user profile images
 * Updates User entity's profileImageUrl when image is uploaded
 */
export const onUserProfileImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
    bucket: STORAGE_BUCKETS.USER_PROFILES,
  },
  async (event) => {
    try {
      const {bucket, name, contentType} = event.data;

      if (!name || !contentType) {
        console.log("Missing file name or content type");
        return;
      }

      // Only process image files
      if (!contentType.startsWith("image/")) {
        console.log(`Skipping non-image file: ${contentType}`);
        return;
      }

      // Extract userId from file path (e.g., "user_profile_images/{userId}/filename.jpg")
      const pathParts = name.split("/");
      if (pathParts.length < 2 || pathParts[0] !== "user_profile_images") {
        console.log(`Invalid file path structure: ${name}`);
        return;
      }

      const userId = pathParts[1];
      if (!userId) {
        console.log("Could not extract userId from file path");
        return;
      }

      // Get Firebase Storage instance
      const storage = admin.storage();

      try {
        // Get the original file
        const originalFile = storage.bucket(bucket).file(name);

        // Create fixed file path in user_profiles directory (always use profile.webp)
        const processedFilePath = `user_profiles/${userId}/profile.webp`;
        const processedFile = storage.bucket(bucket).file(processedFilePath);

        // Check if the processed file already exists and delete it first
        try {
          const [exists] = await processedFile.exists();
          if (exists) {
            await processedFile.delete();
            console.log(`Deleted existing profile image: ${processedFilePath}`);
          }
        } catch (deleteError) {
          console.log(`No existing file to delete or delete failed: ${(deleteError as Error).message}`);
        }

        // Copy the original file to the processed location (with fixed filename)
        await originalFile.copy(processedFile);
        console.log(`Copied ${name} to ${processedFilePath}`);

        // Generate public URL with timestamp for cache invalidation
        const timestamp = Date.now();
        const processedPublicUrl = `https://storage.googleapis.com/${bucket}/${processedFilePath}?v=${timestamp}`;

        // Get use case and update user with timestamped image URL
        const userUseCases = Providers.getUserProvider().create();
        const updateImageUseCase = new UpdateUserImageUseCase(userUseCases.userRepository);

        console.log(`Executing updateImageUseCase for user ${userId} with URL: ${processedPublicUrl}`);

        const result = await updateImageUseCase.execute({
          userId: userId,
          imageUrl: processedPublicUrl,
        });

        console.log("UpdateImageUseCase result:", JSON.stringify(result, null, 2));

        if (result.success) {
          console.log("✅ Successfully updated user profile image:", processedPublicUrl);
          console.log("✅ Updated user data:", JSON.stringify(result.data, null, 2));

          // Clean up the original file in user_profile_images after successful processing
          try {
            await originalFile.delete();
            console.log("🗑️ Cleaned up original file:", name);
          } catch (cleanupError) {
            console.log("⚠️ Failed to cleanup original file:", (cleanupError as Error).message);
          }
        } else {
          console.error("❌ Failed to update user profile image:", result.error);
          console.error("❌ Error details:", JSON.stringify(result.error, null, 2));
        }
      } catch (copyError) {
        console.error(`Error processing file from ${name} to user_profiles:`, copyError);
      }
    } catch (error) {
      console.error("Error processing user profile image upload:", error);
    }
  }
);
