/**
 * Dependency injection container
 * Manages creation and lifecycle of dependencies
 */

import { AuthRepositoryFactory, UserRepositoryFactory, FunctionsRepositoryFactory } from '../factories';
import { AuthSessionUseCaseProvider, AuthRegistrationUseCaseProvider, FunctionsUseCaseProvider } from '../../domain/providers';

export class Container {
  private static instance: Container;
  
  // Repository factories
  private readonly authRepositoryFactory: AuthRepositoryFactory;
  private readonly userRepositoryFactory: UserRepositoryFactory;
  private readonly functionsRepositoryFactory: FunctionsRepositoryFactory;
  
  // Use case providers
  private readonly authSessionUseCaseProvider: AuthSessionUseCaseProvider;
  private readonly authRegistrationUseCaseProvider: AuthRegistrationUseCaseProvider;
  private readonly functionsUseCaseProvider: FunctionsUseCaseProvider;

  private constructor() {
    // Initialize repository factories
    this.authRepositoryFactory = new AuthRepositoryFactory();
    this.userRepositoryFactory = new UserRepositoryFactory();
    this.functionsRepositoryFactory = new FunctionsRepositoryFactory();
    
    // Initialize use case providers
    this.authSessionUseCaseProvider = new AuthSessionUseCaseProvider(
      this.authRepositoryFactory,
      this.userRepositoryFactory
    );
    
    this.authRegistrationUseCaseProvider = new AuthRegistrationUseCaseProvider(
      this.authRepositoryFactory,
      this.userRepositoryFactory
    );
    
    this.functionsUseCaseProvider = new FunctionsUseCaseProvider(
      this.functionsRepositoryFactory
    );
  }

  public static getInstance(): Container {
    if (!Container.instance) {
      Container.instance = new Container();
    }
    return Container.instance;
  }

  // Factory getters
  public getAuthRepositoryFactory(): AuthRepositoryFactory {
    return this.authRepositoryFactory;
  }

  public getUserRepositoryFactory(): UserRepositoryFactory {
    return this.userRepositoryFactory;
  }

  public getFunctionsRepositoryFactory(): FunctionsRepositoryFactory {
    return this.functionsRepositoryFactory;
  }

  // Use case provider getters
  public getAuthSessionUseCaseProvider(): AuthSessionUseCaseProvider {
    return this.authSessionUseCaseProvider;
  }

  public getAuthRegistrationUseCaseProvider(): AuthRegistrationUseCaseProvider {
    return this.authRegistrationUseCaseProvider;
  }

  public getFunctionsUseCaseProvider(): FunctionsUseCaseProvider {
    return this.functionsUseCaseProvider;
  }

  // Convenience methods for getting use cases
  public getAuthSessionUseCases() {
    return this.authSessionUseCaseProvider.create();
  }

  public getAuthRegistrationUseCases() {
    return this.authRegistrationUseCaseProvider.create();
  }

  public getFunctionsUseCases() {
    return this.functionsUseCaseProvider.create();
  }
}

// Export singleton instance
export const container = Container.getInstance();