# Chat Memory Management System

## Overview

This document describes the advanced memory management system implemented for chat messages with bidirectional loading capabilities.

## Key Features

### 1. Memory Management Configuration (`ChatMemoryConfig`)
- **MAX_MESSAGES_IN_MEMORY**: 200 - Maximum messages kept in memory
- **PAGINATION_SIZE**: 50 - Messages loaded per request
- **MEMORY_CLEANUP_THRESHOLD**: 150 - When cleanup starts
- **MESSAGES_TO_REMOVE_ON_CLEANUP**: 50 - Messages removed during cleanup

### 2. Bidirectional Loading (`MessageMemoryManager`)
- **Load Older Messages**: Fetches messages older than current oldest
- **Load Newer Messages**: Fetches messages newer than current newest
- **Dynamic Memory Management**: Automatically removes messages when limits exceeded
- **Smart Cleanup**: Removes from opposite end when loading in one direction

### 3. Enhanced UseCase Support
- **FetchPastMessagesUseCase**: Enhanced with `beforeTimestamp` parameter
- **FetchNewerMessagesUseCase**: New UseCase for loading newer messages
- **Timestamp-based Filtering**: Client-side filtering for better pagination

## Architecture

```
ViewModel
    ↓
MessageService (with MessageMemoryManager)
    ↓
ChatUseCases (FetchPastMessages + FetchNewerMessages)
    ↓
MessageRepository
    ↓
Firestore
```

## Usage Examples

### Loading Older Messages
```kotlin
// ViewModel
fun loadOlderMessages() {
    val result = messageService.loadOlderMessages(currentUserId)
    // Handle result with memory management
}
```

### Loading Newer Messages
```kotlin
// ViewModel  
fun loadNewerMessages() {
    val result = messageService.loadNewerMessages(currentUserId)
    // Handle result with memory management
}
```

### Memory Status
```kotlin
// Check memory info
val memoryInfo = messageService.getMemoryInfo()
Log.d("Memory", "Messages: ${memoryInfo.currentMessageCount}/${memoryInfo.maxCapacity}")
Log.d("Memory", "Can load older: ${memoryInfo.canLoadOlder}")
Log.d("Memory", "Can load newer: ${memoryInfo.canLoadNewer}")
```

## Benefits

1. **Memory Efficiency**: Never exceeds configured memory limits
2. **Seamless UX**: Users can browse entire chat history
3. **Performance**: Maintains smooth scrolling with limited memory usage
4. **Bidirectional**: Supports loading both older and newer messages
5. **Smart Cleanup**: Automatically removes messages from opposite end

## Implementation Details

### Memory Management Flow

1. **Initial Load**: Load first 50 messages and set in memory manager
2. **Load Older**: 
   - Fetch messages older than current oldest timestamp
   - Add to memory manager
   - If memory limit exceeded, remove newest messages
3. **Load Newer**: 
   - Fetch messages newer than current newest timestamp
   - Add to memory manager  
   - If memory limit exceeded, remove oldest messages
4. **Real-time Messages**: Added as newest, trigger cleanup if needed

### Error Handling

- Firebase Storage 404 errors are gracefully handled for profile images
- Memory cleanup notifies users when messages are removed
- Loading states prevent concurrent operations
- Fallback user profiles for failed profile loads

## Future Enhancements

1. **Server-side Pagination**: Implement Firestore cursor-based pagination
2. **Intelligent Prefetching**: Preload messages based on scroll direction
3. **Message Persistence**: Cache recently accessed messages to disk
4. **Configurable Limits**: Allow per-user memory configuration