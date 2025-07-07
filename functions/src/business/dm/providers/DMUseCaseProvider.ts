import {RepositoryFactory} from "../../../domain/shared/RepositoryFactory";
import {DMChannelRepository} from "../../../domain/dmchannel/repositories/dmchannel.repository";
import {DMWrapperRepository} from "../../../domain/dmwrapper/repositories/dmwrapper.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {DMChannelRepositoryFactoryContext} from "../../../domain/dmchannel/repositories/factory/DMChannelRepositoryFactoryContext";
import {DMWrapperRepositoryFactoryContext} from "../../../domain/dmwrapper/repositories/factory/DMWrapperRepositoryFactoryContext";
import {UserRepositoryFactoryContext} from "../../../domain/user/repositories/factory/UserRepositoryFactoryContext";
import {CreateDMChannelUseCase} from "../usecases/createDMChannel.usecase";

/**
 * DM UseCases collection
 */
export interface DMUseCases {
  createDMChannelUseCase: CreateDMChannelUseCase;
  dmChannelRepository: DMChannelRepository;
  dmWrapperRepository: DMWrapperRepository;
  userRepository: UserRepository;
}

/**
 * Provider for DM-related use cases
 */
export class DMUseCaseProvider {
  constructor(
    private readonly dmChannelRepositoryFactory: RepositoryFactory<DMChannelRepository, DMChannelRepositoryFactoryContext>,
    private readonly dmWrapperRepositoryFactory: RepositoryFactory<DMWrapperRepository, DMWrapperRepositoryFactoryContext>,
    private readonly userRepositoryFactory: RepositoryFactory<UserRepository, UserRepositoryFactoryContext>
  ) {}

  /**
   * Creates and returns all DM use cases with their dependencies
   * @return {DMUseCases} Collection of DM use cases
   */
  create(): DMUseCases {
    const dmChannelRepository = this.dmChannelRepositoryFactory.create({});
    const dmWrapperRepository = this.dmWrapperRepositoryFactory.create({});
    const userRepository = this.userRepositoryFactory.create({});

    return {
      createDMChannelUseCase: new CreateDMChannelUseCase(
        userRepository,
        dmChannelRepository,
        dmWrapperRepository
      ),
      dmChannelRepository,
      dmWrapperRepository,
      userRepository,
    };
  }
}