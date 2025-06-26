package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.repository.FakeProjectRepository
import com.example.domain.util.TestDataBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateProjectUseCaseTest {

    private lateinit var createProjectUseCase: CreateProjectUseCase
    private lateinit var fakeProjectRepository: FakeProjectRepository

    @Before
    fun setUp() {
        fakeProjectRepository = FakeProjectRepository()
        createProjectUseCase = CreateProjectUseCase(fakeProjectRepository)
    }

    @Test
    fun `create project success`() = runTest {
        // Given
        val projectName = "Test Project"
        val ownerId = "owner123"
        val description = "Test project description"

        // When
        val result = createProjectUseCase(
            name = projectName,
            ownerId = ownerId,
            description = description,
            imageUrl = null
        )

        // Then
        assertTrue("Project creation should succeed", result is CustomResult.Success)
        val projectId = (result as CustomResult.Success).data
        assertTrue("Project ID should not be empty", projectId.value.isNotEmpty())
    }

    @Test
    fun `create project with image URL success`() = runTest {
        // Given
        val projectName = "Test Project"
        val ownerId = "owner123"
        val description = "Test project description"
        val imageUrl = "https://example.com/image.jpg"

        // When
        val result = createProjectUseCase(
            name = projectName,
            ownerId = ownerId,
            description = description,
            imageUrl = imageUrl
        )

        // Then
        assertTrue("Project creation should succeed", result is CustomResult.Success)
        val projectId = (result as CustomResult.Success).data
        assertTrue("Project ID should not be empty", projectId.value.isNotEmpty())
    }

    @Test
    fun `create project with blank name fails`() = runTest {
        // Given
        val blankName = ""
        val ownerId = "owner123"

        // When & Then
        try {
            createProjectUseCase(
                name = blankName,
                ownerId = ownerId,
                description = null,
                imageUrl = null
            )
            assertTrue("Should throw exception for blank name", false)
        } catch (e: IllegalArgumentException) {
            // Expected behavior
            assertTrue("Should throw IllegalArgumentException", true)
        }
    }

    @Test
    fun `create project with blank owner ID fails`() = runTest {
        // Given
        val projectName = "Test Project"
        val blankOwnerId = ""

        // When & Then
        try {
            createProjectUseCase(
                name = projectName,
                ownerId = blankOwnerId,
                description = null,
                imageUrl = null
            )
            assertTrue("Should throw exception for blank owner ID", false)
        } catch (e: IllegalArgumentException) {
            // Expected behavior
            assertTrue("Should throw IllegalArgumentException", true)
        }
    }

    @Test
    fun `create project repository error`() = runTest {
        // Given
        val projectName = "Test Project"
        val ownerId = "owner123"
        fakeProjectRepository.setShouldThrowError(true)

        // When
        val result = createProjectUseCase(
            name = projectName,
            ownerId = ownerId,
            description = null,
            imageUrl = null
        )

        // Then
        assertTrue("Project creation should fail", result is CustomResult.Failure)
    }

    @Test
    fun `create project with null description`() = runTest {
        // Given
        val projectName = "Test Project"
        val ownerId = "owner123"

        // When
        val result = createProjectUseCase(
            name = projectName,
            ownerId = ownerId,
            description = null,
            imageUrl = null
        )

        // Then
        assertTrue("Project creation should succeed with null description", result is CustomResult.Success)
    }

    @Test
    fun `create project with empty description`() = runTest {
        // Given
        val projectName = "Test Project"
        val ownerId = "owner123"
        val emptyDescription = ""

        // When
        val result = createProjectUseCase(
            name = projectName,
            ownerId = ownerId,
            description = emptyDescription,
            imageUrl = null
        )

        // Then
        assertTrue("Project creation should succeed with empty description", result is CustomResult.Success)
    }

    @Test
    fun `create project stores project in repository`() = runTest {
        // Given
        val projectName = "Test Project"
        val ownerId = "owner123"
        val description = "Test description"

        // When
        val result = createProjectUseCase(
            name = projectName,
            ownerId = ownerId,
            description = description,
            imageUrl = null
        )

        // Then
        assertTrue("Project creation should succeed", result is CustomResult.Success)
        val projectId = (result as CustomResult.Success).data

        // Verify project is stored in repository
        val storedProjectResult = fakeProjectRepository.findById(projectId, com.google.firebase.firestore.Source.DEFAULT)
        assertTrue("Project should be stored in repository", storedProjectResult is CustomResult.Success)
        
        val storedProject = (storedProjectResult as CustomResult.Success).data as com.example.domain.model.base.Project
        assertEquals("Project name should match", projectName, storedProject.name.value)
        assertEquals("Owner ID should match", ownerId, storedProject.ownerId.value)
        assertEquals("Description should match", description, storedProject.description)
    }

    @Test
    fun `create project with various valid names`() = runTest {
        val validNames = listOf(
            "Simple Project",
            "Project-With-Dashes",
            "Project_With_Underscores",
            "Project123",
            "A",
            "Very Long Project Name That Exceeds Normal Length",
            "Project with émoji 🚀",
            "프로젝트 이름", // Korean
            "Project.With.Dots"
        )

        validNames.forEach { name ->
            // When
            val result = createProjectUseCase(
                name = name,
                ownerId = "owner123",
                description = null,
                imageUrl = null
            )

            // Then
            assertTrue("Project creation should succeed for name: $name", result is CustomResult.Success)
        }
    }
}