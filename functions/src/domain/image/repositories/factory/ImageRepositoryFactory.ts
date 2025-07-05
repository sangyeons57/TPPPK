import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { ImageRepositoryFactoryContext } from './ImageRepositoryFactoryContext';
import { ImageRepository } from '../image.repository';
import { FirestoreImageDataSource } from '../../../../infrastructure/datasources/firestore/image.datasource';

/**
 * Factory for creating image repositories
 */
export class ImageRepositoryFactory implements RepositoryFactory<ImageRepository, ImageRepositoryFactoryContext> {
  /**
   * Creates an image repository instance
   * @param context - Optional context for image repository creation
   * @returns ImageRepository instance
   */
  create(context?: ImageRepositoryFactoryContext): ImageRepository {
    return new FirestoreImageDataSource();
  }
}