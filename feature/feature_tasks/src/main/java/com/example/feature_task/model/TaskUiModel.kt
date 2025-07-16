import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskType
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder

/**
 * Task.kt domain 모델을 ui에서 사용할있도록 모델을 만듦
 */

data class TaskUiModel(
    val id: DocumentId,
    val taskType: TaskType,
    val status: TaskStatus,
    val content: TaskContent,
    val order: TaskOrder,
)