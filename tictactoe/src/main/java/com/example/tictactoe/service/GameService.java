package com.example.tictactoe.service;

import com.example.tictactoe.model.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class GameService {

    @Autowired
    public Game game;

    boolean isWin(int mark){
        for (int i = 0; i <3; i++){
            if (Arrays.equals(game.getTablero()[i], new int[]{mark, mark, mark})){return  true;}
        }

        for (int i = 0; i <3; i++){
            if (game.getTablero()[i][0] == mark && game.getTablero()[i][1] == mark && game.getTablero()[i][2] == mark){return  true;}
        }

        if (game.getTablero()[0][0] == mark && game.getTablero()[1][1] == mark && game.getTablero()[2][2] == mark){return  true;}

        return game.getTablero()[0][2] == mark && game.getTablero()[1][1] == mark && game.getTablero()[2][0] == mark;
    }

    boolean validMove(int x, int y, int mark){

        if (x < 0 || x > 2 || y < 0 || y > 2) {return  false;}

        if (game.getTablero()[x][y] == 0){
            game.setMark(x,y,mark);
            return true;}

        return  false;
    }
}
