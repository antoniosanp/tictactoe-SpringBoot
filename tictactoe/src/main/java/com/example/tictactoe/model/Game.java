package com.example.tictactoe.model;

public class Game {
    private String gameId;
    private int[][] tablero = new int[3][3];
    private Player player1;
    private Player player2;
    private int turn; // ID del jugador que le toca
    // ...
}