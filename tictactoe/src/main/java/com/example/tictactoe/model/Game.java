package com.example.tictactoe.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Game {
    private String gameId;
    private int[][] board;
    private Player playerX;
    private Player playerO;
    private int currentTurn; // 1 para X, 2 para O
    private GameStatus status;

    public Game() {
        this.gameId = UUID.randomUUID().toString();
        this.board = new int[3][3]; 
        this.currentTurn = 1; // Por regla de negocio, inicia X
        this.status = GameStatus.WAITING_FOR_PLAYER;
    }
}

/**
 * Define los estados posibles del ciclo de vida de una partida.
 */
enum GameStatus {
    WAITING_FOR_PLAYER, // Solo hay un jugador en la sala
    IN_PROGRESS,        // Partida con dos jugadores activa
    FINISHED            // La partida ha terminado (ganador o empate)
}