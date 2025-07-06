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

      // Extract projectId from file path (e.g., "projects/{projectId}/image.jpg")
      const pathParts = name.split("/");
      if (pathParts.length < 2 || pathParts[0] !== "projects") {
        console.log(`Invalid file path structure: ${name}`);
        return;
      }

      const projectId = pathParts[1];
      if (!projectId) {
        console.log("Could not extract projectId from file path");
        return;
      }

      // Generate public URL
      const publicUrl = `https://storage.googleapis.com/${bucket}/${name}`;

      // Get use case and update project
      const projectUseCases = Providers.getProjectProvider().create();
      const updateImageUseCase = new UpdateProjectImageUseCase(projectUseCases.projectRepository);

      const result = await updateImageUseCase.execute({
        projectId: projectId,
        imageUrl: publicUrl,
      });

      if (result.success) {
        console.log(`Successfully updated project ${projectId} image: ${publicUrl}`);
      } else {
        console.error(`Failed to update project ${projectId} image:`, result.error);
      }
    } catch (error) {
      console.error("Error processing project image upload:", error);
    }
  }
);
