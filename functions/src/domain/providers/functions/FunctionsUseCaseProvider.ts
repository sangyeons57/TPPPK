/**
 * Functions use case provider
 * Groups Firebase Functions related use cases
 */

import { FunctionsRepository, RepositoryFactory } from '../../repositories';
import { FunctionsRepositoryFactoryContext } from '../../repositories';
import { HelloWorldUseCase } from '../../usecases/functions/HelloWorldUseCase';

export interface FunctionsUseCases {
  readonly helloWorldUseCase: HelloWorldUseCase;
  readonly functionsRepository: FunctionsRepository;
}

export class FunctionsUseCaseProvider {
  constructor(
    private readonly functionsRepositoryFactory: RepositoryFactory<FunctionsRepositoryFactoryContext, FunctionsRepository>
  ) {}

  create(options: {
    functionsContext?: FunctionsRepositoryFactoryContext;
  } = {}): FunctionsUseCases {
    const functionsRepository = this.functionsRepositoryFactory.create(
      options.functionsContext || {}
    );

    return {
      helloWorldUseCase: new HelloWorldUseCase(functionsRepository),
      functionsRepository,
    };
  }
}