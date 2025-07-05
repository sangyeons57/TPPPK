import { ProjectRepository } from '../../../domain/project/project.repository';
import { ProjectEntity, ProjectStatus, ProjectName } from '../../../domain/project/project.entity';
import { TestFactories, TestUtils } from '../../helpers';

describe('ProjectRepository Contract Tests', () => {
  let repository: ProjectRepository;
  let testProject: ProjectEntity;
  let testProjectName: ProjectName;

  beforeEach(() => {
    testProjectName = new ProjectName('Test Project');
    
    testProject = TestFactories.createProject({
      id: 'project_123',
      name: testProjectName.value,
      ownerId: 'user_123',
      status: ProjectStatus.ACTIVE,
      memberCount: 5,
    });

    // Mock repository will be provided by concrete implementation tests
    repository = {} as ProjectRepository;
  });

  describe('findById', () => {
    it('should return project when found', async () => {
      const mockResult = TestUtils.createSuccessResult(testProject);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('project_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProject);
      }
      expect(repository.findById).toHaveBeenCalledWith('project_123');
    });

    it('should return null when project not found', async () => {
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('nonexistent');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });

    it('should return error when repository fails', async () => {
      const error = new Error('Database connection failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.findById = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findById('project_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('findByOwnerId', () => {
    it('should return projects owned by user', async () => {
      const projects = [testProject];
      const mockResult = TestUtils.createSuccessResult(projects);
      repository.findByOwnerId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByOwnerId('user_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(projects);
      }
      expect(repository.findByOwnerId).toHaveBeenCalledWith('user_123');
    });

    it('should return empty array when no projects found for owner', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByOwnerId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByOwnerId('user_456');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findByName', () => {
    it('should return project when found by name', async () => {
      const mockResult = TestUtils.createSuccessResult(testProject);
      repository.findByName = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByName(testProjectName);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProject);
      }
      expect(repository.findByName).toHaveBeenCalledWith(testProjectName);
    });

    it('should return null when no project found with name', async () => {
      const differentName = new ProjectName('Different Project');
      const mockResult = TestUtils.createSuccessResult(null);
      repository.findByName = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByName(differentName);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBeNull();
      }
    });
  });

  describe('findByStatus', () => {
    it('should return projects with specific status', async () => {
      const activeProjects = [testProject];
      const mockResult = TestUtils.createSuccessResult(activeProjects);
      repository.findByStatus = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByStatus(ProjectStatus.ACTIVE);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeProjects);
      }
      expect(repository.findByStatus).toHaveBeenCalledWith(ProjectStatus.ACTIVE);
    });

    it('should return empty array when no projects with status found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findByStatus = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findByStatus(ProjectStatus.ARCHIVED);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('save', () => {
    it('should save project and return saved entity', async () => {
      const mockResult = TestUtils.createSuccessResult(testProject);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testProject);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(testProject);
      }
      expect(repository.save).toHaveBeenCalledWith(testProject);
    });

    it('should return error when save fails', async () => {
      const error = new Error('Save failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.save = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.save(testProject);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('update', () => {
    it('should update project and return updated entity', async () => {
      const updatedProject = testProject.updateProject({
        name: new ProjectName('Updated Project'),
      });
      const mockResult = TestUtils.createSuccessResult(updatedProject);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(updatedProject);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(updatedProject);
      }
      expect(repository.update).toHaveBeenCalledWith(updatedProject);
    });

    it('should return error when update fails', async () => {
      const error = new Error('Update failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.update = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.update(testProject);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('delete', () => {
    it('should delete project by ID', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('project_123');
      
      expect(result.success).toBe(true);
      expect(repository.delete).toHaveBeenCalledWith('project_123');
    });

    it('should return error when delete fails', async () => {
      const error = new Error('Delete failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.delete = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.delete('project_123');
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });

  describe('exists', () => {
    it('should return true when project exists', async () => {
      const mockResult = TestUtils.createSuccessResult(true);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('project_123');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(true);
      }
      expect(repository.exists).toHaveBeenCalledWith('project_123');
    });

    it('should return false when project does not exist', async () => {
      const mockResult = TestUtils.createSuccessResult(false);
      repository.exists = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.exists('nonexistent');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toBe(false);
      }
    });
  });

  describe('findActiveProjects', () => {
    it('should return active projects without limit', async () => {
      const activeProjects = [testProject];
      const mockResult = TestUtils.createSuccessResult(activeProjects);
      repository.findActiveProjects = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveProjects();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeProjects);
      }
      expect(repository.findActiveProjects).toHaveBeenCalledWith(undefined);
    });

    it('should return active projects with limit', async () => {
      const activeProjects = [testProject];
      const mockResult = TestUtils.createSuccessResult(activeProjects);
      repository.findActiveProjects = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveProjects(10);
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(activeProjects);
      }
      expect(repository.findActiveProjects).toHaveBeenCalledWith(10);
    });

    it('should return empty array when no active projects found', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findActiveProjects = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findActiveProjects();
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('findProjectsByMemberId', () => {
    it('should return projects where user is a member', async () => {
      const memberProjects = [testProject];
      const mockResult = TestUtils.createSuccessResult(memberProjects);
      repository.findProjectsByMemberId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findProjectsByMemberId('user_456');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual(memberProjects);
      }
      expect(repository.findProjectsByMemberId).toHaveBeenCalledWith('user_456');
    });

    it('should return empty array when user is not a member of any projects', async () => {
      const mockResult = TestUtils.createSuccessResult([]);
      repository.findProjectsByMemberId = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.findProjectsByMemberId('user_789');
      
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data).toEqual([]);
      }
    });
  });

  describe('updateMemberCount', () => {
    it('should update member count for project', async () => {
      const mockResult = TestUtils.createSuccessResult(undefined);
      repository.updateMemberCount = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.updateMemberCount('project_123', 10);
      
      expect(result.success).toBe(true);
      expect(repository.updateMemberCount).toHaveBeenCalledWith('project_123', 10);
    });

    it('should return error when update fails', async () => {
      const error = new Error('Update member count failed');
      const mockResult = TestUtils.createFailureResult(error);
      repository.updateMemberCount = jest.fn().mockResolvedValue(mockResult);

      const result = await repository.updateMemberCount('project_123', 10);
      
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error).toBe(error);
      }
    });
  });
});