/**
 * Functions repository factory implementation
 */

import { RepositoryFactory, FunctionsRepository, FunctionsRepositoryFactoryContext } from '../../domain/repositories';
import { FunctionsRepositoryImpl } from '../repositories/FunctionsRepositoryImpl';

export class FunctionsRepositoryFactory implements RepositoryFactory<FunctionsRepositoryFactoryContext, FunctionsRepository> {
  create(input: FunctionsRepositoryFactoryContext): FunctionsRepository {
    const config = input.functionsConfig || {};
    
    return new FunctionsRepositoryImpl({
      timeout: config.timeout,
      region: config.region,
      enableLogging: config.enableLogging,
    });
  }
}