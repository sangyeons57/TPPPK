export class MockFirestoreHelper {
  private static collections: Map<string, Map<string, any>> = new Map();

  static reset(): void {
    this.collections.clear();
  }

  static mockCollection(collectionName: string, documents: Record<string, any>): void {
    const collection = new Map();
    Object.entries(documents).forEach(([id, data]) => {
      collection.set(id, {
        id,
        data: () => data,
        exists: true,
        ref: {
          id,
          update: jest.fn().mockResolvedValue(undefined),
          set: jest.fn().mockResolvedValue(undefined),
          delete: jest.fn().mockResolvedValue(undefined),
        },
      });
    });
    this.collections.set(collectionName, collection);
  }

  static getDocument(collection: string, id: string): any {
    return this.collections.get(collection)?.get(id) || {
      id,
      exists: false,
      data: () => undefined,
    };
  }

  static addDocument(collection: string, id: string, data: any): void {
    if (!this.collections.has(collection)) {
      this.collections.set(collection, new Map());
    }
    this.collections.get(collection)!.set(id, {
      id,
      data: () => data,
      exists: true,
      ref: {
        id,
        update: jest.fn().mockResolvedValue(undefined),
        set: jest.fn().mockResolvedValue(undefined),
        delete: jest.fn().mockResolvedValue(undefined),
      },
    });
  }

  static mockQuery(collection: string, filter: any): any {
    const docs = Array.from(this.collections.get(collection)?.values() || []);
    return {
      empty: docs.length === 0,
      docs,
      get: jest.fn().mockResolvedValue({
        empty: docs.length === 0,
        docs,
      }),
    };
  }

  // Helper for Subcollection bilateral friend relationships
  static mockBilateralFriendship(userId1: string, userId2: string, status: string, userData: any = {}): void {
    // Mock user1's subcollection: users/{userId1}/friends/{userId2}
    const user1FriendsCollection = `users/${userId1}/friends`;
    this.addDocument(user1FriendsCollection, userId2, {
      name: userData.user2Name || "User 2",
      profileImageUrl: userData.user2Profile || null,
      status,
      requestedAt: new Date(),
      acceptedAt: status === "ACCEPTED" ? new Date() : null,
      createdAt: new Date(),
      updatedAt: new Date(),
      ...userData.user2Data,
    });

    // Mock user2's subcollection: users/{userId2}/friends/{userId1}  
    const user2FriendsCollection = `users/${userId2}/friends`;
    const correspondingStatus = status === "REQUESTED" ? "PENDING" : status === "PENDING" ? "REQUESTED" : status;
    this.addDocument(user2FriendsCollection, userId1, {
      name: userData.user1Name || "User 1", 
      profileImageUrl: userData.user1Profile || null,
      status: correspondingStatus,
      requestedAt: new Date(),
      acceptedAt: status === "ACCEPTED" ? new Date() : null,
      createdAt: new Date(),
      updatedAt: new Date(),
      ...userData.user1Data,
    });
  }

  // Helper for querying Subcollection with filters
  static mockSubcollectionQuery(collectionPath: string, filters: Array<{field: string, operator: string, value: any}>): any {
    const docs = Array.from(this.collections.get(collectionPath)?.values() || []);
    
    // Simple filter implementation for testing
    const filteredDocs = docs.filter(doc => {
      return filters.every(filter => {
        const data = doc.data();
        switch (filter.operator) {
          case "==":
            return data[filter.field] === filter.value;
          case "!=":
            return data[filter.field] !== filter.value;
          default:
            return true;
        }
      });
    });

    return {
      empty: filteredDocs.length === 0,
      docs: filteredDocs,
      get: jest.fn().mockResolvedValue({
        empty: filteredDocs.length === 0,
        docs: filteredDocs,
      }),
    };
  }

  // Support querying collection group by document ID across any subcollection path suffix
  static queryCollectionGroupByDocId(collectionSuffix: string, docId: string): any[] {
    const results: any[] = [];
    for (const [collectionPath, docs] of this.collections.entries()) {
      if (collectionPath.endsWith(collectionSuffix)) {
        const doc = docs.get(docId);
        if (doc) {
          results.push(doc);
        }
      }
    }
    return results;
  }
}

// Enhanced mock instances with better Subcollection support
const createMockDoc = (docId: string = "mock-doc") => ({
  id: docId,
  get: jest.fn().mockImplementation(() => {
    // Check if document exists in MockFirestoreHelper
    const exists = Math.random() > 0.5; // Default behavior, can be overridden
    return Promise.resolve({
      exists,
      id: docId,
      data: () => exists ? { mockData: true } : undefined,
    });
  }),
  set: jest.fn().mockResolvedValue(undefined),
  update: jest.fn().mockResolvedValue(undefined),
  delete: jest.fn().mockResolvedValue(undefined),
});

const createMockCollection = (collectionPath: string = "mock-collection") => {
  const collection = {
    doc: jest.fn().mockImplementation((docId?: string) => {
      const id = docId || `mock-doc-${Date.now()}-${Math.random()}`;
      return createMockDoc(id);
    }),
    add: jest.fn().mockImplementation((data: any) => {
      const docId = `mock-doc-${Date.now()}-${Math.random()}`;
      return Promise.resolve({
        id: docId,
        get: jest.fn().mockResolvedValue({ id: docId, data: () => data, exists: true }),
      });
    }),
    where: jest.fn().mockReturnThis(),
    limit: jest.fn().mockReturnThis(),
    orderBy: jest.fn().mockReturnThis(),
    offset: jest.fn().mockReturnThis(),
    get: jest.fn().mockResolvedValue({ empty: true, docs: [] }),
  };

  // Ensure chainable methods return the collection itself
  collection.where.mockReturnValue(collection);
  collection.limit.mockReturnValue(collection);
  collection.orderBy.mockReturnValue(collection);
  collection.offset.mockReturnValue(collection);

  return collection;
};

export const mockBatch = {
  set: jest.fn().mockReturnThis(),
  update: jest.fn().mockReturnThis(),
  delete: jest.fn().mockReturnThis(),
  commit: jest.fn().mockResolvedValue(undefined),
};

// Enhanced Mock Firestore with Subcollection support
export const mockFirestore = {
  collection: jest.fn().mockImplementation((collectionPath: string) => {
    return createMockCollection(collectionPath);
  }),
  // Minimal collectionGroup support for tests using documentId equality filter
  collectionGroup: jest.fn().mockImplementation((collectionId: string) => {
    let targetDocId: string | undefined;
    const group: any = {
      where: jest.fn().mockImplementation((_field: any, op: string, value: any) => {
        if (op === "==") {
          targetDocId = value;
        }
        return group; // chainable
      }),
      get: jest.fn().mockImplementation(async () => {
        const docs = targetDocId
          ? MockFirestoreHelper.queryCollectionGroupByDocId(collectionId, targetDocId)
          : [];
        return {
          empty: docs.length === 0,
          docs,
        };
      }),
    };
    return group;
  }),
  batch: jest.fn().mockReturnValue(mockBatch),
  settings: jest.fn(),
  FieldValue: {
    serverTimestamp: jest.fn().mockReturnValue({ _type: "serverTimestamp" }),
    delete: jest.fn().mockReturnValue({ _type: "delete" }),
  },
};
