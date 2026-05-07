package com.example.tictactoe.service;

import com.example.tictactoe.model.Game;
import com.example.tictactoe.model.GameMove;
import com.example.tictactoe.model.Player;
import com.example.tictactoe.model.GameStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    // Almacén de partidas en memoria
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    /**
     * Crea una nueva instancia de Game y la registra en el mapa.
     */
    public Game createGame(Player player) {
        player.setMark(1);
        Game game = new Game();
        game.setPlayerX(player);
        game.setStatus(GameStatus.WAITING_FOR_PLAYER);
        game.setMessage("Esperando a que se una el jugador O");
        games.put(game.getGameId(), game);
        return game;
    }

    /**
     * Permite a un segundo jugador unirse a una partida existente.
     */
    public Game joinGame(String gameId, Player player) throws Exception {
        Game game = games.get(gameId);
        if (game == null) throw new Exception("La partida no existe");
        if (game.getPlayerO() != null) throw new Exception("La partida ya está llena");

        player.setMark(2);
        game.setPlayerO(player);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setMessage("Turno de " + game.getPlayerX().getName() + " (X)");
        return game;
    }

    public Game makeMove(GameMove move, String sessionId) throws Exception {
        Game game = games.get(move.gameId());

        if (game == null || game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new Exception("Movimiento no permitido: partida no válida o no iniciada");
        }

        // Determinar qué jugador está intentando mover
        Player currentPlayer = (game.getCurrentTurn() == 1) ? game.getPlayerX() : game.getPlayerO();

        // Validación de seguridad por SessionId
        if (!currentPlayer.getSessionId().equals(sessionId)) {
            throw new Exception("No es tu turno");
        }

        // Validación de la celda
        if (game.getBoard()[move.x()][move.y()] != 0) {
            throw new Exception("La casilla ya está ocupada");
        }

        // Aplicar movimiento
        game.getBoard()[move.x()][move.y()] = game.getCurrentTurn();

        // Verificar victoria o empate
        if (checkWinner(game.getBoard(), game.getCurrentTurn())) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinnerMark(game.getCurrentTurn());
            game.setWinnerName(currentPlayer.getName());
            game.setMessage("Ganó " + currentPlayer.getName() + " (" + markToSymbol(game.getCurrentTurn()) + ")");
        } else if (isBoardFull(game.getBoard())) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinnerMark(0);
            game.setWinnerName(null);
            game.setMessage("Empate");
        } else {
            // Cambiar turno: si era 1 pasa a 2, si era 2 pasa a 1
            game.setCurrentTurn(game.getCurrentTurn() == 1 ? 2 : 1);
            Player nextPlayer = game.getCurrentTurn() == 1 ? game.getPlayerX() : game.getPlayerO();
            game.setMessage("Turno de " + nextPlayer.getName() + " (" + markToSymbol(game.getCurrentTurn()) + ")");
        }

        return game;
    }

    private String markToSymbol(int mark) {
        return mark == 1 ? "X" : "O";
    }

    private boolean checkWinner(int[][] board, int mark) {
        // Comprobar filas, columnas y diagonales
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == mark && board[i][1] == mark && board[i][2] == mark) return true;
            if (board[0][i] == mark && board[1][i] == mark && board[2][i] == mark) return true;
        }
        if (board[0][0] == mark && board[1][1] == mark && board[2][2] == mark) return true;
        if (board[0][2] == mark && board[1][1] == mark && board[2][0] == mark) return true;

        return false;
    }

    private boolean isBoardFull(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

}
