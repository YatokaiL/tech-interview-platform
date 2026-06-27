package com.diploma.collaboration_service.ws;

import com.diploma.collaboration_service.security.JwtPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.UUID;

/**
 * Relays WebRTC signaling for a 1:1 interview call. The server never sees media —
 * it only forwards SDP offers/answers, ICE candidates, the shared phase
 * (lobby ↔ coding) and peers' media on/off state between the two participants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallSignalingHandler extends TextWebSocketHandler {

    private static final int CONCURRENT_BUFFER_BYTES = 1_048_576;
    private static final int CONCURRENT_SEND_TIME_MS = 10_000;

    private final CallRoomRegistry registry;
    private final ObjectMapper mapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        JwtPrincipal principal = (JwtPrincipal) session.getAttributes().get(HandshakeAttrs.PRINCIPAL);
        UUID sessionId = (UUID) session.getAttributes().get(HandshakeAttrs.SESSION_ID);

        CallRoom room = registry.getOrCreate(sessionId);
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, CONCURRENT_SEND_TIME_MS, CONCURRENT_BUFFER_BYTES);
        room.addParticipant(safe, principal);
        log.info("Call WS connect session={} user={} role={} roomSize={}",
                sessionId, principal.email(), principal.role(), room.size());

        sendJoined(safe, room, principal);
        broadcastExcept(room, safe, envelope("peer-joined").set("from", presence(principal)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID sessionId = (UUID) session.getAttributes().get(HandshakeAttrs.SESSION_ID);
        CallRoom room = registry.get(sessionId);
        if (room == null) {
            close(session, CloseStatus.SERVER_ERROR.withReason("Call room not found"));
            return;
        }
        WebSocketSession senderInRoom = room.findBySessionId(session.getId());
        WebSocketSession sender = senderInRoom != null ? senderInRoom : session;
        JwtPrincipal principal = (JwtPrincipal) session.getAttributes().get(HandshakeAttrs.PRINCIPAL);

        JsonNode payload;
        try {
            payload = mapper.readTree(message.getPayload());
        } catch (IOException e) {
            return;
        }

        String type = payload.path("type").asText();
        switch (type) {
            case "sdp", "ice", "media" -> relay(room, sender, principal, payload);
            case "phase" -> {
                String phase = payload.path("phase").asText("lobby");
                room.setPhase(phase);
                relay(room, sender, principal, payload);
            }
            case "ping" -> { /* keep-alive */ }
            default -> log.debug("Unknown call message type: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID sessionId = (UUID) session.getAttributes().get(HandshakeAttrs.SESSION_ID);
        if (sessionId == null) return;
        CallRoom room = registry.get(sessionId);
        if (room == null) return;

        CallRoom.Participant gone = room.removeParticipant(session.getId());
        log.info("Call WS disconnect session={} user={} roomSize={}",
                sessionId, gone != null ? gone.principal().email() : "?", room.size());
        if (gone != null) {
            broadcastExcept(room, session, envelope("peer-left").set("from", presence(gone.principal())));
        }
        if (room.size() == 0) {
            registry.evict(sessionId);
        }
    }

    // --- helpers -----------------------------------------------------------

    /** Forwards a message to the other peer(s) verbatim, stamping the sender. */
    private void relay(CallRoom room, WebSocketSession sender, JwtPrincipal principal, JsonNode payload) {
        ObjectNode out = payload.deepCopy();
        out.set("from", presence(principal));
        broadcastExcept(room, sender, out);
    }

    private void sendJoined(WebSocketSession session, CallRoom room, JwtPrincipal me) throws IOException {
        ObjectNode out = envelope("joined");
        out.set("you", presence(me));
        out.put("phase", room.getPhase());
        ArrayNode peers = out.putArray("peers");
        for (CallRoom.Participant p : room.participants()) {
            if (p.principal().userId().equals(me.userId())) continue;
            peers.add(presence(p.principal()));
        }
        session.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
    }

    private ObjectNode presence(JwtPrincipal p) {
        ObjectNode node = mapper.createObjectNode();
        node.put("userId", p.userId().toString());
        node.put("email", p.email());
        node.put("role", p.role());
        return node;
    }

    private ObjectNode envelope(String type) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", type);
        return node;
    }

    private void broadcastExcept(CallRoom room, WebSocketSession sender, ObjectNode envelope) {
        String text;
        try {
            text = mapper.writeValueAsString(envelope);
        } catch (IOException e) {
            log.warn("Failed to serialize call WS message", e);
            return;
        }
        TextMessage msg = new TextMessage(text);
        String senderId = sender.getId();
        for (CallRoom.Participant p : room.participants()) {
            if (p.session().getId().equals(senderId)) continue;
            sendQuiet(p.session(), msg);
        }
    }

    private void sendQuiet(WebSocketSession session, TextMessage msg) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(msg);
        } catch (Exception e) {
            log.warn("Failed to send call WS message to {}: {}", session.getId(), e.toString());
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {}
    }
}
