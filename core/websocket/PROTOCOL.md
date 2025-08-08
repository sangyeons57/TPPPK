# WebSocket Message Protocol (Envelope)

Scope

- This document defines the client/server transport protocol for WebSocket messaging.
- The Envelope is used ONLY for network transport between client and server.
- DO NOT persist, cache, or expose the Envelope to domain/data layers. It must not leak beyond the
  WebSocket transport layer.

Versioning

- schema: "v2"
- Backward compatibility: older clients may send plain text; servers should gate behavior by
  `schema` when present.

Message Kinds

- kind: "message" | "message_patch"

Envelope: message

- schema: string (required). Example: "v2"
- kind: "message" (required)
- messageId: string (required)
- messageType: string (required). Example: "TEXT"
- payload: object (required). Content JSON (must NOT contain messageType)
    - Example TEXT with image attachment:
      {
      "content": "",
      "attachments": [
      { "kind": "image", "url": "https://...", "mime": "image/jpeg", "filename": "...", "width": 1280, "height": 960, "size": 234567 }
      ]
      }
- replyToMessageId: string | null (optional)
- meta: object (optional) { projectId?: string, channelType?: string, clientSentAt?: number }

Envelope: message_patch

- schema: string (required). Example: "v2"
- kind: "message_patch" (required)
- targetMessageId: string (required)
- patchVersion: number (required, monotonic per message)
- patch: object (required)
    - status?: "media_ready" | "thumb_ready" | "error" | string
    -
  attachments?: [ { index: number, ready?: boolean, width?: number, height?: number, size?: number, thumbnailUrl?: string } ]
    - error?: { code: string, message?: string }

Transport Semantics

- All WebSocket sends MUST serialize the Envelope as a JSON string.
- `messageType` lives in the Envelope ONLY. It must NOT be duplicated inside `payload`.
- `payload` is application content and follows the domain-level `MessagePayload` shape (content +
  attachments), without type.
- Clients MAY continue to optimistically store domain `Message` in local DB. The Envelope is not
  stored.

Image Flow (recommended)

1) Client uploads image, gets a stable download URL.
2) Client sends Envelope(kind="message") with TEXT payload containing the image attachment(s) using
   the stable URL(s).
3) Client may send Envelope(kind="message_patch") to signal readiness and provide
   dimensions/size/thumbnail data.
4) Receiver UI may show a loading spinner until either image load succeeds (client-side) or it
   receives `message_patch` with `status=media_ready`.

Examples

- message
  {
  "schema": "v2",
  "kind": "message",
  "messageId": "abc123",
  "messageType": "TEXT",
  "payload": { "content": "", "
  attachments": [ { "kind": "image", "url": "https://...", "mime": "image/jpeg", "filename": "img.jpg" } ] },
  "replyToMessageId": null,
  "meta": { "projectId": "p1", "channelType": "room" }
  }

- message_patch
  {
  "schema": "v2",
  "kind": "message_patch",
  "targetMessageId": "abc123",
  "patchVersion": 1,
  "patch": {
  "status": "media_ready",
  "attachments": [ { "index": 0, "ready": true, "width": 1280, "height": 960, "size": 234567 } ]
  }
  }

Developer Notes

- Keep Envelope construction within the WebSocket transport boundary (e.g., a sender adapter).
- Domain entities remain unchanged: `MessageType` is a field of the message entity, `MessagePayload`
  contains only content/attachments.
- Do not pass Envelope through use cases or repositories. Build the Envelope right before sending
  over the socket; parse it immediately on receive and then map to domain.


