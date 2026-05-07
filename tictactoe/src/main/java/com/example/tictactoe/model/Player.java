package com.example.tictactoe.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private String sessionId; // ID único de la conexión
    private String name;
    private Integer mark; // 1 para X, 2 para O

}
