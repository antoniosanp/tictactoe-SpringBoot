package com.example.tictactoe.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

@Data
@Schema(description = "Modelo del juego o sala")
public class Game {
    @Schema(description = "ID unica de la sesion", example = "asdasd12313", accessMode = Schema.AccessMode.READ_ONLY)
    private String gameId;

    @Schema(description = "matriz 3x3 que representa el tablero")
    private int[][] board;
    private Player playerX;
    private Player playerO;
    private int currentTurn; // 1 para X, 2 para O
    private GameStatus status;
    private Integer winnerMark;
    private String winnerName;
    private String message;

    public Game() {
        this.gameId = UUID.randomUUID().toString();
        this.board = new int[3][3];
        this.currentTurn = 1; // Por regla de negocio, inicia X
        this.status = GameStatus.WAITING_FOR_PLAYER;
        this.message = "Esperando al segundo jugador";
    }
}
