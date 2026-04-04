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
    private boolean ready = false;
    private boolean spectator = false;
    private int shotsFired = 0;
}
