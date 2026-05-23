package com.ecibet.russianroulette.model;

import lombok.Data;

import java.time.Instant;

@Data
public class GameState {
    private int currentRound = 1;
    private Card activeCard;
    private int currentTurnIndex = 0;
    private LastPlay lastPlay;
    private boolean waitingForShoot = false;
    private String shooterPlayerId;
    private Instant turnStartedAt;
    private boolean firstTurn = true;
}
