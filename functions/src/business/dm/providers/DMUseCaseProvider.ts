import {RepositoryFactory} from "../../../domain/shared/RepositoryFactory";
import {DMChannelRepository} from "../../../domain/dmchannel/repositories/dmchannel.repository";
import {DMWrapperRepository} from "../../../domain/dmwrapper/repositories/dmwrapper.repository";
import {UserRepository} from "../../../domain/user/repositories/user.repository";
import {DMChannelRepositoryFactoryContext} from "../../../domain/dmchannel/repositories/factory/DMChannelRepositoryFactoryContext";
import {DMWrapperRepositoryFactoryContext} from "../../../domain/dmwrapper/repositories/factory/DMWrapperRepositoryFactoryContext";
import {UserRepositoryFactoryContext} from "../../../domain/user/repositories/factory/UserRepositoryFactoryContext";
import {CreateDMChannelUseCase} from "../usecases/createDMChannel.usecase";
import {BlockDMChannelUseCase} from "../usecases/blockDMChannel.usecase";
import {UnblockDMChannelUseCase} from "../usecases/unblockDMChannel.usecase";

/**
 * DM UseCases collection
 */
export interface DMUseCases {
  createDMChannelUseCase: CreateDMChannelUseCase;
  blockDMChannelUseCase: BlockDMChannelUseCase;
  unblockDMChannelUseCase: UnblockDMChannelUseCase;
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
      blockDMChannelUseCase: new BlockDMChannelUseCase(
        dmChannelRepository,
        dmWrapperRepository
      ),
      unblockDMChannelUseCase: new UnblockDMChannelUseCase(
        dmChannelRepository,
        dmWrapperRepository,
        userRepository
      ),
      dmChannelRepository,
      dmWrapperRepository,
      userRepository,
    };
  }
}