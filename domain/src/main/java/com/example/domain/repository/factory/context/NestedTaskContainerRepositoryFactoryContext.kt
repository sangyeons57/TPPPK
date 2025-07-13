package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

/**
 * 중첩된 TaskContainer Repository를 위한 Factory Context
 * 
 * @param collectionPath 중첩된 TaskContainer collection 경로
 * @param containerPath 중첩 경로 정보 (루트부터 현재 레벨까지의 container ID들)
 */
class NestedTaskContainerRepositoryFactoryContext(
    override val collectionPath: CollectionPath,
    val containerPath: List<String> = emptyList()
) : DefaultRepositoryFactoryContext {
    
    /**
     * 현재 중첩 레벨을 반환합니다.
     * 0 = 루트 레벨, 1 = 1단계 중첩, 2 = 2단계 중첩, ...
     */
    val nestingLevel: Int get() = containerPath.size
    
    /**
     * 부모 container ID를 반환합니다.
     * 루트 레벨인 경우 null을 반환합니다.
     */
    val parentContainerId: String? get() = containerPath.lastOrNull()
    
    /**
     * 루트 레벨인지 확인합니다.
     */
    val isRootLevel: Boolean get() = containerPath.isEmpty()
    
    companion object {
        /**
         * 루트 레벨 TaskContainer용 Factory Context를 생성합니다.
         */
        fun createRoot(projectId: String, channelId: String): NestedTaskContainerRepositoryFactoryContext {
            return NestedTaskContainerRepositoryFactoryContext(
                collectionPath = CollectionPath.projectChannelUnifiedTaskContainer(projectId, channelId),
                containerPath = emptyList()
            )
        }
        
        /**
         * 중첩된 TaskContainer용 Factory Context를 생성합니다.
         */
        fun createNested(
            projectId: String, 
            channelId: String, 
            containerPath: List<String>
        ): NestedTaskContainerRepositoryFactoryContext {
            return NestedTaskContainerRepositoryFactoryContext(
                collectionPath = CollectionPath.nestedTaskContainer(projectId, channelId, containerPath),
                containerPath = containerPath
            )
        }
    }
}