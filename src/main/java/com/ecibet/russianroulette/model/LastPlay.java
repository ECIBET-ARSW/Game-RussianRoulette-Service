package com.ecibet.russianroulette.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LastPlay {
    private String playerId;
    private List<Card> actualCards;   // cartas reales puestas boca abajo
    private Card declaredCard;        // lo que el jugador dijo que eran
    private int declaredCount;        // cuántas dijo que puso
}
