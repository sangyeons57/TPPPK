import { RepositoryFactoryContext } from '../../../shared/RepositoryFactory';

/**
 * Context for image-related repository creation
 */
export interface ImageRepositoryFactoryContext extends RepositoryFactoryContext {
  bucketName?: string;
  imagePath?: string;
}