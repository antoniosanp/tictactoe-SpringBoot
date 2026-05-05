package com.example.tictactoe.model;

public class Game {


    int[][] tablero = new int[3][3];
    int id;
    int whosTurn;

    public  Game(int id){
    this.id = id;

    this.tablero =
            new int[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
    }

    public void setMark(int x,  int y, int mark){
        tablero[x][y] = mark;
    }

    public void setWhosTurn(int whosTurn) {
        this.whosTurn = whosTurn;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTablero(int[][] tablero) {
        this.tablero = tablero;
    }

    public int[][] getTablero() {
        return tablero;
    }

    public int getId() {
        return id;
    }

    public int getWhosTurn() {
        return whosTurn;
    }
}
