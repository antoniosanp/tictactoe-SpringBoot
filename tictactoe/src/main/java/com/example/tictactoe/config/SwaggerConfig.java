package com.example.tictactoe.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Tictactoe",
                version = "1.0",
                description = "documentacion de los enpoint STOMP del juego"
        )
)
public class SwaggerConfig {
}
