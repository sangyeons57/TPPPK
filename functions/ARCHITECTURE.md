# Firebase Functions - Clean Architecture Structure

## 🏗️ **New Organized Architecture**

This document describes the reorganized Firebase Functions structure following Clean Architecture principles and domain-driven design patterns.

### 📁 **Project Structure**

```
functions/src/
├── 🎯 business/                    # Business Logic Layer
│   ├── auth/
│   │   ├── usecases/              # Authentication use cases
│   │   ├── AuthSessionUseCaseProvider.ts
│   │   └── index.ts
│   ├── friend/
│   │   ├── usecases/              # Friend management use cases
│   │   ├── FriendUseCaseProvider.ts
│   │   └── index.ts
│   ├── user/
│   │   ├── usecases/              # User management use cases
│   │   ├── UserUseCaseProvider.ts
│   │   └── index.ts
│   ├── project/
│   │   ├── usecases/              # Project management use cases
│   │   ├── ProjectUseCaseProvider.ts
│   │   └── index.ts
│   └── index.ts
│
├── 🏗️ domain/                      # Domain Layer
│   ├── shared/
│   │   ├── RepositoryFactory.ts   # Base factory interfaces
│   │   └── index.ts
│   ├── auth/
│   │   ├── entities/
│   │   │   ├── session.entity.ts
│   │   │   └── index.ts
│   │   ├── repositories/
│   │   │   ├── session.repository.ts
│   │   │   ├── factory/
│   │   │   │   ├── SessionRepositoryFactory.ts
│   │   │   │   ├── SessionRepositoryFactoryContext.ts
│   │   │   │   └── index.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── friend/
│   │   ├── entities/
│   │   ├── repositories/
│   │   │   ├── factory/
│   │   └── index.ts
│   ├── user/
│   │   ├── entities/
│   │   ├── repositories/
│   │   │   ├── factory/
│   │   └── index.ts
│   ├── project/
│   │   ├── entities/
│   │   ├── repositories/
│   │   │   ├── factory/
│   │   └── index.ts
│   ├── image/
│   │   ├── entities/
│   │   ├── repositories/
│   │   │   ├── factory/
│   │   ├── services/
│   │   │   ├── imageProcessing.service.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   └── index.ts
│
├── 🗃️ infrastructure/              # Infrastructure Layer
│   ├── datasources/
│   │   ├── firestore/
│   │   │   ├── session.datasource.ts
│   │   │   ├── friend.datasource.ts
│   │   │   ├── userProfile.datasource.ts
│   │   │   ├── project.datasource.ts
│   │   │   ├── image.datasource.ts
│   │   │   └── index.ts
│   │   └── index.ts
│   ├── container/
│   │   ├── ProviderContainer.ts
│   │   └── index.ts
│   └── index.ts
│
├── 🔧 core/                        # Core Utilities
│   ├── constants.ts
│   ├── errors.ts
│   ├── types.ts
│   └── index.ts
│
├── ⚙️ config/                      # Configuration
│   ├── dependencies.ts
│   └── index.ts
│
├── 🌐 triggers/                    # API Layer (Unchanged)
│   ├── auth/
│   ├── friend/
│   ├── user/
│   ├── project/
│   └── system/
│
├── 🧪 test/                        # Testing (To be reorganized)
│   ├── application/
│   ├── domain/
│   └── helpers/
│
└── 📄 index.ts                     # Entry point
```

## 🎯 **Architecture Principles**

### **1. Clean Layer Separation**
- **business/**: Application logic and use case orchestration (merged application + providers)
- **domain/**: Business rules, entities, repository contracts, and factories
- **infrastructure/**: External concerns (databases, containers, services)
- **core/**: Framework-agnostic utilities only
- **triggers/**: API entry points and function handlers

### **2. Domain Co-location**
- Related files grouped together by domain (auth, friend, user, project, image)
- Factories live with repository definitions
- Factory contexts co-located with implementations
- Clear ownership boundaries within each domain

### **3. Provider Pattern Benefits**
- Encapsulates repository creation logic
- Provides clean use case groupings
- Enables easy testing with mock providers
- Maintains dependency injection principles

## 🔄 **Key Improvements**

### **Before Reorganization:**
- Scattered factory implementations in `core/factory/`
- Separate `application/` and `providers/` doing similar work
- Poor domain organization
- Infrastructure mixed with core utilities

### **After Reorganization:**
- Co-located domain files (entities, repositories, factories)
- Merged business logic layer (use cases + providers)
- Clean infrastructure separation
- Comprehensive index files for easy imports

## 📦 **Import Patterns**

### **Business Layer Usage:**
```typescript
// Clean domain-specific imports
import { RepositoryFactory } from '../../domain/shared/RepositoryFactory';
import { SessionRepositoryFactoryContext } from '../../domain/auth/repositories/factory/SessionRepositoryFactoryContext';
import { LoginUserUseCase } from './usecases/loginUser.usecase';
```

### **Trigger Layer Usage:**
```typescript
// Simplified provider usage
import { Providers } from '../../config/dependencies';

const authUseCases = Providers.getAuthSessionProvider().create();
const result = await authUseCases.loginUserUseCase.execute(request);
```

## 🏭 **Factory Pattern Evolution**

### **Domain-Specific Factories:**
- Each domain has its own factory in `domain/{domain}/repositories/factory/`
- Factory contexts are co-located with factory implementations
- Shared factory interfaces in `domain/shared/`

### **Provider Integration:**
- Providers use domain-specific factories
- Clean dependency injection through ProviderContainer
- Type-safe factory creation with contexts

## 🎪 **Benefits Achieved**

1. **Domain-Driven Organization**: Related files grouped by business domain
2. **Reduced Coupling**: Clear separation between layers
3. **Improved Maintainability**: Easier to find and modify related code
4. **Better Testing**: Domain-specific mock helpers and test organization
5. **Type Safety**: Comprehensive TypeScript interfaces and exports
6. **Clean Imports**: Intuitive import paths following architectural boundaries

## 🚀 **Usage Examples**

### **Creating Use Cases:**
```typescript
// In business layer providers
const authProvider = new AuthSessionUseCaseProvider(
  sessionRepositoryFactory,
  userRepositoryFactory
);

const authUseCases = authProvider.create();
```

### **Using in Triggers:**
```typescript
// In trigger functions
const authUseCases = Providers.getAuthSessionProvider().create();
const result = await authUseCases.loginUserUseCase.execute(loginRequest);
```

This architecture now mirrors the successful Android client structure while being optimized for TypeScript/Firebase Functions, providing a solid foundation for scalable serverless development.