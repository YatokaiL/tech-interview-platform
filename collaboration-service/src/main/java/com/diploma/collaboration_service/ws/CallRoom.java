package com.diploma.collaboration_service.ws;

import com.diploma.collaboration_service.security.JwtPrincipal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A 1:1 audio/video call room for a session. Holds only presence and the shared
 * "phase" (lobby vs coding) — no media flows through here, the server just
 * relays WebRTC signaling between the two peers.
 */
public class CallRoom {

    @Getter
    private final UUID sessionId;

    /** Shared interview phase: "lobby" until the interviewer opens the coding section. */
    @Getter
    @Setter
    private volatile String phase = "lobby";

    private final Map<String, Participant> participants = new ConcurrentHashMap<>();

    public CallRoom(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Participant addParticipant(WebSocketSession session, JwtPrincipal principal) {
        Participant participant = new Participant(session, principal);
        participants.put(session.getId(), participant);
        return participant;
    }

    public Participant removeParticipant(String wsSessionId) {
        return participants.remove(wsSessionId);
    }

    public WebSocketSession findBySessionId(String wsSessionId) {
        Participant p = participants.get(wsSessionId);
        return p == null ? null : p.session();
    }

    public Collection<Participant> participants() {
        return participants.values();
    }

    public int size() {
        return participants.size();
    }

    public record Participant(WebSocketSession session, JwtPrincipal principal) {}
}
