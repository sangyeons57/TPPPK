package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

/**
 * ProjectInvitation Repository Factory Context
 * DDD pattern에 따라 collection path를 관리합니다.
 */
class ProjectInvitationRepositoryFactoryContext(
    override val collectionPath: CollectionPath = CollectionPath.projectInvitations()
) : DefaultRepositoryFactoryContext