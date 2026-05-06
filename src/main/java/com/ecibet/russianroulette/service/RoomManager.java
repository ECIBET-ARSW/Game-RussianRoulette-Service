package com.ecibet.russianroulette.service;

import com.ecibet.russianroulette.model.Room;
import com.ecibet.russianroulette.model.RoomStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {

    @Value("${ecibet.game.max-rooms}") private int maxRooms;
    @Value("${ecibet.game.max-players-per-room}") private int maxPlayers;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom(String hostId, String username, String name, BigDecimal buyIn) {
        if (rooms.size() >= maxRooms)
            throw new IllegalStateException("Maximum number of rooms reached");

        Room room = new Room(hostId, name, buyIn);
        rooms.put(room.getId(), room);
        return room;
    }

    public Room getRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) throw new java.util.NoSuchElementException("Room not found: " + roomId);
        return room;
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public boolean canJoin(String roomId) {
        Room room = rooms.get(roomId);
        return room != null
                && room.getStatus() == RoomStatus.WAITING
                && !room.isFull(maxPlayers);
    }

    public boolean isUserInAnyRoom(String userId) {
        return rooms.values().stream()
                .anyMatch(r -> r.getStatus() == RoomStatus.WAITING
                        && r.findPlayer(userId) != null);
    }

    public Room findRoomByUserId(String userId) {
        return rooms.values().stream()
                .filter(r -> r.findPlayer(userId) != null)
                .findFirst()
                .orElse(null);
    }

    public void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

    public int getMaxPlayers() { return maxPlayers; }
}
