/**
 * Dependency injection container for managing providers and their dependencies
 * Implements singleton pattern to ensure single instance throughout the application
 */
export class ProviderContainer {
  private static instance: ProviderContainer;
  private providers: Map<string, any> = new Map();
  private initialized = false;

  /**
   * Private constructor to prevent direct instantiation
   */
  private constructor() {}

  /**
   * Gets the singleton instance of ProviderContainer
   * @return {ProviderContainer} ProviderContainer singleton instance
   */
  public static getInstance(): ProviderContainer {
    if (!ProviderContainer.instance) {
      ProviderContainer.instance = new ProviderContainer();
    }
    return ProviderContainer.instance;
  }

  /**
   * Registers a provider instance with a unique key
   * @param {string} key - Unique identifier for the provider
   * @param {T} provider - Provider instance to register
   */
  public register<T>(key: string, provider: T): void {
    if (this.providers.has(key)) {
      throw new Error(`Provider with key '${key}' is already registered`);
    }
    this.providers.set(key, provider);
  }

  /**
   * Retrieves a provider instance by key
   * @param {string} key - Unique identifier for the provider
   * @return {T} Provider instance
   * @throws Error if provider is not found
   */
  public get<T>(key: string): T {
    const provider = this.providers.get(key);
    if (!provider) {
      throw new Error(`Provider with key '${key}' is not registered`);
    }
    return provider as T;
  }

  /**
   * Checks if a provider is registered
   * @param {string} key - Unique identifier for the provider
   * @return {boolean} True if provider is registered, false otherwise
   */
  public has(key: string): boolean {
    return this.providers.has(key);
  }

  /**
   * Removes a provider from the container
   * @param {string} key - Unique identifier for the provider
   * @return {boolean} True if provider was removed, false if not found
   */
  public remove(key: string): boolean {
    return this.providers.delete(key);
  }

  /**
   * Clears all registered providers
   */
  public clear(): void {
    this.providers.clear();
    this.initialized = false;
  }

  /**
   * Gets all registered provider keys
   * @return {string[]} Array of all registered provider keys
   */
  public getRegisteredKeys(): string[] {
    return Array.from(this.providers.keys());
  }

  /**
   * Marks the container as initialized
   * This prevents further registrations in production
   */
  public markAsInitialized(): void {
    this.initialized = true;
  }

  /**
   * Checks if the container is initialized
   * @return {boolean} True if initialized, false otherwise
   */
  public isInitialized(): boolean {
    return this.initialized;
  }

  /**
   * Registers multiple providers at once
   * @param {Record<string, any>} providers - Object containing key-provider pairs
   */
  public registerMultiple(providers: Record<string, any>): void {
    for (const [key, provider] of Object.entries(providers)) {
      this.register(key, provider);
    }
  }
}

/**
 * Provider registry keys for type-safe access
 */
export const ProviderKeys = {
  // Repository Factories
  FRIEND_REPOSITORY_FACTORY: "friendRepositoryFactory",
  MEMBER_REPOSITORY_FACTORY: "memberRepositoryFactory",
  USER_REPOSITORY_FACTORY: "userRepositoryFactory",
  PROJECT_REPOSITORY_FACTORY: "projectRepositoryFactory",
  PROJECT_WRAPPER_REPOSITORY_FACTORY: "projectWrapperRepositoryFactory",

  // UseCase Providers
  FRIEND_USECASE_PROVIDER: "friendUseCaseProvider",
  MEMBER_USECASE_PROVIDER: "memberUseCaseProvider",
  USER_USECASE_PROVIDER: "userUseCaseProvider",
  PROJECT_USECASE_PROVIDER: "projectUseCaseProvider",
  PROJECT_WRAPPER_USECASE_PROVIDER: "projectWrapperUseCaseProvider",
} as const;

/**
 * Type for provider keys
 */
export type ProviderKey = typeof ProviderKeys[keyof typeof ProviderKeys];
