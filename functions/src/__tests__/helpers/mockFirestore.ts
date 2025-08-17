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
}

// Create shared mock instances that can be reused across tests
const mockDoc = {
  id: "mock-doc",
  get: jest.fn().mockResolvedValue({ exists: false }),
  set: jest.fn().mockResolvedValue(undefined),
  update: jest.fn().mockResolvedValue(undefined),
  delete: jest.fn().mockResolvedValue(undefined),
};

const mockCollection = {
  doc: jest.fn().mockReturnValue(mockDoc),
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
mockCollection.where.mockReturnValue(mockCollection);
mockCollection.limit.mockReturnValue(mockCollection);
mockCollection.orderBy.mockReturnValue(mockCollection);
mockCollection.offset.mockReturnValue(mockCollection);

const mockBatch = {
  set: jest.fn().mockReturnThis(),
  update: jest.fn().mockReturnThis(),
  delete: jest.fn().mockReturnThis(),
  commit: jest.fn().mockResolvedValue(undefined),
};

// Mock Firestore methods
export const mockFirestore = {
  collection: jest.fn().mockReturnValue(mockCollection),
  batch: jest.fn().mockReturnValue(mockBatch),
  settings: jest.fn(),
  FieldValue: {
    serverTimestamp: jest.fn().mockReturnValue({ _type: "serverTimestamp" }),
    delete: jest.fn().mockReturnValue({ _type: "delete" }),
  },
};