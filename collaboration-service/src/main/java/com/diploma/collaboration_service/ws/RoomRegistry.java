package com.diploma.collaboration_service.ws;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomRegistry {

    private final Map<UUID, Room> rooms = new ConcurrentHashMap<>();

    public Room getOrCreate(UUID sessionId, String language, String initialCode) {
        return rooms.computeIfAbsent(sessionId, id -> new Room(id, language, initialCode));
    }

    public Room get(UUID sessionId) {
        return rooms.get(sessionId);
    }

    public void evict(UUID sessionId) {
        rooms.remove(sessionId);
    }

    public int activeRooms() {
        return rooms.size();
    }
}
