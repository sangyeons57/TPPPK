import { ProjectEntity, ProjectName, ProjectDescription, ProjectImage, ProjectStatus } from '../../domain/project/project.entity';
import { ValidationError } from '../../core/errors';
import { TestFactories, TestUtils } from '../helpers';

describe('Project Domain Entities', () => {
  describe('ProjectName Value Object', () => {
    it('should create valid project name', () => {
      const name = new ProjectName('My Project');
      expect(name.value).toBe('My Project');
    });

    it('should throw ValidationError for short project name', () => {
      expect(() => new ProjectName('ab')).toThrow(ValidationError);
      expect(() => new ProjectName('ab')).toThrow('Project name must be at least 3 characters');
    });

    it('should throw ValidationError for long project name', () => {
      const longName = 'a'.repeat(101);
      expect(() => new ProjectName(longName)).toThrow(ValidationError);
      expect(() => new ProjectName(longName)).toThrow('Project name must be at most 100 characters');
    });

    it('should check equality correctly', () => {
      const name1 = new ProjectName('Project One');
      const name2 = new ProjectName('Project One');
      const name3 = new ProjectName('Project Two');

      expect(name1.equals(name2)).toBe(true);
      expect(name1.equals(name3)).toBe(false);
    });
  });

  describe('ProjectDescription Value Object', () => {
    it('should create valid project description', () => {
      const description = new ProjectDescription('This is a test project');
      expect(description.value).toBe('This is a test project');
    });

    it('should allow empty description', () => {
      const description = new ProjectDescription('');
      expect(description.value).toBe('');
    });

    it('should throw ValidationError for long description', () => {
      const longDescription = 'a'.repeat(1001);
      expect(() => new ProjectDescription(longDescription)).toThrow(ValidationError);
      expect(() => new ProjectDescription(longDescription)).toThrow('Project description must be at most 1000 characters');
    });

    it('should check equality correctly', () => {
      const desc1 = new ProjectDescription('Same description');
      const desc2 = new ProjectDescription('Same description');
      const desc3 = new ProjectDescription('Different description');

      expect(desc1.equals(desc2)).toBe(true);
      expect(desc1.equals(desc3)).toBe(false);
    });
  });

  describe('ProjectImage Value Object', () => {
    it('should create valid project image with HTTPS URL', () => {
      const image = new ProjectImage('https://example.com/image.jpg');
      expect(image.value).toBe('https://example.com/image.jpg');
    });

    it('should throw ValidationError for non-HTTPS URL', () => {
      expect(() => new ProjectImage('http://example.com/image.jpg')).toThrow(ValidationError);
      expect(() => new ProjectImage('http://example.com/image.jpg')).toThrow('Project image must be a valid HTTPS URL');
    });

    it('should throw ValidationError for invalid URL format', () => {
      expect(() => new ProjectImage('not-a-url')).toThrow(ValidationError);
      expect(() => new ProjectImage('not-a-url')).toThrow('Project image must be a valid HTTPS URL');
    });

    it('should check equality correctly', () => {
      const image1 = new ProjectImage('https://example.com/image.jpg');
      const image2 = new ProjectImage('https://example.com/image.jpg');
      const image3 = new ProjectImage('https://example.com/other.jpg');

      expect(image1.equals(image2)).toBe(true);
      expect(image1.equals(image3)).toBe(false);
    });
  });

  describe('ProjectEntity', () => {
    let mockDateNow: jest.SpyInstance;

    beforeEach(() => {
      mockDateNow = TestUtils.mockDateNow(TestUtils.createMockDate('2023-01-15T12:00:00.000Z'));
    });

    afterEach(() => {
      TestUtils.restoreAllMocks();
    });

    it('should create project entity with required fields', () => {
      const project = TestFactories.createProject({
        id: 'project_123',
        name: 'Test Project',
        ownerId: 'user_123',
        status: ProjectStatus.ACTIVE,
        memberCount: 1,
      });

      expect(project.id).toBe('project_123');
      expect(project.name.value).toBe('Test Project');
      expect(project.ownerId).toBe('user_123');
      expect(project.status).toBe(ProjectStatus.ACTIVE);
      expect(project.memberCount).toBe(1);
      expect(project.description).toBeUndefined();
      expect(project.image).toBeUndefined();
    });

    it('should create project entity with optional fields', () => {
      const project = TestFactories.createProject({
        name: 'Full Project',
        description: 'A complete project with all fields',
        image: 'https://example.com/project.jpg',
      });

      expect(project.name.value).toBe('Full Project');
      expect(project.description?.value).toBe('A complete project with all fields');
      expect(project.image?.value).toBe('https://example.com/project.jpg');
    });

    describe('updateProject', () => {
      it('should update project name', () => {
        const originalProject = TestFactories.createProject({ name: 'Original Name' });
        const newName = new ProjectName('Updated Name');

        const updatedProject = originalProject.updateProject({ name: newName });

        expect(updatedProject.name).toBe(newName);
        expect(updatedProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedProject.id).toBe(originalProject.id);
        expect(updatedProject.ownerId).toBe(originalProject.ownerId);
      });

      it('should update project description', () => {
        const originalProject = TestFactories.createProject();
        const newDescription = new ProjectDescription('New description');

        const updatedProject = originalProject.updateProject({ description: newDescription });

        expect(updatedProject.description).toBe(newDescription);
        expect(updatedProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
      });

      it('should update project image', () => {
        const originalProject = TestFactories.createProject();
        const newImage = new ProjectImage('https://example.com/new-image.jpg');

        const updatedProject = originalProject.updateProject({ image: newImage });

        expect(updatedProject.image).toBe(newImage);
        expect(updatedProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
      });

      it('should remove description when explicitly set to undefined', () => {
        const originalProject = TestFactories.createProject({
          description: 'Original description',
        });

        const updatedProject = originalProject.updateProject({ description: undefined });

        expect(updatedProject.description).toBeUndefined();
      });

      it('should keep existing values when not specified in updates', () => {
        const originalProject = TestFactories.createProject({
          name: 'Original Name',
          description: 'Original description',
        });
        const newName = new ProjectName('Updated Name');

        const updatedProject = originalProject.updateProject({ name: newName });

        expect(updatedProject.name).toBe(newName);
        expect(updatedProject.description?.value).toBe('Original description');
      });
    });

    describe('updateMemberCount', () => {
      it('should update member count', () => {
        const originalProject = TestFactories.createProject({ memberCount: 1 });

        const updatedProject = originalProject.updateMemberCount(5);

        expect(updatedProject.memberCount).toBe(5);
        expect(updatedProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(updatedProject.id).toBe(originalProject.id);
      });

      it('should allow zero member count', () => {
        const originalProject = TestFactories.createProject({ memberCount: 1 });

        const updatedProject = originalProject.updateMemberCount(0);

        expect(updatedProject.memberCount).toBe(0);
      });

      it('should throw ValidationError for negative member count', () => {
        const project = TestFactories.createProject();

        expect(() => project.updateMemberCount(-1)).toThrow(ValidationError);
        expect(() => project.updateMemberCount(-1)).toThrow('Member count cannot be negative');
      });
    });

    describe('archive', () => {
      it('should archive active project', () => {
        const activeProject = TestFactories.createProject({ status: ProjectStatus.ACTIVE });

        const archivedProject = activeProject.archive();

        expect(archivedProject.status).toBe(ProjectStatus.ARCHIVED);
        expect(archivedProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(archivedProject.id).toBe(activeProject.id);
      });
    });

    describe('activate', () => {
      it('should activate archived project', () => {
        const archivedProject = TestFactories.createProject({ status: ProjectStatus.ARCHIVED });

        const activeProject = archivedProject.activate();

        expect(activeProject.status).toBe(ProjectStatus.ACTIVE);
        expect(activeProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(activeProject.id).toBe(archivedProject.id);
      });
    });

    describe('delete', () => {
      it('should delete project', () => {
        const activeProject = TestFactories.createProject({ status: ProjectStatus.ACTIVE });

        const deletedProject = activeProject.delete();

        expect(deletedProject.status).toBe(ProjectStatus.DELETED);
        expect(deletedProject.updatedAt).toEqual(new Date('2023-01-15T12:00:00.000Z'));
        expect(deletedProject.id).toBe(activeProject.id);
      });
    });
  });
});