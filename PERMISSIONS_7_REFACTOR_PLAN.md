# 7-Permission Model Refactor Plan

## Overview

This document defines a simplified, product-aligned permission model with exactly 7 permissions and
outlines the changes, side effects, and migration plan to adopt it across the Android app (
Domain/Data/UseCase/Feature) and Firebase (rules/functions where applicable).

## New Permission Set (7)

IDs are stable enum names used in code and Firestore. Display names are for UI.

1) ROLE_EDIT: 역할 수정 권한 (역할에 할당된 권한 수정)
2) MEMBER_INVITE: 멤버 초대 권한
3) MEMBER_MANAGE: 멤버 관리 권한 (내보내기, 밴, 멤버에게 역할 할당)
4) STRUCTURE_EDIT: 구조 편집 권한 (프로젝트 채널/카테고리 순서 수정, 이름 수정 등)
5) PROJECT_SETTINGS: 프로젝트 설정 권한 (이름, 프로필 이미지)
6) CHANNEL_WRITE: 채널 쓰기 권한 (채팅 + 테스크/할일 수정)
7) CHANNEL_READ: 채널 읽기 권한

Notes

- CHANNEL_READ is the baseline for accessing project channels; CHANNEL_WRITE implies CHANNEL_READ in
  UX, but the model keeps them separate for clarity and future rules.
- ADMINISTRATOR or other super-sets are removed; if needed later, map to all-true for the 7 flags at
  the application layer.

## Mapping From Current Model

Current enums:

- Domain storage: `RolePermission` (domain/model/data/project/RolePermission.kt) — many granular
  values
- UI display: `PermissionType` (domain/vo/permission/PermissionType.kt) — similar breadth with
  metadata

High-level mapping (old → new):

- MANAGE_ROLES, CREATE_ROLES, EDIT_ROLES, DELETE_ROLES → ROLE_EDIT
- INVITE_MEMBERS → MEMBER_INVITE
- MANAGE_MEMBERS, REMOVE_MEMBERS, EDIT_MEMBER_ROLES → MEMBER_MANAGE
- MANAGE_CHANNELS, CREATE_CHANNELS, EDIT_CHANNELS, DELETE_CHANNELS, MANAGE_CATEGORIES,
  CREATE_CATEGORIES, EDIT_CATEGORIES, DELETE_CATEGORIES → STRUCTURE_EDIT
- MANAGE_PROJECT, EDIT_PROJECT_DETAILS, DELETE_PROJECT → PROJECT_SETTINGS (delete project can be
  gated separately by owner if needed)
- SEND_MESSAGES, MANAGE_TASKS, CREATE_TASKS, ASSIGN_TASKS → CHANNEL_WRITE
- READ (no explicit old entry) → CHANNEL_READ (baseline for viewing channels/messages)
- FILE and MEETING specific permissions → fold into CHANNEL_WRITE for now (subject to later split if
  product demands)
- ADMINISTRATOR → all 7 set to true (application-only behavior; enum removed)

## Target Model Changes

1) Replace/reshape enums

- Collapse `RolePermission` to the 7 values above; remove others.
- Collapse `PermissionType` to the same 7 values and keep UI metadata (display name, description,
  category → simplified categories such as ROLE, MEMBER, STRUCTURE, PROJECT, CHANNEL).

2) Persistence model

- Firestore subcollection remains: `/projects/{projectId}/roles/{roleId}/permissions/{permissionId}`
  with `permissionId` in the 7-ID set.
- Existing permission docs with old IDs become obsolete; migration maps them into the 7.

3) Use cases

- `GetRolePermissionsUseCase`: returns `List<RolePermission>` of the 7 only.
- `GetUserPermissionsForProjectUseCase`: aggregates the 7 only; provide
  `hasPermission(projectId, userId, RolePermission)` helper as-is.
- Add `SetRolePermissionsUseCase` (create/update) to write the 7 flags for a role.

4) UI (Feature)

- `feature_add_role`, `feature_edit_role`: replace switches with the 7 toggles from
  `PermissionType`.
- `EditRoleViewModel`: load/save permission toggles via new use case(s); remove TODOs.

5) Firebase

- Functions: current checks rely on roles (OWNER/ADMIN). Unaffected short-term; long-term, consider
  aligning Functions to read these 7 permissions for finer control.
- Firestore rules: optional enhancement later to enforce these permissions (currently using
  owner/member checks). Plan after app-side refactor is stable.

## Impacted Files (Non-exhaustive)

Domain

- Update: `domain/.../model/data/project/RolePermission.kt`
- Update: `domain/.../vo/permission/PermissionType.kt` (reduce + metadata)
- Check: `domain/.../model/base/Permission.kt` (ID → new enums)
- Check: Events referencing specific permissions (likely no change needed)

Domain Repository Interfaces

- Keep: `ProjectRoleRepository.getRolePermissions(projectId, roleId)`
- Keep: `PermissionRepository` (subcollection access)

Data

- Implement: `ProjectRoleRepositoryImpl.getRolePermissions()` (currently TODO)
- Check: `PermissionRepositoryImpl` (no breaking changes; ensure mapping fragile to enum names)
- DataSource: `PermissionRemoteDataSource` unchanged; ensure correct collection path setup with
  roleId
- Mapper: `PermissionMapper` uses `RolePermission.from(dto.id)` — update to new enum set

UseCase Layer

- Update consumers of `RolePermission` to handle only the 7
- Add: `SetRolePermissionsUseCase`
- Provider: `ProjectRoleUseCaseProvider` — add factory that sets `permissionRepository` collection
  with the specific `roleId` when fetching/updating permissions

Feature Layer

- `feature_add_role`: simplify permission UI to 7 toggles
- `feature_edit_role`: load current 7 flags, allow edit/save

Rules/Functions

- No immediate breaking change; document future enhancement to reflect the 7-permission checks in
  Functions/Rules

Tests

- Update/replace permission-related tests to the 7-permission set

## Backward Compatibility and Migration

Strategy: phased, non-breaking where possible.

Phase 0 — Introduce 7-permission enums alongside old

- Add new `RolePermission` values and keep old temporarily with mapping table (old→new)
- Update UI to show only the 7 via `PermissionType`
- `GetRolePermissionsUseCase` maps old stored IDs to new 7 using mapping; unknowns ignored/logged

Phase 1 — Write-path uses only the 7

- Add `SetRolePermissionsUseCase` and update edit/create flows to write only new IDs
- Ensure `ProjectRoleRepositoryImpl.getRolePermissions()` implemented and reads per-role permissions

Phase 2 — Data migration (optional but recommended)

- One-off script/Function to convert existing role permission docs to the 7 IDs
- Remove obsolete docs

Phase 3 — Remove legacy

- Remove old granular enum values, mapping code, and UI branches
- Update tests to only reference the 7-permission model

## Side Effects and Risks

- Existing permissions in Firestore with old IDs will no longer be recognized once legacy mapping is
  removed → plan data migration before cleanup.
- UI assumptions about granular permissions disappear; features previously gated by fine-grained
  flags must align to the 7 categories (e.g., file/meeting actions align under CHANNEL_WRITE).
- `ADMINISTRATOR` removal: if any code depends on it, replace by all-true 7-permission behavior at
  call sites.
- Firestore rules currently don’t enforce the 7; app enforces via use cases and UI. Consider
  updating rules later to improve security-in-depth.
- Functions still check OWNER/ADMIN; ensure product alignment (OWNER implies all 7, ADMIN behavior
  defined by product; potentially map ADMIN to a subset/all of the 7 later).

## Implementation Plan (Actionable)

1) Enums and Mapping

- Replace `RolePermission` with 7 IDs; add a temporary mapping object `LegacyPermissionMapper` (
  old→new)
- Reduce `PermissionType` to the same 7 with display metadata

2) Repository/UseCase

- Implement `ProjectRoleRepositoryImpl.getRolePermissions()`
- Add `SetRolePermissionsUseCase` for per-role upsert of the 7
- Update `ProjectRoleUseCaseProvider` to expose permission get/set with proper collection scoping by
  roleId
- Update `GetUserPermissionsForProjectUseCase` to aggregate only these 7

3) Feature UI

- `feature_add_role`/`feature_edit_role` to use the 7 toggles from `PermissionType`
- Remove TODOs, wire load/save

4) Data Migration (Optional Step)

- Provide a small tool or CF script to translate existing permission docs to the 7 new IDs

5) Cleanup

- Remove legacy granular enums and mapping after migration
- Update tests

## Open Questions

- Should `CHANNEL_WRITE` include task creation/edit universally, or require separate toggles in the
  future?
- Is project deletion restricted to OWNER regardless of `PROJECT_SETTINGS`? (Recommended: keep
  deletion OWNER-only)
- Define ADMIN semantics across client and Functions (map to all 7 or subset?)

