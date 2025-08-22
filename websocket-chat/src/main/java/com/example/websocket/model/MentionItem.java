package com.example.websocket.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message-level mention item passed through WebSocket and stored in Firestore as-is.
 * type: "USER" | "ROLE" | "EVERYONE"
 * id: userId | roleId | "*" (for EVERYONE if client sends)
 * displayName: client-rendered text for UI convenience
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MentionItem {

    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    private String id;

    @JsonProperty("displayName")
    private String displayName;

    public MentionItem() {}

    public MentionItem(String type, String id, String displayName) {
        this.type = type;
        this.id = id;
        this.displayName = displayName;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}

