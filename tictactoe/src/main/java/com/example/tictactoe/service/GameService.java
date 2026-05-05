package com.example.tictactoe.service;

import com.example.tictactoe.model.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private  final Map<String, Game> games = new ConcurrentHashMap<>();

    public Game JoinGame(String PlayerId){

        return  new Game(0);
    }
}
