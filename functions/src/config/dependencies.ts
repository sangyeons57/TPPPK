import {FriendRepositoryFactory} from "../domain/friend/repositories/factory/FriendRepositoryFactory";
import {UserRepositoryFactory} from "../domain/user/repositories/factory/UserRepositoryFactory";
import {ProjectRepositoryFactory} from "../domain/project/repositories/factory/ProjectRepositoryFactory";
import {
  FriendUseCaseProvider,
  UserUseCaseProvider,
  ProjectUseCaseProvider,
} from "../business";
import { ProviderContainer, ProviderKeys } from "../infrastructure/container/ProviderContainer";
import { ImageProcessingService } from "../core/services/imageProcessing.service";
import { FirestoreImageDataSource, FirebaseStorageService } from "../infrastructure/datasources/firestore/image.datasource";

/**
 * Configuration class for setting up dependency injection
 * Registers all repository factories and use case providers
 */
export class DependencyConfig {
  private static isInitialized = false;
  private static container: ProviderContainer;

  /**
   * Initializes all dependencies for the application
   * Should be called once at application startup
   */
  public static initialize(): void {
    if (DependencyConfig.isInitialized) {
      console.warn("Dependencies already initialized. Skipping...");
      return;
    }

    console.log("Initializing dependencies...");
    DependencyConfig.container = ProviderContainer.getInstance();
    // Register repository factories
    DependencyConfig.registerRepositoryFactories();
    // Register use case providers
    DependencyConfig.registerUseCaseProviders();
    // Mark container as initialized
    DependencyConfig.container.markAsInitialized();
    DependencyConfig.isInitialized = true;
    console.log("Dependencies initialized successfully");
    console.log("Registered providers:", DependencyConfig.container.getRegisteredKeys());
  }

  /**
   * Gets the provider container instance
   * @return ProviderContainer instance
   * @throws Error if dependencies are not initialized
   */
  public static getContainer(): ProviderContainer {
    if (!DependencyConfig.isInitialized) {
      throw new Error("Dependencies not initialized. Call DependencyConfig.initialize() first.");
    }
    return DependencyConfig.container;
  }

  /**
   * Registers all repository factories
   */
  private static registerRepositoryFactories(): void {
    console.log("Registering repository factories...");

    const container = DependencyConfig.container;


    container.register(
      ProviderKeys.FRIEND_REPOSITORY_FACTORY,
      new FriendRepositoryFactory()
    );

    container.register(
      ProviderKeys.USER_REPOSITORY_FACTORY,
      new UserRepositoryFactory()
    );

    container.register(
      ProviderKeys.PROJECT_REPOSITORY_FACTORY,
      new ProjectRepositoryFactory()
    );
    
    // Register Image Processing Service instead of factory
    const imageRepository = new FirestoreImageDataSource();
    const imageStorageService = new FirebaseStorageService();
    const imageProcessingService = new ImageProcessingService(imageRepository, imageStorageService);
    
    container.register(
      ProviderKeys.IMAGE_PROCESSING_SERVICE,
      imageProcessingService
    );

    console.log("Repository factories and services registered");
  }

  /**
   * Registers all use case providers
   */
  private static registerUseCaseProviders(): void {
    console.log("Registering use case providers...");

    const container = DependencyConfig.container;


    // Friend Use Case Provider
    container.register(
      ProviderKeys.FRIEND_USECASE_PROVIDER,
      new FriendUseCaseProvider(
        container.get(ProviderKeys.FRIEND_REPOSITORY_FACTORY),
        container.get(ProviderKeys.USER_REPOSITORY_FACTORY)
      )
    );

    // User Use Case Provider
    container.register(
      ProviderKeys.USER_USECASE_PROVIDER,
      new UserUseCaseProvider(
        container.get(ProviderKeys.USER_REPOSITORY_FACTORY),
        container.get(ProviderKeys.IMAGE_PROCESSING_SERVICE)
      )
    );

    // Project Use Case Provider
    container.register(
      ProviderKeys.PROJECT_USECASE_PROVIDER,
      new ProjectUseCaseProvider(
        container.get(ProviderKeys.PROJECT_REPOSITORY_FACTORY),
        container.get(ProviderKeys.IMAGE_PROCESSING_SERVICE)
      )
    );

    console.log("Use case providers registered");
  }

  /**
   * Resets the dependency injection system
   * Should only be used for testing purposes
   */
  public static reset(): void {
    console.log("Resetting dependencies...");
    if (DependencyConfig.container) {
      DependencyConfig.container.clear();
    }
    DependencyConfig.isInitialized = false;
    console.log("Dependencies reset");
  }

  /**
   * Gets status information about the dependency injection system
   * @returns Object containing initialization status and registered providers
   */
  public static getStatus(): {
    isInitialized: boolean;
    registeredProviders: string[];
    containerInitialized: boolean;
  } {
    return {
      isInitialized: DependencyConfig.isInitialized,
      registeredProviders: DependencyConfig.container?.getRegisteredKeys() || [],
      containerInitialized: DependencyConfig.container?.isInitialized() || false,
    };
  }
}

/**
 * Helper function to get a provider from the container
 * @param key - Provider key
 * @returns Provider instance
 */
export function getProvider<T>(key: string): T {
  const container = DependencyConfig.getContainer();
  return container.get<T>(key);
}

/**
 * Type-safe helper functions for getting specific providers
 */
export const Providers = {
  getFriendProvider: () => getProvider<FriendUseCaseProvider>(ProviderKeys.FRIEND_USECASE_PROVIDER),
  getUserProvider: () => getProvider<UserUseCaseProvider>(ProviderKeys.USER_USECASE_PROVIDER),
  getProjectProvider: () => getProvider<ProjectUseCaseProvider>(ProviderKeys.PROJECT_USECASE_PROVIDER),
} as const;
