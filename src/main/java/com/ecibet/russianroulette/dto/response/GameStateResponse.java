package com.ecibet.russianroulette.dto.response;

import com.ecibet.russianroulette.model.Card;
import lombok.Builder;
import lombok.Data;

import java.util.List;

// Enviado por WebSocket a /topic/room/{roomId} en cada cambio de estado
@Data
@Builder
public class GameStateResponse {
    private String type;              // GAME_STARTED, CARDS_PLAYED, ACCUSED, SHOT_RESULT, GAME_OVER, LOBBY_UPDATE
    private String message;
    private int currentRound;
    private Card activeCard;
    private String currentTurnPlayerId;
    private String currentTurnUsername;
    private String lastPlayerId;  // quien acaba de jugar (antes del advanceTurn)
    private int turnTimerSeconds;

    // Solo visible cuando hay acusación (se revelan las cartas)
    private RevealedPlay revealedPlay;

    // Solo cuando alguien dispara
    private ShotResult shotResult;

    // Lista de jugadores con su estado (vivo/eliminado)
    private List<PlayerStateResponse> players;

    // Disparos del revólver
    private int shotsFired;
    private int totalChambers;

    // Solo al terminar la partida
    private String winnerId;
    private String winnerUsername;

    @Data
    @Builder
    public static class RevealedPlay {
        private String playerId;
        private List<Card> actualCards;
        private Card declaredCard;
        private int declaredCount;
        private boolean wasLying;
        private String loserPlayerId;   // quien enfrenta el revólver
    }

    @Data
    @Builder
    public static class ShotResult {
        private String playerId;
        private boolean eliminated;     // true = murió, false = sobrevivió
    }

    @Data
    @Builder
    public static class PlayerStateResponse {
        private String userId;
        private String username;
        private int cardCount;
        private boolean eliminated;
        private boolean spectator;
        private boolean isCurrentTurn;
        private int shotsFired;
    }
}
