package com.example.tictactoe.model;

public enum GameStatus {
    WAITING_FOR_PLAYER, // Solo hay un jugador en la sala
    IN_PROGRESS,        // Partida con dos jugadores activa
    FINISHED            // La partida ha terminado (ganador o empate)
}