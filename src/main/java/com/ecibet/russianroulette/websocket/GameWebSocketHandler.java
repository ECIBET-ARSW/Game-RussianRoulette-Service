package com.ecibet.russianroulette.websocket;

import com.ecibet.russianroulette.dto.request.AccuseRequest;
import com.ecibet.russianroulette.dto.request.PlayCardsRequest;
import com.ecibet.russianroulette.dto.response.GameStateResponse;
import com.ecibet.russianroulette.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketHandler {

    private final SimpMessagingTemplate messaging;
    private final GameService gameService;

    // Jugador pone cartas boca abajo y declara
    @MessageMapping("/room/{roomId}/play")
    public void playCards(@DestinationVariable String roomId,
                          @Payload PlayCardsRequest request) {
        try {
            GameStateResponse state = gameService.playCards(roomId, request);
            broadcast(roomId, state);
        } catch (Exception e) {
            log.error("Error in playCards for room {}: {}", roomId, e.getMessage());
            broadcastError(roomId, e.getMessage());
        }
    }

    // Jugador acusa al anterior de mentir
    @MessageMapping("/room/{roomId}/accuse")
    public void accuse(@DestinationVariable String roomId,
                       @Payload AccuseRequest request) {
        try {
            GameStateResponse state = gameService.accuse(roomId, request);
            broadcast(roomId, state);
        } catch (Exception e) {
            log.error("Error in accuse for room {}: {}", roomId, e.getMessage());
            broadcastError(roomId, e.getMessage());
        }
    }

    // Jugador jala el gatillo
    @MessageMapping("/room/{roomId}/shoot")
    public void shoot(@DestinationVariable String roomId,
                      @Payload String userId) {
        try {
            GameStateResponse state = gameService.shoot(roomId, userId.replace("\"", ""));
            broadcast(roomId, state);
        } catch (Exception e) {
            log.error("Error in shoot for room {}: {}", roomId, e.getMessage());
            broadcastError(roomId, e.getMessage());
        }
    }

    // Host inicia la partida
    @MessageMapping("/room/{roomId}/start")
    public void startGame(@DestinationVariable String roomId,
                          @Payload String hostId) {
        try {
            String cleanHostId = hostId.replace("\"", "");
            GameStateResponse state = gameService.startGame(roomId, cleanHostId);
            broadcast(roomId, state);
        } catch (Exception e) {
            log.error("Error starting game in room {}: {}", roomId, e.getMessage());
            broadcastError(roomId, e.getMessage());
        }
    }

    public void broadcast(String roomId, GameStateResponse state) {
        messaging.convertAndSend("/topic/room/" + roomId, state);
    }

    private void broadcastError(String roomId, String message) {
        messaging.convertAndSend("/topic/room/" + roomId,
                GameStateResponse.builder().type("ERROR").message(message).build());
    }
}
