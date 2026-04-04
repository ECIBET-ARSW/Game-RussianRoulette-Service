package com.ecibet.russianroulette.dto.response;

import com.ecibet.russianroulette.model.RoomStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomResponse {
    private String id;
    private String name;
    private String hostId;
    private RoomStatus status;
    private BigDecimal buyIn;
    private BigDecimal pot;
    private int playerCount;
    private int maxPlayers;
    private List<String> playerNames;
}
