package com.example.domain.model.channel

// Imports for testing (JUnit, Assertions, Mocking - assuming MockK or similar)
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock // Or io.mockk.mockk
import org.mockito.junit.MockitoJUnitRunner // Or equivalent test runner
import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import com.example.domain.repository.base.ProjectRoleRepository
import kotlinx.coroutines.runBlocking // For testing suspend functions
import org.mockito.Mockito.`when` // Or io.mockk.coEvery for MockK

@RunWith(MockitoJUnitRunner::class) // Or appropriate runner for your mocking framework
class ChannelPermissionTest {

    // Mock the dependency
    @Mock
    lateinit var mockRoleRepository: ProjectRoleRepository

    // Test data
    private val testChannelId = "test_channel_123"
    private val testUserId = "user_abc"
    private val testProjectId = "project_xyz"
    private val testRoleIdMember = "role_member"
    private val testRoleIdAdmin = "role_admin"

    @Before
    fun setUp() {
        // Setup common mock behaviors if needed before each test
    }

    // --- Test cases will go here ---

    @Test
    fun `hasPermission returns override value when overridePermissions is set`() = runBlocking {
        // Arrange: Create permission with an override (READ=true, SEND=false)
        val overrides = mapOf(
            RolePermission.READ_MESSAGES to true,
            RolePermission.SEND_MESSAGES to false
        )
        val permission = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = testProjectId,
            roleIds = listOf(testRoleIdMember), // User has a role, but override should take precedence
            overridePermissions = overrides
        )

        // Mock repository behavior (although it shouldn't be called due to override)
        val memberRole = Role(
            id = testRoleIdMember, 
            name = "Member", 
            permissions = mapOf(RolePermission.READ_MESSAGES to true, RolePermission.SEND_MESSAGES to true)
        )
        `when`(mockRoleRepository.getRole(testProjectId, testRoleIdMember)).thenReturn(Result.success(memberRole))

        // Act & Assert
        assertTrue("READ_MESSAGES should be true due to override", permission.hasPermission(RolePermission.READ_MESSAGES, mockRoleRepository))
        assertFalse("SEND_MESSAGES should be false due to override", permission.hasPermission(RolePermission.SEND_MESSAGES, mockRoleRepository))
        // Check a permission not in overrides - should fall back to role (or default false if role doesn't grant it)
        // Let's assume MANAGE_MESSAGES is false in the role
        assertFalse("MANAGE_MESSAGES should be false as it's not overridden and role doesn't grant it", permission.hasPermission(RolePermission.MANAGE_MESSAGES, mockRoleRepository))
    }

    @Test
    fun `hasPermission checks roles when override is not set`() = runBlocking {
        // Arrange: Permission with no overrides, user has two roles (Member, Admin)
        val permission = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = testProjectId,
            roleIds = listOf(testRoleIdMember, testRoleIdAdmin),
            overridePermissions = null // No overrides
        )

        // Mock roles: Member has READ, Admin has SEND
        val memberRole = Role(
            id = testRoleIdMember, 
            name = "Member", 
            permissions = mapOf(RolePermission.READ_MESSAGES to true, RolePermission.SEND_MESSAGES to false)
        )
        val adminRole = Role(
            id = testRoleIdAdmin, 
            name = "Admin", 
            permissions = mapOf(RolePermission.READ_MESSAGES to false, RolePermission.SEND_MESSAGES to true)
        )

        // Mock repository responses
        `when`(mockRoleRepository.getRole(testProjectId, testRoleIdMember)).thenReturn(Result.success(memberRole))
        `when`(mockRoleRepository.getRole(testProjectId, testRoleIdAdmin)).thenReturn(Result.success(adminRole))

        // Act & Assert
        // User should have READ because Member role grants it
        assertTrue("READ_MESSAGES should be true via Member role", permission.hasPermission(RolePermission.READ_MESSAGES, mockRoleRepository))
        // User should have SEND because Admin role grants it
        assertTrue("SEND_MESSAGES should be true via Admin role", permission.hasPermission(RolePermission.SEND_MESSAGES, mockRoleRepository))
        // User should not have MANAGE because neither role grants it
        assertFalse("MANAGE_MESSAGES should be false as neither role grants it", permission.hasPermission(RolePermission.MANAGE_MESSAGES, mockRoleRepository))
    }

    @Test
    fun `hasPermission returns default DM permissions when projectId is null`() = runBlocking {
        // Arrange: Permission with projectId = null (DM channel)
        // Overrides and roles should be ignored for DM channels according to current logic.
        val overrides = mapOf(RolePermission.SEND_MESSAGES to false) // Intentionally set an override to ensure it's ignored
        val permission = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = null, // DM Channel
            roleIds = listOf(testRoleIdAdmin), // Has a role, but should be ignored
            overridePermissions = overrides
        )

        // Act & Assert
        assertTrue("DM should have READ_MESSAGES by default", permission.hasPermission(RolePermission.READ_MESSAGES, mockRoleRepository))
        assertTrue("DM should have SEND_MESSAGES by default", permission.hasPermission(RolePermission.SEND_MESSAGES, mockRoleRepository))
        assertTrue("DM should have UPLOAD_FILES by default", permission.hasPermission(RolePermission.UPLOAD_FILES, mockRoleRepository))
        assertTrue("DM should have MENTION_MEMBERS by default", permission.hasPermission(RolePermission.MENTION_MEMBERS, mockRoleRepository))
        
        assertFalse("DM should NOT have MANAGE_MESSAGES by default", permission.hasPermission(RolePermission.MANAGE_MESSAGES, mockRoleRepository))
        assertFalse("DM should NOT have MANAGE_ROLES by default", permission.hasPermission(RolePermission.MANAGE_ROLES, mockRoleRepository))
        // ... test other permissions that should be false by default for DMs
    }

    @Test
    fun `hasPermission returns false when projectId exists but roleIds is empty and no override`() = runBlocking {
        // Arrange: Permission for a project channel, but user has no roles
        val permission = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = testProjectId,
            roleIds = emptyList(), // No roles
            overridePermissions = null // No overrides
        )

        // Act & Assert
        assertFalse("Should not have READ without roles/overrides", permission.hasPermission(RolePermission.READ_MESSAGES, mockRoleRepository))
        assertFalse("Should not have SEND without roles/overrides", permission.hasPermission(RolePermission.SEND_MESSAGES, mockRoleRepository))
        assertFalse("Should not have MANAGE without roles/overrides", permission.hasPermission(RolePermission.MANAGE_MESSAGES, mockRoleRepository))
        // ... test other relevant permissions
    }

    @Test
    fun `hasPermission handles repository failure gracefully`() = runBlocking {
        // Arrange: User has two roles, but fetching one fails
        val permission = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = testProjectId,
            roleIds = listOf(testRoleIdMember, "role_fetch_fails"),
            overridePermissions = null
        )

        // Mock repository: Member role grants READ, the other fetch fails
        val memberRole = Role(
            id = testRoleIdMember, 
            name = "Member", 
            permissions = mapOf(RolePermission.READ_MESSAGES to true, RolePermission.SEND_MESSAGES to false)
        )
        `when`(mockRoleRepository.getRole(testProjectId, testRoleIdMember)).thenReturn(Result.success(memberRole))
        `when`(mockRoleRepository.getRole(testProjectId, "role_fetch_fails")).thenReturn(Result.failure(Exception("Firestore error")))

        // Act & Assert
        // Should still have READ permission because the Member role fetch succeeded
        assertTrue("READ should be true because Member role lookup succeeded", permission.hasPermission(RolePermission.READ_MESSAGES, mockRoleRepository))
        // Should NOT have SEND permission because Member doesn't grant it, and the other role fetch failed
        assertFalse("SEND should be false because Member doesn't grant it and other role fetch failed", permission.hasPermission(RolePermission.SEND_MESSAGES, mockRoleRepository))
    }

    @Test
    fun `toFirestoreOverrideMap creates correct map structure`() {
        // Arrange: Permission with some overrides
        val overrides = mapOf(
            RolePermission.READ_MESSAGES to true,
            RolePermission.UPLOAD_FILES to false
        )
        val permission = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = testProjectId,
            roleIds = listOf(testRoleIdMember),
            overridePermissions = overrides
        )

        // Act
        val firestoreMap = permission.toFirestoreOverrideMap()

        // Assert
        // Check that only expected top-level keys exist
        assertEquals("Map should contain userId and permissions (and implicitly updatedAt)", 2, firestoreMap.size) 
        assertEquals("Map should contain correct userId", testUserId, firestoreMap["userId"])
        assertTrue("Map should contain permissions key", firestoreMap.containsKey("permissions"))

        // Check the structure of the nested permissions map
        val permissionsMap = firestoreMap["permissions"] as? Map<*, *> 
        assertNotNull("Permissions map should exist", permissionsMap)
        assertEquals("Permissions map should have correct size", 2, permissionsMap!!.size)
        assertEquals("Permissions map should contain READ_MESSAGES", true, permissionsMap[RolePermission.READ_MESSAGES.name])
        assertEquals("Permissions map should contain UPLOAD_FILES", false, permissionsMap[RolePermission.UPLOAD_FILES.name])

        // Ensure legacy fields are not included
        assertNull("Map should not contain channelId", firestoreMap["channelId"])
        assertNull("Map should not contain projectId", firestoreMap["projectId"])
        assertNull("Map should not contain roleIds", firestoreMap["roleIds"])
        assertNull("Map should not contain role (legacy)", firestoreMap["role"])
        assertNull("Map should not contain customPermissions (legacy)", firestoreMap["customPermissions"])
    }
    
    @Test
    fun `toFirestoreOverrideMap handles null or empty overrides`() {
        // Arrange: Permission with null overrides
        val permissionNullOverrides = ChannelPermission(
            channelId = testChannelId,
            userId = testUserId,
            projectId = testProjectId,
            roleIds = listOf(testRoleIdMember),
            overridePermissions = null
        )
        
         // Arrange: Permission with empty overrides
        val permissionEmptyOverrides = permissionNullOverrides.copy(overridePermissions = emptyMap())

        // Act
        val firestoreMapNull = permissionNullOverrides.toFirestoreOverrideMap()
        val firestoreMapEmpty = permissionEmptyOverrides.toFirestoreOverrideMap()

        // Assert for null overrides
        assertEquals("Map should only contain userId (and implicitly updatedAt) for null overrides", 1, firestoreMapNull.size) 
        assertEquals("Map should contain correct userId for null overrides", testUserId, firestoreMapNull["userId"])
        assertNull("Map should not contain permissions key for null overrides", firestoreMapNull["permissions"])
        
        // Assert for empty overrides
        assertEquals("Map should only contain userId (and implicitly updatedAt) for empty overrides", 1, firestoreMapEmpty.size) 
        assertEquals("Map should contain correct userId for empty overrides", testUserId, firestoreMapEmpty["userId"])
        assertNull("Map should not contain permissions key for empty overrides", firestoreMapEmpty["permissions"])
    }

} 