package com.ecibet.russianroulette.model;

import lombok.Getter;

import java.util.Random;

@Getter
public class Revolver {

    private static final int CHAMBERS = 6;
    private final boolean[] cylinder = new boolean[CHAMBERS]; // true = bala
    private int currentPosition;

    public Revolver() {
        Random random = new Random();
        // 1 bala en posición aleatoria
        cylinder[random.nextInt(CHAMBERS)] = true;
        // posición inicial aleatoria — el tambor no se reinicia entre disparos
        currentPosition = random.nextInt(CHAMBERS);
    }

    // Retorna true si el jugador muere, false si sobrevive
    public boolean pullTrigger() {
        boolean fired = cylinder[currentPosition];
        currentPosition = (currentPosition + 1) % CHAMBERS;
        return fired;
    }
}
