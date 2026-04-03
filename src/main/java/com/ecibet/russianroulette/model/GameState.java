package com.ecibet.russianroulette.model;

import lombok.Data;

import java.time.Instant;

@Data
public class GameState {
    private int currentRound = 1;
    private Card activeCard;           // carta válida de la ronda (KING o ACE, alterna)
    private int currentTurnIndex = 0;  // índice del jugador activo en la lista de vivos
    private LastPlay lastPlay;         // última jugada, acusable por el siguiente jugador
    private boolean waitingForShoot = false;
    private String shooterPlayerId;
    private String lastPlayerEmptyHand; // userId del jugador que se quedó sin cartas en el turno anterior
    private Instant turnStartedAt;
}
