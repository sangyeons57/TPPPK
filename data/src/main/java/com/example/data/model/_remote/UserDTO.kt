import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class UserDTO(
    @DocumentId val uid: String = "",
    val email: String = "",
    val name: String = "",
    val consentTimeStamp: Timestamp? = null,
    val profileImageUrl: String? = null,
    val memo: String? = null,
    val status: String = "offline", // "online", "offline", "away" 등
    val createdAt: Timestamp = Timestamp.now(),
    val fcmToken: String? = null,
    val accountStatus: String = "active" // "active", "suspended", "deleted" 등
)