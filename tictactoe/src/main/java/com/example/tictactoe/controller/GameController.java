package com.example.tictactoe.controller;

import com.example.tictactoe.model.Game;
import com.example.tictactoe.model.GameMove;
import com.example.tictactoe.model.Player;
import com.example.tictactoe.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate; // Para enviar mensajes programáticamente

    public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/create")
    public void create(@Payload Player player, SimpMessageHeaderAccessor headerAccessor) {
        // El sessionId es vital para identificar al jugador en el Service
        player.setSessionId(headerAccessor.getSessionId());
        Game game = gameService.createGame(player);

        // Notificamos al creador sobre la partida creada
        messagingTemplate.convertAndSend("/topic/game-progress/" + game.getGameId(), game);
    }

    @MessageMapping("/move")
    public void move(@Payload GameMove move, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        String sessionId = headerAccessor.getSessionId();

        // Ejecutamos la lógica que definimos en el Service
        Game updatedGame = gameService.makeMove(move, sessionId);

        // Publicamos el estado actualizado a TODOS los suscritos a esa partida
        messagingTemplate.convertAndSend("/topic/game-progress/" + updatedGame.getGameId(), updatedGame);
    }
}