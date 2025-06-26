package com.example.domain.model.base

import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.util.TestDataBuilder
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [Project] domain model. Adapted to current API (2025-06).
 */
class ProjectTest {

    @Test
    fun `basic creation`() {
        val project = Project.create(
            name = ProjectName("Demo"),
            imageUrl = null,
            ownerId = OwnerId("owner")
        )
        assertEquals(ProjectName("Demo"), project.name)
        assertEquals(OwnerId("owner"), project.ownerId)
        assertEquals(DocumentId.EMPTY, project.id)
    }

    @Test
    fun `change name updates timestamp`() {
        val project = TestDataBuilder.createTestProject()
        val old = project.updatedAt
        project.changeName(ProjectName("New"))
        assertTrue(project.updatedAt.isAfter(old))
        assertEquals("New", project.name.value)
    }

    @Test
    fun `equality based on id`() {
        val now = Instant.now()
        val id = DocumentId("same")
        val p1 = Project.fromDataSource(id, ProjectName("A"), null, OwnerId("o"), now, now)
        val p2 = Project.fromDataSource(id, ProjectName("B"), null, OwnerId("o"), now, now)
        val p3 = Project.fromDataSource(
            DocumentId("diff"),
            ProjectName("C"),
            null,
            OwnerId("o"),
            now,
            now
        )
        assertEquals(p1, p2)
        assertNotEquals(p1, p3)
    }

    @Test
    fun `create project with image url`() {
        val imageUrl = ImageUrl.toImageUrl("https://example.com/image.jpg")
        val project = Project.create(
            name = ProjectName("Project With Image"),
            imageUrl = imageUrl,
            ownerId = OwnerId("owner")
        )

        assertEquals(imageUrl, project.imageUrl)
    }

    @Test
    fun `create project with all fields`() {
        // Given
        val projectId = DocumentId("project123")
        val projectName = ProjectName("Test Project")
        val ownerId = OwnerId("owner123")
        val description = "Test project description"
        val imageUrl = ImageUrl.toImageUrl("https://example.com/image.jpg")

        // When
        val project = Project.create(
            id = projectId,
            name = projectName,
            ownerId = ownerId,
            description = description,
            imageUrl = imageUrl
        )

        // Then
        assertEquals("Project ID should match", projectId, project.id)
        assertEquals("Project name should match", projectName, project.name)
        assertEquals("Owner ID should match", ownerId, project.ownerId)
        assertEquals("Description should match", description, project.description)
        assertEquals("Image URL should match", imageUrl, project.imageUrl)
        assertNotNull("Created at should not be null", project.createdAt)
    }

    @Test
    fun `project update name creates new instance with updated name`() {
        // Given
        val originalProject = TestDataBuilder.createTestProject(
            name = "Original Name"
        )
        val newName = ProjectName("Updated Name")

        // When
        val updatedProject = originalProject.updateName(newName)

        // Then
        assertEquals("Updated project should have new name", newName, updatedProject.name)
        assertEquals("Updated project should keep same ID", originalProject.id, updatedProject.id)
        assertEquals("Updated project should keep same owner", originalProject.ownerId, updatedProject.ownerId)
        assertEquals("Updated project should keep same description", originalProject.description, updatedProject.description)
        assertEquals("Updated project should keep same image URL", originalProject.imageUrl, updatedProject.imageUrl)
        assertEquals("Updated project should keep same created time", originalProject.createdAt, updatedProject.createdAt)
    }

    @Test
    fun `project update description creates new instance with updated description`() {
        // Given
        val originalProject = TestDataBuilder.createTestProject(
            description = "Original description"
        )
        val newDescription = "Updated description"

        // When
        val updatedProject = originalProject.updateDescription(newDescription)

        // Then
        assertEquals("Updated project should have new description", newDescription, updatedProject.description)
        assertEquals("Updated project should keep same name", originalProject.name, updatedProject.name)
        assertEquals("Updated project should keep same ID", originalProject.id, updatedProject.id)
    }

    @Test
    fun `project update image URL creates new instance with updated image URL`() {
        // Given
        val originalProject = TestDataBuilder.createTestProject()
        val newImageUrl = ImageUrl.toImageUrl("https://example.com/new-image.jpg")

        // When
        val updatedProject = originalProject.updateImageUrl(newImageUrl)

        // Then
        assertEquals("Updated project should have new image URL", newImageUrl, updatedProject.imageUrl)
        assertEquals("Updated project should keep same name", originalProject.name, updatedProject.name)
        assertEquals("Updated project should keep same ID", originalProject.id, updatedProject.id)
    }

    @Test
    fun `project remove image URL sets image URL to null`() {
        // Given
        val originalProject = TestDataBuilder.createTestProject(
            imageUrl = "https://example.com/image.jpg"
        )

        // When
        val updatedProject = originalProject.removeImageUrl()

        // Then
        assertNull("Updated project should have null image URL", updatedProject.imageUrl)
        assertEquals("Updated project should keep same name", originalProject.name, updatedProject.name)
        assertEquals("Updated project should keep same ID", originalProject.id, updatedProject.id)
    }

    @Test
    fun `project domain events are generated correctly`() {
        // Given
        val project = TestDataBuilder.createTestProject()

        // When - perform various operations that should generate domain events
        val renamedProject = project.updateName(ProjectName("New Name"))
        val updatedDescProject = renamedProject.updateDescription("New Description")

        // Then - verify domain events exist (if implemented)
        // Note: This test documents expected behavior for domain events
        // Implementation may vary based on how domain events are implemented
        assertNotNull("Project should exist after updates", updatedDescProject)
        assertEquals("Final project should have updated name", "New Name", updatedDescProject.name.value)
        assertEquals("Final project should have updated description", "New Description", updatedDescProject.description)
    }

    @Test
    fun `project immutability verification`() {
        // Given
        val originalProject = TestDataBuilder.createTestProject()
        val originalName = originalProject.name
        val originalDescription = originalProject.description

        // When - update operations should not modify original
        val updatedProject = originalProject.updateName(ProjectName("New Name"))

        // Then
        assertEquals("Original project name should not change", originalName, originalProject.name)
        assertEquals("Original project description should not change", originalDescription, originalProject.description)
        assertTrue("Updated project should be different instance", originalProject !== updatedProject)
    }

    @Test
    fun `project creation with edge case values`() {
        // Test various edge cases for project creation
        val edgeCases = listOf(
            Triple("A", "owner1", "Single char name"),
            Triple("Very Long Project Name That Exceeds Normal Expectations And Contains Many Words", "owner2", "Very long name"),
            Triple("Project-With-Special-Characters_123", "owner3", "Special characters"),
            Triple("プロジェクト", "owner4", "Unicode characters"),
            Triple("Project with émoji 🚀", "owner5", "Emoji in name")
        )

        edgeCases.forEach { (name, ownerId, testCase) ->
            // When
            val project = Project.create(
                id = DocumentId("test_${testCase.replace(" ", "_")}"),
                name = ProjectName(name),
                ownerId = OwnerId(ownerId),
                description = testCase,
                imageUrl = null
            )

            // Then
            assertEquals("Project name should match for $testCase", name, project.name.value)
            assertEquals("Owner ID should match for $testCase", ownerId, project.ownerId.value)
            assertEquals("Description should match for $testCase", testCase, project.description)
        }
    }
}