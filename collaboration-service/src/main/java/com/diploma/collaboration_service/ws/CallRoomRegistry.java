package com.diploma.collaboration_service.ws;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallRoomRegistry {

    private final Map<UUID, CallRoom> rooms = new ConcurrentHashMap<>();

    public CallRoom getOrCreate(UUID sessionId) {
        return rooms.computeIfAbsent(sessionId, CallRoom::new);
    }

    public CallRoom get(UUID sessionId) {
        return rooms.get(sessionId);
    }

    public void evict(UUID sessionId) {
        rooms.remove(sessionId);
    }
}
