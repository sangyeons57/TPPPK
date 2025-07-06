import {ProviderContainer, ProviderKeys} from "./ProviderContainer";

// Datasources
import {FirestoreUserDataSource} from "../datasources/firestore/user.datasource";
import {FirestoreFriendDataSource} from "../datasources/firestore/friend.datasource";
import {FirestoreProjectDataSource} from "../datasources/firestore/project.datasource";

// Repository Implementations
import {UserRepositoryImpl} from "../repositories/user.repository.impl";
import {FriendRepositoryImpl} from "../repositories/friend.repository.impl";
import {ProjectRepositoryImpl} from "../repositories/project.repository.impl";

// Repository Interfaces
import {UserRepository} from "../../domain/user/repositories/user.repository";
import {FriendRepository} from "../../domain/friend/repositories/friend.repository";
import {ProjectRepository} from "../../domain/project/repositories/project.repository";

/**
 * DI 컨테이너 설정 및 의존성 등록
 */
export function setupContainer(): void {
  const container = ProviderContainer.getInstance();

  // Datasources 등록
  const userDataSource = new FirestoreUserDataSource();
  const friendDataSource = new FirestoreFriendDataSource();
  const projectDataSource = new FirestoreProjectDataSource();

  // Repository 구현체 등록
  const userRepository: UserRepository = new UserRepositoryImpl(userDataSource);
  const friendRepository: FriendRepository = new FriendRepositoryImpl(friendDataSource);
  const projectRepository: ProjectRepository = new ProjectRepositoryImpl(projectDataSource);

  // 컨테이너에 등록
  container.register("userRepository", userRepository);
  container.register("friendRepository", friendRepository);
  container.register("projectRepository", projectRepository);

  // 기존 Factory 패턴 지원을 위한 Factory 등록
  container.register(ProviderKeys.USER_REPOSITORY_FACTORY, () => userRepository);
  container.register(ProviderKeys.FRIEND_REPOSITORY_FACTORY, () => friendRepository);
  container.register(ProviderKeys.PROJECT_REPOSITORY_FACTORY, () => projectRepository);

  container.markAsInitialized();
}

export function getUserRepository(): UserRepository {
  return ProviderContainer.getInstance().get<UserRepository>("userRepository");
}

export function getFriendRepository(): FriendRepository {
  return ProviderContainer.getInstance().get<FriendRepository>("friendRepository");
}

export function getProjectRepository(): ProjectRepository {
  return ProviderContainer.getInstance().get<ProjectRepository>("projectRepository");
}
