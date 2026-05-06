package com.ecibet.russianroulette.controller;

import com.ecibet.russianroulette.dto.request.CreateRoomRequest;
import com.ecibet.russianroulette.dto.request.JoinRoomRequest;
import com.ecibet.russianroulette.dto.response.GameStateResponse;
import com.ecibet.russianroulette.dto.response.RoomResponse;
import com.ecibet.russianroulette.model.Player;
import com.ecibet.russianroulette.model.Room;
import com.ecibet.russianroulette.service.GameService;
import com.ecibet.russianroulette.service.RoomManager;
import com.ecibet.russianroulette.websocket.GameWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games/liars-bar")
@RequiredArgsConstructor
public class LobbyController {

    private final GameService gameService;
    private final RoomManager roomManager;
    private final GameWebSocketHandler wsHandler;

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms() {
        List<RoomResponse> rooms = roomManager.getAllRooms().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomId) {
        Room room = roomManager.getRoom(roomId);
        return ResponseEntity.ok(toResponse(room));
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest req) {
        Room room = gameService.createRoom(req.getUserId(), req.getUsername(), req.getRoomName(), req.getBuyIn());
        broadcastLobbyUpdate();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(room));
    }

    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String roomId,
                                                  @Valid @RequestBody JoinRoomRequest req) {
        Room room = gameService.joinRoom(roomId, req.getUserId(), req.getUsername());
        broadcastLobbyUpdate();
        return ResponseEntity.ok(toResponse(room));
    }

    @GetMapping("/rooms/{roomId}/state")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable String roomId) {
        Room room = roomManager.getRoom(roomId);
        if (room.getStatus() != com.ecibet.russianroulette.model.RoomStatus.IN_PROGRESS)
            return ResponseEntity.noContent().build();
        GameStateResponse state = gameService.buildCurrentState(roomId);
        return ResponseEntity.ok(state);
    }

    @GetMapping("/rooms/{roomId}/hand")
    public ResponseEntity<List<String>> getHand(@PathVariable String roomId,
                                                 @RequestParam String userId) {
        Room room = roomManager.getRoom(roomId);
        Player player = room.findPlayer(userId);
        if (player == null) return ResponseEntity.notFound().build();
        List<String> hand = player.getHand().stream().map(Enum::name).toList();
        return ResponseEntity.ok(hand);
    }

    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String roomId,
                                           @RequestParam String userId) {
        gameService.leaveRoom(roomId, userId);
        broadcastLobbyUpdate();
        return ResponseEntity.noContent().build();
    }

    // Notifica a todos en /topic/lobby que la lista de salas cambió
    private void broadcastLobbyUpdate() {
        List<RoomResponse> rooms = roomManager.getAllRooms().stream()
                .map(this::toResponse)
                .toList();
        wsHandler.broadcast("lobby", com.ecibet.russianroulette.dto.response.GameStateResponse.builder()
                .type("LOBBY_UPDATE")
                .build());
    }

    private RoomResponse toResponse(Room room) {
        RoomResponse res = new RoomResponse();
        res.setId(room.getId());
        res.setName(room.getName());
        res.setHostId(room.getHostId());
        res.setStatus(room.getStatus());
        res.setBuyIn(room.getBuyIn());
        res.setPot(room.getPot());
        res.setPlayerCount((int) room.getPlayers().stream().filter(p -> !p.isEliminated()).count());
        res.setMaxPlayers(roomManager.getMaxPlayers());
        res.setPlayerNames(room.getPlayers().stream().filter(p -> !p.isEliminated()).map(p -> p.getUsername()).toList());
        return res;
    }
}
