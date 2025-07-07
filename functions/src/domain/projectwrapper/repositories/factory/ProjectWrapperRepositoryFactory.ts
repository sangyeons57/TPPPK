import {RepositoryFactory} from "../../../shared/RepositoryFactory";
import {ProjectWrapperRepository} from "../projectwrapper.repository";
import {ProjectWrapperRepositoryFactoryContext} from "./ProjectWrapperRepositoryFactoryContext";

export interface ProjectWrapperRepositoryFactory extends RepositoryFactory<ProjectWrapperRepository, ProjectWrapperRepositoryFactoryContext> {
  create(context?: ProjectWrapperRepositoryFactoryContext): ProjectWrapperRepository;
}
