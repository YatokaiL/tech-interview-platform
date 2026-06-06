package com.diploma.collaboration_service.ws;

import com.diploma.collaboration_service.security.JwtPrincipal;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Room {

    @Getter
    private final UUID sessionId;

    private final Map<String, Participant> participants = new ConcurrentHashMap<>();

    private final ReentrantLock stateLock = new ReentrantLock();

    @Getter
    private volatile String code = "";

    @Getter
    private volatile String language;

    @Getter
    private volatile Instant lastUpdatedAt = Instant.now();

    public Room(UUID sessionId, String language, String initialCode) {
        this.sessionId = sessionId;
        this.language = language;
        if (initialCode != null) this.code = initialCode;
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

    public void updateCode(String newCode, String newLanguage) {
        stateLock.lock();
        try {
            if (newCode != null) this.code = newCode;
            if (newLanguage != null && !newLanguage.isBlank()) this.language = newLanguage;
            this.lastUpdatedAt = Instant.now();
        } finally {
            stateLock.unlock();
        }
    }

    public record Participant(WebSocketSession session, JwtPrincipal principal) {}
}
