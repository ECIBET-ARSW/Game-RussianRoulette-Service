package com.ecibet.russianroulette.model;

public enum RoomStatus {
    WAITING,      // en lobby, esperando jugadores
    IN_PROGRESS,  // partida en curso
    FINISHED      // partida terminada, sala por eliminar
}
