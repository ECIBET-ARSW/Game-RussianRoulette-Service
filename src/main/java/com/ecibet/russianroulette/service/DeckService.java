package com.ecibet.russianroulette.service;

import com.ecibet.russianroulette.model.Card;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DeckService {

    private static final int KINGS = 8;
    private static final int ACES = 8;
    private static final int QUEENS = 8;
    private static final int JOKERS = 6;
    private static final int CARDS_PER_PLAYER = 5;

    // Construye el mazo: 6 reyes + 6 ases + 2 jokers = 14 cartas
    public List<Card> buildDeck() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < KINGS;   i++) deck.add(Card.KING);
        for (int i = 0; i < ACES;    i++) deck.add(Card.ACE);
        for (int i = 0; i < QUEENS;  i++) deck.add(Card.QUEEN);
        for (int i = 0; i < JOKERS;  i++) deck.add(Card.JOKER);
        Collections.shuffle(deck);
        return deck;
    }

    // Reparte 5 cartas a cada jugador, retorna las manos en orden
    public List<List<Card>> deal(int playerCount) {
        List<Card> deck = buildDeck();
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            int from = i * CARDS_PER_PLAYER;
            int to   = from + CARDS_PER_PLAYER;
            hands.add(new ArrayList<>(deck.subList(from, Math.min(to, deck.size()))));
        }
        return hands;
    }
}
