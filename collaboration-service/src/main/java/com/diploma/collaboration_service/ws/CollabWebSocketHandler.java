package com.diploma.collaboration_service.ws;

import com.diploma.collaboration_service.client.SessionClient;
import com.diploma.collaboration_service.security.JwtPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private final RoomRegistry registry;
    private final ObjectMapper mapper;
    private final SessionClient sessionClient;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        JwtPrincipal principal = (JwtPrincipal) session.getAttributes().get(HandshakeAttrs.PRINCIPAL);
        UUID sessionId = (UUID) session.getAttributes().get(HandshakeAttrs.SESSION_ID);
        SessionClient.SessionInfo info = (SessionClient.SessionInfo) session.getAttributes().get(HandshakeAttrs.SESSION_INFO);

        Room room = registry.get(sessionId);
        if (room == null) {
            String initialCode = "";
            SessionClient.LatestSnapshot snap = sessionClient.fetchLatestSnapshot(sessionId, principal.rawToken());
            if (snap != null && snap.getCode() != null) initialCode = snap.getCode();
            room = registry.getOrCreate(sessionId, info.getLanguage(), initialCode);
        }

        Room.Participant me = room.addParticipant(session, principal);
        log.info("WS connect session={} user={} role={} roomSize={}",
                sessionId, principal.email(), principal.role(), room.size());

        sendInit(session, room, me);
        broadcastExcept(room, session, "participant-joined", buildPresence(principal));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID sessionId = (UUID) session.getAttributes().get(HandshakeAttrs.SESSION_ID);
        Room room = registry.get(sessionId);
        if (room == null) {
            close(session, CloseStatus.SERVER_ERROR.withReason("Room not found"));
            return;
        }
        JwtPrincipal principal = (JwtPrincipal) session.getAttributes().get(HandshakeAttrs.PRINCIPAL);

        JsonNode payload;
        try {
            payload = mapper.readTree(message.getPayload());
        } catch (IOException e) {
            sendError(session, "Invalid JSON");
            return;
        }

        String type = payload.path("type").asText();
        switch (type) {
            case "edit" -> handleEdit(room, session, principal, payload);
            case "cursor" -> handleCursor(room, session, principal, payload);
            case "chat" -> handleChat(room, principal, payload);
            case "ping" -> { /* keep-alive, ignore */ }
            default -> sendError(session, "Unknown message type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID sessionId = (UUID) session.getAttributes().get(HandshakeAttrs.SESSION_ID);
        if (sessionId == null) return;
        Room room = registry.get(sessionId);
        if (room == null) return;

        Room.Participant gone = room.removeParticipant(session.getId());
        log.info("WS disconnect session={} user={} status={} roomSize={}",
                sessionId, gone != null ? gone.principal().email() : "?", status, room.size());

        if (gone != null) {
            broadcastExcept(room, session, "participant-left", buildPresence(gone.principal()));
        }
        if (room.size() == 0) {
            registry.evict(sessionId);
        }
    }

    // --- message handlers --------------------------------------------------

    private void handleEdit(Room room, WebSocketSession sender, JwtPrincipal principal, JsonNode payload) {
        String code = payload.path("code").asText("");
        String language = payload.path("language").asText(null);
        room.updateCode(code, language);

        ObjectNode out = mapper.createObjectNode();
        out.put("type", "edit");
        out.put("code", code);
        if (language != null) out.put("language", language);
        out.set("by", buildPresence(principal));
        out.put("at", Instant.now().toString());

        broadcastExcept(room, sender, out);
    }

    private void handleCursor(Room room, WebSocketSession sender, JwtPrincipal principal, JsonNode payload) {
        ObjectNode out = mapper.createObjectNode();
        out.put("type", "cursor");
        out.put("line", payload.path("line").asInt(1));
        out.put("column", payload.path("column").asInt(1));
        out.set("by", buildPresence(principal));
        broadcastExcept(room, sender, out);
    }

    private void handleChat(Room room, JwtPrincipal principal, JsonNode payload) {
        String text = payload.path("text").asText("").trim();
        if (text.isEmpty()) return;
        if (text.length() > 2000) text = text.substring(0, 2000);

        ObjectNode out = mapper.createObjectNode();
        out.put("type", "chat");
        out.put("text", text);
        out.set("by", buildPresence(principal));
        out.put("at", Instant.now().toString());

        broadcastAll(room, out);

        ObjectNode persisted = mapper.createObjectNode();
        persisted.put("text", text);
        persisted.put("email", principal.email());
        persisted.put("role", principal.role());
        sessionClient.appendEvent(room.getSessionId(), principal.rawToken(), "chat", persisted);
    }

    // --- helpers -----------------------------------------------------------

    private void sendInit(WebSocketSession session, Room room, Room.Participant me) throws IOException {
        ObjectNode out = mapper.createObjectNode();
        out.put("type", "init");
        out.put("code", room.getCode());
        out.put("language", room.getLanguage());
        out.set("you", buildPresence(me.principal()));

        List<ObjectNode> presence = new ArrayList<>();
        for (Room.Participant p : room.participants()) {
            presence.add(buildPresence(p.principal()));
        }
        out.set("participants", mapper.valueToTree(presence));
        session.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
    }

    private ObjectNode buildPresence(JwtPrincipal p) {
        ObjectNode node = mapper.createObjectNode();
        node.put("userId", p.userId().toString());
        node.put("email", p.email());
        node.put("role", p.role());
        return node;
    }

    private void broadcastExcept(Room room, WebSocketSession sender, String type, ObjectNode body) {
        ObjectNode envelope = body.deepCopy();
        envelope.put("type", type);
        broadcastExcept(room, sender, envelope);
    }

    private void broadcastExcept(Room room, WebSocketSession sender, ObjectNode envelope) {
        String text;
        try {
            text = mapper.writeValueAsString(envelope);
        } catch (IOException e) {
            log.warn("Failed to serialize WS message", e);
            return;
        }
        TextMessage msg = new TextMessage(text);
        for (Room.Participant p : room.participants()) {
            if (p.session() == sender) continue;
            sendQuiet(p.session(), msg);
        }
    }

    private void broadcastAll(Room room, ObjectNode envelope) {
        String text;
        try {
            text = mapper.writeValueAsString(envelope);
        } catch (IOException e) {
            log.warn("Failed to serialize WS message", e);
            return;
        }
        TextMessage msg = new TextMessage(text);
        for (Room.Participant p : room.participants()) {
            sendQuiet(p.session(), msg);
        }
    }

    private void sendQuiet(WebSocketSession session, TextMessage msg) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(msg);
        } catch (IOException e) {
            log.debug("Failed to send WS message to {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String message) {
        ObjectNode out = mapper.createObjectNode();
        out.put("type", "error");
        out.put("message", message);
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
        } catch (IOException ignored) {}
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {}
    }

}
