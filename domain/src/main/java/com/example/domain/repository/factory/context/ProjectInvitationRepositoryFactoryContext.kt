package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath

/**
 * ProjectInvitation Repository Factory Context
 * DDD pattern에 따라 collection path를 관리합니다.
 */
class ProjectInvitationRepositoryFactoryContext(
    override val collectionPath: CollectionPath = CollectionPath.projectInvitations()
) : DefaultRepositoryFactoryContext