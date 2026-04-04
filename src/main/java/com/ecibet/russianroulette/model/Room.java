package com.ecibet.russianroulette.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Room {
    private final String id = UUID.randomUUID().toString();
    private String hostId;
    private String name;
    private RoomStatus status = RoomStatus.WAITING;
    private BigDecimal buyIn;
    private BigDecimal pot = BigDecimal.ZERO;
    private List<Player> players = new ArrayList<>();
    private GameState gameState;
    private Revolver revolver;

    public Room(String hostId, String name, BigDecimal buyIn) {
        this.hostId = hostId;
        this.name = name;
        this.buyIn = buyIn;
    }

    public List<Player> getActivePlayers() {
        return players.stream().filter(p -> !p.isEliminated()).toList();
    }

    public boolean isFull(int maxPlayers) {
        return players.size() >= maxPlayers;
    }

    public Player findPlayer(String userId) {
        return players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }
}
