package com.ecibet.russianroulette.dto.request;

import com.ecibet.russianroulette.model.Card;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PlayCardsRequest {
    @NotBlank
    private String userId;
    @NotEmpty
    private List<Card> cards;      // cartas reales que pone boca abajo
    @NotNull
    private Card declaredCard;     // lo que declara (KING o ACE)
    private int declaredCount;     // cuántas dice que pone
}
