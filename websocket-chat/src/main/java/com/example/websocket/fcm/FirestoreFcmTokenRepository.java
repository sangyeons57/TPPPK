package com.example.websocket.fcm;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
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
        // New schema: single field at /users/{uid} with key "fcmToken"
        DocumentReference userDoc = db.collection("users").document(userId);
        DocumentSnapshot snap = userDoc.get().get();
        List<String> tokens = new ArrayList<>(1);
        if (snap.exists()) {
            String token = snap.getString("fcmToken");
            if (token != null && !token.isBlank()) tokens.add(token);
        }
        return tokens;
    }

    @Override
    public void removeInvalidTokens(String userId, List<String> invalidTokens) throws ExecutionException, InterruptedException {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;
        DocumentReference userDoc = db.collection("users").document(userId);
        DocumentSnapshot snap = userDoc.get().get();
        if (!snap.exists()) return;
        String current = snap.getString("fcmToken");
        if (current != null && invalidTokens.contains(current)) {
            ApiFuture<WriteResult> future = userDoc.update("fcmToken", null);
            future.get();
        }
    }
}
