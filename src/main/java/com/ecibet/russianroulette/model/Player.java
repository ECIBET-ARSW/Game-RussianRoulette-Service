package com.ecibet.russianroulette.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Player {
    private final String userId;
    private final String username;
    private List<Card> hand = new ArrayList<>();
    private boolean eliminated = false;
    private boolean ready = false;       // listo para iniciar en lobby
    private boolean spectator = false;   // eliminado pero sigue viendo
}
