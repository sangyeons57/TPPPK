package com.example.websocket.service;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight Firestore-backed service (no DI) for resolving project members.
 *
 * Collection shape assumptions (best-effort, tolerant of variations):
 * - projects/{projectId}/members/{docId}
 *   - userId?: string  (fallback to docId if absent)
 *   - roles?:  string[]
 *   - permissions?: string[] | map
 */
public class ProjectMemberService {

    private final Firestore db;

    public ProjectMemberService() {
        this.db = FirestoreClient.getFirestore();
    }

    /**
     * Returns all user IDs of members in the given project.
     *
     * Path: /projects/{projectId}/members
     * Logic: take document IDs as user IDs (each member doc id = userId)
     */
    public List<String> getProjectMemberIds(String projectId) throws Exception {
        CollectionReference membersCol = db.collection("projects").document(projectId).collection("members");
        List<QueryDocumentSnapshot> docs = membersCol.get().get().getDocuments();
        List<String> ids = new ArrayList<>(docs.size());
        for (QueryDocumentSnapshot d : docs) {
            String uid = d.getId(); // docId is the userId
            if (uid != null && !uid.isBlank()) ids.add(uid);
        }
        return ids;
    }

    /**
     * Returns user IDs of members in the given project that match the provided role.
     *
     * Path: /projects/{projectId}/members/{memberId}
     * Doc schema: each member doc contains "roleIds": string[]
     * Logic: iterate all members; if roleIds contains roleId, collect doc id (userId).
     */
    public List<String> getProjectMemberIdsByRole(String projectId, String roleId) throws Exception {
        CollectionReference membersCol = db.collection("projects").document(projectId).collection("members");
        List<QueryDocumentSnapshot> docs = membersCol.get().get().getDocuments();
        Set<String> ids = new HashSet<>();
        for (QueryDocumentSnapshot d : docs) {
            String uid = d.getId();
            if (uid == null || uid.isBlank()) continue;

            if (d.contains("roleIds")) {
                List<String> roleIds = (List<String>) d.get("roleIds");
                if (roleIds != null) {
                    for (Object r : roleIds) {
                        if (r != null && roleId.equalsIgnoreCase(String.valueOf(r))) {
                            ids.add(uid);
                            break;
                        }
                    }
                }
            }
        }
        return new ArrayList<>(ids);
    }
}
