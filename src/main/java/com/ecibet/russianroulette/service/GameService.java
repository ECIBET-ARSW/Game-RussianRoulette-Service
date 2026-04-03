package com.ecibet.russianroulette.service;

import com.ecibet.russianroulette.client.WalletClient;
import com.ecibet.russianroulette.dto.request.AccuseRequest;
import com.ecibet.russianroulette.dto.request.PlayCardsRequest;
import com.ecibet.russianroulette.dto.response.GameStateResponse;
import com.ecibet.russianroulette.dto.response.GameStateResponse.*;
import com.ecibet.russianroulette.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final RoomManager roomManager;
    private final DeckService deckService;
    private final WalletClient walletClient;

    @Value("${ecibet.game.min-players-to-start}") private int minPlayers;
    @Value("${ecibet.game.turn-timer-seconds}") private int turnTimer;

    // ── Lobby ────────────────────────────────────────────────────────────────

    public Room createRoom(String hostId, String username, String name, BigDecimal buyIn) {
        Room room = roomManager.createRoom(hostId, username, name, buyIn);
        Player host = new Player(hostId, username);
        room.getPlayers().add(host);
        debitBuyIn(hostId, buyIn);
        room.setPot(buyIn);
        log.info("Room created: {} by {}", room.getId(), hostId);
        return room;
    }

    public Room joinRoom(String roomId, String userId, String username) {
        if (!roomManager.canJoin(roomId))
            throw new IllegalStateException("Cannot join room " + roomId);

        Room room = roomManager.getRoom(roomId);
        if (room.findPlayer(userId) != null)
            throw new IllegalStateException("Player already in room");

        room.getPlayers().add(new Player(userId, username));
        debitBuyIn(userId, room.getBuyIn());
        room.setPot(room.getPot().add(room.getBuyIn()));
        return room;
    }

    public Room leaveRoom(String roomId, String userId) {
        Room room = roomManager.getRoom(roomId);
        if (room.getStatus() == RoomStatus.WAITING) {
            room.getPlayers().removeIf(p -> p.getUserId().equals(userId));
            walletClient.credit(userId, room.getBuyIn(), "Buy-in refund - left lobby");
            room.setPot(room.getPot().subtract(room.getBuyIn()));
            if (room.getPlayers().isEmpty()) roomManager.removeRoom(roomId);
            else if (room.getHostId().equals(userId))
                room.setHostId(room.getPlayers().get(0).getUserId());
        }
        return room;
    }

    public GameStateResponse buildCurrentState(String roomId) {
        Room room = roomManager.getRoom(roomId);
        return buildStateResponse(room, "GAME_STARTED", "Partida en curso");
    }

    // ── Game start ───────────────────────────────────────────────────────────

    public GameStateResponse startGame(String roomId, String requesterId) {
        Room room = roomManager.getRoom(roomId);

        if (!room.getHostId().equals(requesterId))
            throw new IllegalStateException("Only the host can start the game");
        if (room.getPlayers().size() < minPlayers)
            throw new IllegalStateException("Not enough players");
        if (room.getStatus() != RoomStatus.WAITING)
            throw new IllegalStateException("Room is not in waiting state");

        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setRevolver(new Revolver());

        GameState state = new GameState();
        state.setActiveCard(Card.KING); // primera ronda siempre KING
        state.setTurnStartedAt(Instant.now());
        room.setGameState(state);

        dealCards(room);

        return buildStateResponse(room, "GAME_STARTED", "¡La partida ha comenzado! Carta de la ronda: KING");
    }

    // ── Gameplay ─────────────────────────────────────────────────────────────

    public GameStateResponse playCards(String roomId, PlayCardsRequest req) {
        Room room = roomManager.getRoom(roomId);
        GameState state = room.getGameState();
        validateTurn(room, req.getUserId());

        // Si el turno anterior dejó a alguien sin cartas y no fue acusado, ese jugador gana
        if (state.getLastPlayerEmptyHand() != null) {
            Player winner = room.findPlayer(state.getLastPlayerEmptyHand());
            return endGame(room, winner);
        }

        Player player = room.findPlayer(req.getUserId());

        if (req.getCards().size() > 3)
            throw new IllegalStateException("Cannot play more than 3 cards at once");

        // Validar que el jugador tenga esas cartas en la mano
        for (Card card : req.getCards()) {
            if (!player.getHand().remove(card))
                throw new IllegalStateException("Player does not have card: " + card);
        }

        state.setLastPlay(new LastPlay(
                req.getUserId(),
                req.getCards(),
                req.getDeclaredCard(),
                req.getDeclaredCount()
        ));

        advanceTurn(room);

        // Si el jugador se quedó sin cartas, el siguiente puede acusarlo
        // Si el siguiente decide jugar (no acusar), gana el que se quedó sin cartas
        state.setLastPlayerEmptyHand(player.getHand().isEmpty() ? player.getUserId() : null);
        state.setTurnStartedAt(Instant.now());

        String msg = String.format("%s puso %d carta(s) y declaró: %d %s(s)",
                player.getUsername(), req.getCards().size(),
                req.getDeclaredCount(), req.getDeclaredCard());

        GameStateResponse response = buildStateResponse(room, "CARDS_PLAYED", msg);
        response.setLastPlayerId(req.getUserId());
        return response;
    }

    public GameStateResponse accuse(String roomId, AccuseRequest req) {
        Room room = roomManager.getRoom(roomId);
        GameState state = room.getGameState();
        validateTurn(room, req.getUserId());

        if (state.getLastPlay() == null)
            throw new IllegalStateException("No play to accuse");

        state.setLastPlayerEmptyHand(null);

        LastPlay last = state.getLastPlay();
        boolean wasLying = isLying(last, state.getActiveCard());

        // El perdedor es quien mintió (si acusación correcta) o quien acusó (si acusación incorrecta)
        String loserId = wasLying ? last.getPlayerId() : req.getUserId();
        Player loser = room.findPlayer(loserId);

        state.setWaitingForShoot(true);
        state.setShooterPlayerId(loserId);
        state.setLastPlay(null);

        RevealedPlay reveal = RevealedPlay.builder()
                .playerId(last.getPlayerId())
                .actualCards(last.getActualCards())
                .declaredCard(last.getDeclaredCard())
                .declaredCount(last.getDeclaredCount())
                .wasLying(wasLying)
                .loserPlayerId(loserId)
                .build();

        GameStateResponse response = buildStateResponse(room, "ACCUSED",
                loser.getUsername() + " debe enfrentar el revólver");
        response.setRevealedPlay(reveal);
        return response;
    }

    public GameStateResponse shoot(String roomId, String userId) {
        Room room = roomManager.getRoom(roomId);
        GameState state = room.getGameState();

        if (!state.isWaitingForShoot() || !state.getShooterPlayerId().equals(userId))
            throw new IllegalStateException("It's not your turn to shoot");

        boolean eliminated = room.getRevolver().pullTrigger();
        Player shooter = room.findPlayer(userId);

        state.setWaitingForShoot(false);
        state.setShooterPlayerId(null);

        ShotResult shotResult = ShotResult.builder()
                .playerId(userId)
                .eliminated(eliminated)
                .build();

        if (eliminated) {
            shooter.setEliminated(true);
            shooter.setSpectator(true);
            log.info("Player {} eliminated in room {}", userId, roomId);

            List<Player> alive = room.getActivePlayers();
            if (alive.size() == 1) return endGame(room, alive.get(0));
        } else if (shooter.getHand().isEmpty()) {
            // Sobrevivió el disparo y no tiene cartas → gana
            return endGame(room, shooter);
        }

        // Nueva ronda: repartir cartas y alternar carta activa
        startNewRound(room);

        GameStateResponse response = buildStateResponse(room, "SHOT_RESULT",
                eliminated ? shooter.getUsername() + " fue eliminado" : shooter.getUsername() + " sobrevivió");
        response.setShotResult(shotResult);
        return response;
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private boolean isLying(LastPlay play, Card activeCard) {
        // Miente si alguna carta real no es la carta activa ni joker
        boolean cardsAreFake = play.getActualCards().stream()
                .anyMatch(c -> c != activeCard && c != Card.JOKER);
        // Miente si la cantidad declarada no coincide con las cartas jugadas
        boolean countIsWrong = play.getDeclaredCount() != play.getActualCards().size();
        return cardsAreFake || countIsWrong;
    }

    private void validateTurn(Room room, String userId) {
        if (room.getStatus() != RoomStatus.IN_PROGRESS)
            throw new IllegalStateException("Game is not in progress");
        if (room.getGameState().isWaitingForShoot())
            throw new IllegalStateException("Waiting for shoot");

        List<Player> alive = room.getActivePlayers();
        int idx = room.getGameState().getCurrentTurnIndex() % alive.size();
        if (!alive.get(idx).getUserId().equals(userId))
            throw new IllegalStateException("Not your turn");

        Player player = room.findPlayer(userId);
        if (player != null && player.getHand().isEmpty())
            throw new IllegalStateException("No cards to play");
    }

    private void advanceTurn(Room room) {
        GameState state = room.getGameState();
        int next = (state.getCurrentTurnIndex() + 1) % room.getActivePlayers().size();
        state.setCurrentTurnIndex(next);
    }

    private void dealCards(Room room) {
        List<Player> active = room.getActivePlayers();
        List<List<Card>> hands = deckService.deal(active.size());
        for (int i = 0; i < active.size(); i++) {
            active.get(i).setHand(hands.get(i));
        }
    }

    private void startNewRound(Room room) {
        GameState state = room.getGameState();
        state.setCurrentRound(state.getCurrentRound() + 1);
        state.setActiveCard(state.getActiveCard() == Card.KING ? Card.ACE : Card.KING);
        state.setCurrentTurnIndex(0);
        state.setLastPlay(null);
        state.setLastPlayerEmptyHand(null);
        state.setTurnStartedAt(Instant.now());
    }

    private GameStateResponse endGame(Room room, Player winner) {
        room.setStatus(RoomStatus.FINISHED);
        // TODO: habilitar cuando Wallets-Service esté corriendo
        // walletClient.credit(winner.getUserId(), room.getPot(), "Liar's Bar - winner payout");
        log.info("[DEV] Skipping wallet credit for winner {} amount {}", winner.getUserId(), room.getPot());
        log.info("Game over in room {}. Winner: {}", room.getId(), winner.getUserId());

        GameStateResponse response = buildStateResponse(room, "GAME_OVER",
                winner.getUsername() + " gana el pozo de " + room.getPot() + " COP");
        response.setWinnerId(winner.getUserId());
        response.setWinnerUsername(winner.getUsername());

        roomManager.removeRoom(room.getId());
        return response;
    }

    private void debitBuyIn(String userId, BigDecimal buyIn) {
        // TODO: habilitar cuando Wallets-Service esté corriendo
        // boolean ok = walletClient.debit(userId, buyIn, "Liar's Bar - buy-in");
        // if (!ok) throw new IllegalStateException("Insufficient funds for buy-in");
        log.info("[DEV] Skipping wallet debit for user {} amount {}", userId, buyIn);
    }

    private GameStateResponse buildStateResponse(Room room, String type, String message) {
        GameState state = room.getGameState();
        List<Player> alive = room.getActivePlayers();

        Player currentPlayer = (state != null && !alive.isEmpty())
                ? alive.get(state.getCurrentTurnIndex() % alive.size())
                : null;
        String currentTurnId       = currentPlayer != null ? currentPlayer.getUserId()   : null;
        String currentTurnUsername = currentPlayer != null ? currentPlayer.getUsername() : null;

        List<PlayerStateResponse> playerStates = room.getPlayers().stream()
                .map(p -> PlayerStateResponse.builder()
                        .userId(p.getUserId())
                        .username(p.getUsername())
                        .cardCount(p.getHand().size())
                        .eliminated(p.isEliminated())
                        .spectator(p.isSpectator())
                        .isCurrentTurn(p.getUserId().equals(currentTurnId))
                        .build())
                .toList();

        return GameStateResponse.builder()
                .type(type)
                .message(message)
                .currentRound(state != null ? state.getCurrentRound() : 0)
                .activeCard(state != null ? state.getActiveCard() : null)
                .currentTurnPlayerId(currentTurnId)
                .currentTurnUsername(currentTurnUsername)
                .turnTimerSeconds(turnTimer)
                .players(playerStates)
                .build();
    }
}
