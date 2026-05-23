package com.ecibet.russianroulette.service;

import com.ecibet.russianroulette.dto.request.AccuseRequest;
import com.ecibet.russianroulette.dto.response.GameStateResponse;
import com.ecibet.russianroulette.model.*;
import com.ecibet.russianroulette.websocket.GameWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurnTimerService {

    private final RoomManager roomManager;
    private final GameService gameService;
    private final GameWebSocketHandler wsHandler;

    @Value("${ecibet.game.turn-timer-seconds}") private int turnTimerSeconds;

    // Revisa cada segundo si algún turno expiró
    @Scheduled(fixedDelay = 1000)
    public void checkExpiredTurns() {
        roomManager.getAllRooms().stream()
                .filter(r -> r.getStatus() == RoomStatus.IN_PROGRESS)
                .forEach(this::handleRoomTimer);
    }

    private void handleRoomTimer(Room room) {
        GameState state = room.getGameState();
        if (state == null || state.getTurnStartedAt() == null) return;

        long elapsed = Instant.now().toEpochMilli() - state.getTurnStartedAt().toEpochMilli();
        long effectiveTimer = state.isFirstTurn() ? (turnTimerSeconds * 1000L + 5000L) : (turnTimerSeconds * 1000L);
        if (elapsed < effectiveTimer) return;

        try {
            // Si está esperando disparo, dispara automáticamente
            if (state.isWaitingForShoot()) {
                log.info("Auto-shoot triggered for player {} in room {}", state.getShooterPlayerId(), room.getId());
                GameStateResponse response = gameService.shoot(room.getId(), state.getShooterPlayerId());
                wsHandler.broadcast(room.getId(), response);
                return;
            }

            // Si no hay jugada previa, fuerza acción automática: jugar una carta aleatoria
            List<Player> alive = room.getActivePlayers();
            if (alive.isEmpty()) return;

            Player current = alive.get(state.getCurrentTurnIndex() % alive.size());

            // Si tiene cartas, fuerza jugarlas; si no, acusa automáticamente
            if (!current.getHand().isEmpty() && state.getLastPlay() != null) {
                AccuseRequest autoAccuse = new AccuseRequest();
                autoAccuse.setUserId(current.getUserId());
                log.info("Auto-accuse triggered for player {} in room {}", current.getUserId(), room.getId());
                GameStateResponse response = gameService.accuse(room.getId(), autoAccuse);
                wsHandler.broadcast(room.getId(), response);
            } else if (!current.getHand().isEmpty()) {
                // Fuerza jugar la primera carta declarando la carta activa
                com.ecibet.russianroulette.dto.request.PlayCardsRequest autoPlay =
                        new com.ecibet.russianroulette.dto.request.PlayCardsRequest();
                autoPlay.setUserId(current.getUserId());
                autoPlay.setCards(List.of(current.getHand().get(0)));
                autoPlay.setDeclaredCard(state.getActiveCard());
                autoPlay.setDeclaredCount(1);
                log.info("Auto-play triggered for player {} in room {}", current.getUserId(), room.getId());
                GameStateResponse response = gameService.playCards(room.getId(), autoPlay);
                wsHandler.broadcast(room.getId(), response);
            }
        } catch (Exception e) {
            log.error("Error in timer for room {}: {}", room.getId(), e.getMessage());
        }
    }
}
