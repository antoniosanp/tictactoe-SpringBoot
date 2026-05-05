package com.example.tictactoe.model;

public record GameMove(
        String gameId, 
        int x,         // Fila
        int y          // Columna
) {
}