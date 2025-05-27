package com.example.data.datasource.remote.projectmember

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.util.DateTimeUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class ProjectMemberRemoteDataSourceImplTest {

    @Mock
    private lateinit var firestore: FirebaseFirestore
    @Mock
    private lateinit var auth: FirebaseAuth
    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var projectsCollectionRef: CollectionReference
    @Mock
    private lateinit var projectDocRef: DocumentReference
    @Mock
    private lateinit var membersCollectionRef: CollectionReference
    @Mock
    private lateinit var memberDocRef: DocumentReference
    @Mock
    private lateinit var usersCollectionRef: CollectionReference
    @Mock
    private lateinit var userDocRef: DocumentReference

    @Mock
    private lateinit var projectSnapshot: DocumentSnapshot
    @Mock
    private lateinit var voidTask: Task<Void>
    @Mock
    private lateinit var docTask: Task<DocumentSnapshot>


    private lateinit var projectMemberRemoteDataSource: ProjectMemberRemoteDataSourceImpl

    private val testProjectId = "testProject1"
    private val testUserId = "testUser1"
    private val testOwnerId = "ownerUser"
    private val testNonOwnerId = "nonOwnerUser"
    private val testRoleIds = listOf("role1")
    private val now = DateTimeUtil.nowInstant()
    private val firebaseTimestamp = DateTimeUtil.instantToFirebaseTimestamp(now)


    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        projectMemberRemoteDataSource = ProjectMemberRemoteDataSourceImpl(firestore, auth)

        // Common mock setups
        `when`(auth.currentUser).thenReturn(mockFirebaseUser)

        `when`(firestore.collection(FirestoreConstants.Collections.PROJECTS)).thenReturn(projectsCollectionRef)
        `when`(projectsCollectionRef.document(testProjectId)).thenReturn(projectDocRef)

        `when`(projectDocRef.collection(FirestoreConstants.Collections.MEMBERS)).thenReturn(membersCollectionRef)
        `when`(membersCollectionRef.document(testUserId)).thenReturn(memberDocRef)
        
        `when`(firestore.collection(FirestoreConstants.Collections.USERS)).thenReturn(usersCollectionRef)
        `when`(usersCollectionRef.document(testUserId)).thenReturn(userDocRef)
        `when`(usersCollectionRef.document(testOwnerId)).thenReturn(userDocRef) // For owner check

        // Mock Task<Void> for set, update, delete operations
        `when`(voidTask.isSuccessful).thenReturn(true)
        `when`(voidTask.isComplete).thenReturn(true)
        // Suppress unchecked cast warning for Task<Void>
        `when`(memberDocRef.set(anyMap<String, Any>())).thenReturn(voidTask as Task<Void>)
        `when`(userDocRef.update(anyString(), any())).thenReturn(voidTask as Task<Void>)
        `when`(projectDocRef.update(anyString(), any())).thenReturn(voidTask as Task<Void>)
        `when`(memberDocRef.delete()).thenReturn(voidTask as Task<Void>)
        
        // Mock Task<DocumentSnapshot> for get operations
        `when`(docTask.isSuccessful).thenReturn(true)
        `when`(docTask.isComplete).thenReturn(true)
        `when`(projectDocRef.get()).thenReturn(docTask as Task<DocumentSnapshot>)
        `when`(docTask.result).thenReturn(projectSnapshot)
    }

    // --- addMemberToProject Tests ---
    @Test
    fun `addMemberToProject success by owner should update project memberIds`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testOwnerId)
        `when`(projectSnapshot.exists()).thenReturn(true)
        `when`(projectSnapshot.getString(FirestoreConstants.ProjectFields.OWNER_ID)).thenReturn(testOwnerId)

        val memberData = hashMapOf(
            FirestoreConstants.MemberFields.ROLE_IDS to testRoleIds,
            FirestoreConstants.MemberFields.JOINED_AT to firebaseTimestamp, // Expected value
            FirestoreConstants.MemberFields.CHANNEL_IDS to emptyList<String>()
        )
        // Use argThat for map comparison if exact timestamp is tricky
        `when`(memberDocRef.set(argThat<Map<String, Any>> { map ->
            map[FirestoreConstants.MemberFields.ROLE_IDS] == testRoleIds &&
            map[FirestoreConstants.MemberFields.CHANNEL_IDS] == emptyList<String>() &&
            map.containsKey(FirestoreConstants.MemberFields.JOINED_AT) // Check presence of JOINED_AT
        })).thenReturn(voidTask)


        // Act
        val result = projectMemberRemoteDataSource.addMemberToProject(testProjectId, testUserId, testRoleIds)

        // Assert
        assertTrue(result.isSuccess)
        verify(memberDocRef).set(argThat<Map<String, Any>> { map ->
            map[FirestoreConstants.MemberFields.ROLE_IDS] == testRoleIds &&
            map.containsKey(FirestoreConstants.MemberFields.JOINED_AT)
        })
        verify(userDocRef).update(FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayUnion(testProjectId))
        verify(projectDocRef).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayUnion(testUserId))
    }

    @Test
    fun `addMemberToProject failure if not owner`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testNonOwnerId) // Current user is not the owner
        `when`(projectSnapshot.exists()).thenReturn(true)
        `when`(projectSnapshot.getString(FirestoreConstants.ProjectFields.OWNER_ID)).thenReturn(testOwnerId)

        // Act
        val result = projectMemberRemoteDataSource.addMemberToProject(testProjectId, testUserId, testRoleIds)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("멤버를 추가할 권한이 없습니다.", result.exceptionOrNull()?.message)
        verify(memberDocRef, never()).set(any())
        verify(projectDocRef, never()).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayUnion(testUserId))
    }
    
    @Test
    fun `addMemberToProject failure if project does not exist`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testOwnerId)
        `when`(projectSnapshot.exists()).thenReturn(false) // Project does not exist

        // Act
        val result = projectMemberRemoteDataSource.addMemberToProject(testProjectId, testUserId, testRoleIds)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("존재하지 않는 프로젝트입니다.", result.exceptionOrNull()?.message)
        verify(memberDocRef, never()).set(any())
        verify(projectDocRef, never()).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayUnion(testUserId))
    }


    // --- removeMemberFromProject Tests ---
    @Test
    fun `removeMemberFromProject success by owner should update project memberIds`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testOwnerId)
        `when`(projectSnapshot.exists()).thenReturn(true)
        `when`(projectSnapshot.getString(FirestoreConstants.ProjectFields.OWNER_ID)).thenReturn(testOwnerId)

        // Act
        val result = projectMemberRemoteDataSource.removeMemberFromProject(testProjectId, testUserId)

        // Assert
        assertTrue(result.isSuccess)
        verify(memberDocRef).delete()
        verify(userDocRef).update(FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayRemove(testProjectId))
        verify(projectDocRef).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayRemove(testUserId))
    }

    @Test
    fun `removeMemberFromProject failure if not owner`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testNonOwnerId) // Current user is not owner
        `when`(projectSnapshot.exists()).thenReturn(true)
        `when`(projectSnapshot.getString(FirestoreConstants.ProjectFields.OWNER_ID)).thenReturn(testOwnerId)

        // Act
        val result = projectMemberRemoteDataSource.removeMemberFromProject(testProjectId, testUserId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("멤버를 제거할 권한이 없습니다.", result.exceptionOrNull()?.message)
        verify(memberDocRef, never()).delete()
        verify(projectDocRef, never()).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayRemove(testUserId))
    }
    
    @Test
    fun `removeMemberFromProject failure if trying to remove owner`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testOwnerId)
        `when`(projectSnapshot.exists()).thenReturn(true)
        `when`(projectSnapshot.getString(FirestoreConstants.ProjectFields.OWNER_ID)).thenReturn(testOwnerId)

        // Act
        val result = projectMemberRemoteDataSource.removeMemberFromProject(testProjectId, testOwnerId) // Trying to remove self as owner

        // Assert
        assertTrue(result.isFailure)
        assertEquals("프로젝트 소유자는 제거할 수 없습니다.", result.exceptionOrNull()?.message)
        verify(memberDocRef, never()).delete()
        verify(projectDocRef, never()).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayRemove(testOwnerId))
    }

    @Test
    fun `removeMemberFromProject failure if project does not exist`() = runTest {
        // Arrange
        `when`(mockFirebaseUser.uid).thenReturn(testOwnerId)
        `when`(projectSnapshot.exists()).thenReturn(false) // Project does not exist

        // Act
        val result = projectMemberRemoteDataSource.removeMemberFromProject(testProjectId, testUserId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("존재하지 않는 프로젝트입니다.", result.exceptionOrNull()?.message)
        verify(memberDocRef, never()).delete()
        verify(projectDocRef, never()).update(FirestoreConstants.ProjectFields.MEMBER_IDS, FieldValue.arrayRemove(testUserId))
    }
    
    // Helper to mock Task<Void> for chained calls like .set().await()
    private fun <T> anyMap(): Map<T, T> = any()
}
