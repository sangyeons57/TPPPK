package com.example.websocket.fcm;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed token repository. Assumes tokens are stored under
 * users/{uid}/fcm_tokens/{tokenId}.
 */
public final class FirestoreFcmTokenRepository implements FcmTokenRepository {

    private final Firestore db;

    public FirestoreFcmTokenRepository() {
        this.db = FirestoreClient.getFirestore();
    }

    @Override
    public List<String> getTokensForUser(String userId) throws ExecutionException, InterruptedException {
        CollectionReference col = db.collection("users").document(userId).collection("fcm_tokens");
        List<QueryDocumentSnapshot> docs = col.get().get().getDocuments();
        List<String> tokens = new ArrayList<>(docs.size());
        for (QueryDocumentSnapshot d : docs) {
            tokens.add(d.getId()); // document id is the token
        }
        return tokens;
    }

    @Override
    public void removeInvalidTokens(String userId, List<String> invalidTokens) throws ExecutionException, InterruptedException {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;
        WriteBatch batch = db.batch();
        for (String token : invalidTokens) {
            DocumentReference ref = db.collection("users").document(userId).collection("fcm_tokens").document(token);
            batch.delete(ref);
        }
        ApiFuture<List<WriteResult>> future = batch.commit();
        future.get();
    }
}

