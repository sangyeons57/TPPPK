import {onObjectFinalized} from "firebase-functions/v2/storage";
import {UpdateUserImageUseCase} from "../../business/user/usecases/updateUserImage.usecase";
import {RUNTIME_CONFIG} from "../../core/constants";
import {STORAGE_BUCKETS} from "../../core/constants";
import {Providers} from "../../config/dependencies";

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

      // Import admin SDK for Storage operations
      const admin = require("firebase-admin");
      const storage = admin.storage();

      try {
        // Get the original file
        const originalFile = storage.bucket(bucket).file(name);
        
        // Create processed file path in user_profiles directory
        const filename = pathParts[pathParts.length - 1]; // Get the filename
        const processedFilePath = `user_profiles/${userId}/${filename}`;
        const processedFile = storage.bucket(bucket).file(processedFilePath);

        // Copy the original file to the processed location
        await originalFile.copy(processedFile);
        console.log(`Copied ${name} to ${processedFilePath}`);

        // Generate public URL for the processed file
        const processedPublicUrl = `https://storage.googleapis.com/${bucket}/${processedFilePath}`;

        // Get use case and update user with processed image URL
        const userUseCases = Providers.getUserProvider().create();
        const updateImageUseCase = new UpdateUserImageUseCase(userUseCases.userRepository);

        const result = await updateImageUseCase.execute({
          userId: userId,
          imageUrl: processedPublicUrl,
        });

        if (result.success) {
          console.log(`Successfully updated user ${userId} profile image: ${processedPublicUrl}`);
        } else {
          console.error(`Failed to update user ${userId} profile image:`, result.error);
        }
      } catch (copyError) {
        console.error(`Error copying file from ${name} to user_profiles:`, copyError);
      }
    } catch (error) {
      console.error("Error processing user profile image upload:", error);
    }
  }
);
