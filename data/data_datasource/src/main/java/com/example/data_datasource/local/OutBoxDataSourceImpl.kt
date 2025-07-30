package com.example.data_datasource.local

import com.example.core_common.result.CustomResult
import com.example.data_converter.JsonConverter
import com.example.data_datasource.dao.OutBoxDao
import com.example.data_model.local.OutBoxEntity
import com.example.domain.enum.EntityType
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.base.DMChannel
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Friend
import com.example.domain.model.base.Member
import com.example.domain.model.base.Message
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.base.Permission
import com.example.domain.model.base.Project
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.base.Reaction
import com.example.domain.model.base.Role
import com.example.domain.model.base.Schedule
import com.example.domain.model.base.Task
import com.example.domain.model.base.User
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import com.example.mapper.sync.OutBoxMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OutBox 로컬 데이터 소스 구현체
 * OutBoxDao를 래핑하고 Entity ↔ Domain Model 변환 처리
 */
@Singleton
class OutBoxDataSourceImpl @Inject constructor(
    private val outBoxDao: OutBoxDao,
    private val outBoxMapper: OutBoxMapper,
    private val jsonConverterMap: Map<Class<*>, @JvmSuppressWildcards JsonConverter<*>>
) : OutBoxDataSource {

    /**
     * JsonConverter를 타입 안전하게 가져오는 헬퍼 함수
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> getJsonConverter(clazz: Class<T>): JsonConverter<T> where T : AggregateRoot {
        return jsonConverterMap[clazz] as? JsonConverter<T>
            ?: throw IllegalArgumentException("No JsonConverter found for type: ${clazz.simpleName}")
    }

    /**
     * OutBox<*>를 OutBoxEntity로 변환하는 헬퍼 함수
     */
    @Suppress("UNCHECKED_CAST")
    private fun outBoxToEntity(outBox: OutBox<*>): OutBoxEntity {
        val entityType = outBox.entityType
        val domainClass = when (entityType) {
            EntityType.MESSAGE -> Message::class.java
            EntityType.USER -> User::class.java
            EntityType.PROJECT -> Project::class.java
            EntityType.MESSAGE_ATTACHMENT -> MessageAttachment::class.java
            EntityType.REACTION -> Reaction::class.java
            EntityType.FRIEND -> Friend::class.java
            EntityType.PROJECT_CHANNEL -> ProjectChannel::class.java
            EntityType.PROJECT_INVITATION -> ProjectInvitation::class.java
            EntityType.PROJECTS_WRAPPER -> ProjectsWrapper::class.java
            EntityType.CATEGORY -> Category::class.java
            EntityType.MEMBER -> Member::class.java
            EntityType.ROLE -> Role::class.java
            EntityType.PERMISSION -> Permission::class.java
            EntityType.DM_CHANNEL -> DMChannel::class.java
            EntityType.DM_WRAPPER -> DMWrapper::class.java
            EntityType.TASK -> Task::class.java
            EntityType.SCHEDULE -> Schedule::class.java
        }

        val converter = getJsonConverter(domainClass as Class<AggregateRoot>)
        val payloadJson = converter.toJson(
            outBox.payload?.data as? AggregateRoot
                ?: throw IllegalArgumentException("OutBox payload is null or not AggregateRoot")
        )
        return outBoxMapper.domainToEntity(outBox as OutBox<AggregateRoot>, payloadJson)
    }

    /**
     * EntityType에 따라 적절한 OutBox<*>로 변환 (타입 정보 없이)
     */
    private fun entityToDomainByType(entity: OutBoxEntity): OutBox<*> {
        val entityType = EntityType.valueOf(entity.entityType)

        return when (entityType) {
            EntityType.MESSAGE -> entityToOutBox<Message>(entity)
            EntityType.USER -> entityToOutBox<User>(entity)
            EntityType.PROJECT -> entityToOutBox<Project>(entity)
            EntityType.MESSAGE_ATTACHMENT -> entityToOutBox<MessageAttachment>(entity)
            EntityType.REACTION -> entityToOutBox<Reaction>(entity)
            EntityType.FRIEND -> entityToOutBox<Friend>(entity)
            EntityType.PROJECT_CHANNEL -> entityToOutBox<ProjectChannel>(entity)
            EntityType.PROJECT_INVITATION -> entityToOutBox<ProjectInvitation>(entity)
            EntityType.PROJECTS_WRAPPER -> entityToOutBox<ProjectsWrapper>(entity)
            EntityType.CATEGORY -> entityToOutBox<Category>(entity)
            EntityType.MEMBER -> entityToOutBox<Member>(entity)
            EntityType.ROLE -> entityToOutBox<Role>(entity)
            EntityType.PERMISSION -> entityToOutBox<Permission>(entity)
            EntityType.DM_CHANNEL -> entityToOutBox<DMChannel>(entity)
            EntityType.DM_WRAPPER -> entityToOutBox<DMWrapper>(entity)
            EntityType.TASK -> entityToOutBox<Task>(entity)
            EntityType.SCHEDULE -> entityToOutBox<Schedule>(entity)
        }
    }

    /**
     * OutBoxEntity를 제네릭 OutBox로 변환
     */
    private inline fun <reified T> entityToOutBox(entity: OutBoxEntity): OutBox<T> where T : AggregateRoot {
        val converter = getJsonConverter(T::class.java)
        val domainObject = converter.fromJson(entity.payloadJson)
        return outBoxMapper.entityToDomain(entity, domainObject)
    }


    // ================================
    // Repository용 - 트랜잭션 내 OutBox 추가
    // ================================

    override suspend fun insert(outBox: OutBox<*>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = outBoxToEntity(outBox)
                outBoxDao.insert(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun insertAll(outBoxes: List<OutBox<*>>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxes.map { outBoxToEntity(it) }
                outBoxDao.insertAll(entities)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 도메인 모델 처리
    // ================================

    override suspend fun getById(id: String): CustomResult<OutBox<*>?, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = outBoxDao.getById(id)
                val domainModel = entity?.let { entityToDomainByType(it) }
                CustomResult.Success(domainModel)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun getByStatus(status: OutBoxStatus): CustomResult<List<OutBox<*>>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getByStatus(status.name)
                val domainModels = entities.map { entityToDomainByType(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun getByStatuses(statuses: List<OutBoxStatus>): CustomResult<List<OutBox<*>>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val statusNames = statuses.map { it.name }
                val entities = outBoxDao.getByStatuses(statusNames)
                val domainModels = entities.map { entityToDomainByType(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun getByEntityTypeAndId(
        entityType: String,
        entityId: String
    ): CustomResult<List<OutBox<*>>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getByEntityTypeAndId(entityType, entityId)
                val domainModels = entities.map { entityToDomainByType(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 배치 처리
    // ================================

    override suspend fun getPendingOperations(limit: Int): CustomResult<List<OutBox<*>>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getPendingOperations(limit)
                val domainModels = entities.map { entityToDomainByType(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun update(outBox: OutBox<*>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = outBoxToEntity(outBox)
                outBoxDao.update(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun updateStatusByIds(ids: List<String>, newStatus: OutBoxStatus): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                if (ids.isEmpty()) {
                    return@withContext CustomResult.Success(0)
                }
                val updatedCount = outBoxDao.updateStatusByIds(ids, newStatus.name)
                CustomResult.Success(updatedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun resetRetryableFailedOperations(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val resetCount = outBoxDao.resetRetryableFailedOperations()
                CustomResult.Success(resetCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 정리 및 관리
    // ================================
    
    override suspend fun deleteCompleted(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = outBoxDao.deleteCompleted()
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun delete(outBox: OutBox<*>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = outBoxToEntity(outBox)
                outBoxDao.delete(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteExpiredOperations(timeoutMs: Long): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val currentTimeMs = System.currentTimeMillis()
                val deletedCount = outBoxDao.deleteExpiredOperations(currentTimeMs, timeoutMs)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 통계 및 모니터링
    // ================================
    
    override suspend fun getCountByStatus(status: OutBoxStatus): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = outBoxDao.getCountByStatus(status.name)
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getTotalCount(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = outBoxDao.getTotalCount()
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getStatusStatistics(): CustomResult<Map<OutBoxStatus, Int>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val statistics = outBoxDao.getStatusStatistics()
                val statusMap = statistics.associate { stat ->
                    OutBoxStatus.valueOf(stat.status) to stat.count
                }
                CustomResult.Success(statusMap)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // 테스트 및 디버깅용
    // ================================

    override suspend fun getAllForDebug(): CustomResult<List<OutBox<*>>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = outBoxDao.getAllForDebug()
                val domainModels = entities.map { entityToDomainByType(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                outBoxDao.deleteAll()
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
}