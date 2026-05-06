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

import java.util.Map;

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

    @MessageMapping("/join")
    public void join(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        // 1. Extraer datos del payload (esperamos el gameId y el nombre del jugador)
        String gameId = payload.get("gameId");
        String playerName = payload.get("playerName");

        // 2. Crear el objeto Player para el que se une
        Player player = new Player();
        player.setName(playerName);
        player.setSessionId(headerAccessor.getSessionId()); // Importante: identificar su conexión

        // 3. Ejecutar la lógica en el Service
        Game updatedGame = gameService.joinGame(gameId, player);

        // 4. NOTIFICAR A AMBOS:
        // Al enviar a este topic, tanto el creador como el que se une recibirán
        // el objeto Game actualizado con el status IN_PROGRESS y el PlayerO asignado.
        messagingTemplate.convertAndSend("/topic/game-progress/" + gameId, updatedGame);
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