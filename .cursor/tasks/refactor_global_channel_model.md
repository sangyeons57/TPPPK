# Task: Refactor to a Global Channel Model

- [x] Step 1: Update Firestore Schema (`.cursor/rules/firestore-schema.mdc`):
    *   Clearly define the global `/channels` collection as the single source of truth for all channel documents.
    *   Update the `metadata` field description in the global `/channels` to include examples for DM, project-direct, and project-category channels, showing how `source`, `projectId`, `categoryId`, `participantIds`, etc., differentiate them.
    *   Modify `projects/{projectId}/categories/{categoryId}/channels/` to store *references* to channel IDs (e.g., a subcollection of documents where each document ID is a channel ID and the document contains an `order` field, or an array of channel IDs within the category document if order is managed differently).
    *   Modify `projects/{projectId}/channels/` (for project-direct channels) similarly to store *references* (e.g., subcollection `projectChannelReferences/{channelId}` with an `order` field).
    *   Update `users/{userId}/activeDmIds` (or a similar structure like `users/{userId}/dmChannelIds`) to confirm it stores references to DM channel IDs in the global `/channels` collection.
    *   Remove redundant full channel definitions from under project subcollections in the schema, instead referring to the global definition and noting any specific reference structure or key metadata.
- [x] Step 2: Refactor `ChannelRepository` and `ChannelRepositoryImpl`
- [x] Step 3: Refactor `ProjectStructureRemoteDataSource` and `ProjectStructureRemoteDataSourceImpl`
- [x] Step 4: Refactor `DmRepository` and `DmRepositoryImpl`
- [x] Step 5: Update Navigation (`AppRoutes.kt`): Review and confirm compatibility. Current structure is adequate.
- [x] Step 6: Refactor `ChatViewModel` (or equivalent): Injected `ChannelRepository`, fetches channel details, uses updated `ChannelLocator` for message paths. WebSocket logic still pending. Key TODO: Implement `myUserId` retrieval.
- [x] Step 7: Update UI Components (`DevMenuScreen.kt`, etc.): Reviewed `DevMenuScreen.kt`; its chat navigation calls are correct and compatible. Other UI components should follow suit.
- [ ] Step 8: Review and Update Domain Models and Use Cases (e.g., message use cases to accept `channelId` directly).