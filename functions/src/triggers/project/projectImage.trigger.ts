import {onObjectFinalized} from "firebase-functions/v2/storage";
import {UpdateProjectImageUseCase} from "../../business/project/usecases/updateProjectImage.usecase";
import {RUNTIME_CONFIG} from "../../core/constants";
import {STORAGE_BUCKETS} from "../../core/constants";
import {Providers} from "../../config/dependencies";

/**
 * Simplified Storage trigger for project images
 * Updates Project entity's imageUrl when image is uploaded
 */
export const onProjectImageUpload = onObjectFinalized(
  {
    region: RUNTIME_CONFIG.REGION,
    memory: RUNTIME_CONFIG.MEMORY,
    timeoutSeconds: RUNTIME_CONFIG.TIMEOUT_SECONDS,
    bucket: STORAGE_BUCKETS.PROJECT_IMAGES,
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

      // Extract projectId from file path (e.g., "project_profile_images/{projectId}/filename.jpg")
      const pathParts = name.split("/");
      if (pathParts.length < 2 || pathParts[0] !== "project_profile_images") {
        console.log(`Invalid file path structure: ${name}`);
        return;
      }

      const projectId = pathParts[1];
      if (!projectId) {
        console.log("Could not extract projectId from file path");
        return;
      }

      // Import admin SDK for Storage operations
      const admin = require("firebase-admin");
      const storage = admin.storage();

      try {
        // Get the original file
        const originalFile = storage.bucket(bucket).file(name);

        // Create processed file path in project_profiles directory
        const filename = pathParts[pathParts.length - 1]; // Get the filename
        const processedFilePath = `project_profiles/${projectId}/${filename}`;
        const processedFile = storage.bucket(bucket).file(processedFilePath);

        // Copy the original file to the processed location
        await originalFile.copy(processedFile);
        console.log(`Copied ${name} to ${processedFilePath}`);

        // Generate public URL for the processed file
        const processedPublicUrl = `https://storage.googleapis.com/${bucket}/${processedFilePath}`;

        // Get use case and update project with processed image URL
        const projectUseCases = Providers.getProjectProvider().create();
        const updateImageUseCase = new UpdateProjectImageUseCase(projectUseCases.projectRepository);

        const result = await updateImageUseCase.execute({
          projectId: projectId,
          imageUrl: processedPublicUrl,
        });

        if (result.success) {
          console.log(`Successfully updated project ${projectId} image: ${processedPublicUrl}`);
        } else {
          console.error(`Failed to update project ${projectId} image:`, result.error);
        }
      } catch (copyError) {
        console.error(`Error copying file from ${name} to project_profiles:`, copyError);
      }
    } catch (error) {
      console.error("Error processing project image upload:", error);
    }
  }
);
