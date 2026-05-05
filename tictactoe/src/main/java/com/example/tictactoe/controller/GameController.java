package com.example.tictactoe.controller;


import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {


    @MessageMapping
    public void handleMove(@Payload MoveRequest request, SimpMessageHeaderAccessor headerAccessor){
        String sessionId = headerAccessor.getSessionId();
    }
}
