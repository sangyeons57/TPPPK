/**
 * Generic repository factory interface
 * Provides a contract for creating repositories with optional context
 */
export interface RepositoryFactory<TRepository, TContext = any> {
  /**
   * Creates a repository instance with optional context
   * @param context - Optional context for repository creation
   * @returns Repository instance
   */
  create(context?: TContext): TRepository;
}

/**
 * Base context interface for repository factory contexts
 */
export interface RepositoryFactoryContext {
  [key: string]: any;
}

