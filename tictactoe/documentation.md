# Documentacion Backend - Tic Tac Toe

## 1. Vision general

Este proyecto implementa el backend de un juego de Tic Tac Toe usando Spring Boot y comunicacion en tiempo real sobre WebSocket con STOMP.

No es una API REST tradicional. El flujo principal del juego no usa endpoints HTTP tipo `GET` o `POST`, sino mensajes enviados por el cliente al servidor a destinos STOMP como:

- `/app/create`
- `/app/join`
- `/app/move`

Y respuestas o eventos publicados por el servidor en:

- `/topic/game-progress/{gameId}`
- `/user/queue/game-created`
- `/user/queue/errors`

La aplicacion mantiene el estado de las partidas en memoria, sin base de datos.

---

## 2. Arquitectura general

La arquitectura es simple y queda dividida en estas capas:

- **Bootstrap de Spring Boot**: arranque de la aplicacion.
- **Config**: configuracion de WebSocket/STOMP y metadatos OpenAPI.
- **Controller**: recibe mensajes STOMP del cliente y publica respuestas/eventos.
- **Service**: contiene la logica del juego.
- **Model**: representa el estado del dominio y los mensajes que entran/salen.

Estructura:

```text
com.example.tictactoe
├── TictactoeApplication.java
├── config
│   ├── SwaggerConfig.java
│   └── WebSocketConfig.java
├── controller
│   └── GameController.java
├── model
│   ├── Game.java
│   ├── GameMove.java
│   ├── GameStatus.java
│   └── Player.java
└── service
    └── GameService.java
```

---

## 3. Flujo de capas

### 3.1 Crear partida

1. El cliente envia un mensaje STOMP a `/app/create`.
2. `GameController.create(...)` recibe el payload.
3. El controller extrae el `sessionId` de la conexion WebSocket y lo asigna al jugador.
4. El controller llama a `GameService.createGame(...)`.
5. El service crea la partida, asigna al creador como jugador `X`, guarda la partida en memoria y devuelve el objeto `Game`.
6. El controller:
   - envia la partida creada por canal privado a `/user/queue/game-created`
   - publica tambien el estado de la sala en `/topic/game-progress/{gameId}`

### 3.2 Unirse a partida

1. El cliente envia un mensaje STOMP a `/app/join` con `gameId` y `playerName`.
2. `GameController.join(...)` construye un `Player` con el nombre recibido y el `sessionId` de la conexion.
3. Llama a `GameService.joinGame(...)`.
4. El service valida que la partida exista y que no tenga ya un segundo jugador.
5. Si todo es correcto, asigna al nuevo jugador como `O` y cambia el estado a `IN_PROGRESS`.
6. El controller publica el nuevo estado en `/topic/game-progress/{gameId}` para ambos clientes.

### 3.3 Hacer movimiento

1. El cliente envia un mensaje STOMP a `/app/move`.
2. `GameController.move(...)` obtiene el `sessionId` del socket.
3. Llama a `GameService.makeMove(...)`.
4. El service valida:
   - que la partida exista
   - que este en progreso
   - que el jugador correcto este intentando mover
   - que la casilla este libre
5. El service aplica el movimiento.
6. El service determina si hubo victoria, empate o cambio de turno.
7. El controller publica el `Game` actualizado en `/topic/game-progress/{gameId}`.

### 3.4 Manejo de errores

Si el service o controller lanza una `Exception` durante el procesamiento de un mensaje STOMP, `GameController.handleException(...)` la captura y responde por:

- `/user/queue/errors`

Esto permite que cada cliente reciba sus propios errores sin afectar al resto.

---

## 4. Clase principal

Archivo: [TictactoeApplication.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/TictactoeApplication.java)

```java
@SpringBootApplication
public class TictactoeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TictactoeApplication.class, args);
    }
}
```

### Que hace

Es el punto de entrada de la aplicacion.

### Anotaciones

- `@SpringBootApplication`
  - Es una anotacion compuesta.
  - Incluye:
    - `@Configuration`
    - `@EnableAutoConfiguration`
    - `@ComponentScan`
  - Le dice a Spring Boot que:
    - use configuracion Java
    - cargue autoconfiguraciones segun dependencias
    - escanee componentes dentro del paquete `com.example.tictactoe` y subpaquetes

### Metodo principal

- `SpringApplication.run(...)`
  - crea el contexto de Spring
  - registra beans
  - levanta el servidor embebido
  - arranca la aplicacion

---

## 5. Capa de configuracion

## 5.1 WebSocketConfig

Archivo: [WebSocketConfig.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/config/WebSocketConfig.java)

### Proposito

Configura la infraestructura WebSocket/STOMP del proyecto.

### Codigo relevante

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
```

### Anotaciones

- `@Configuration`
  - Marca la clase como clase de configuracion de Spring.
  - Su contenido se usa para registrar beans y comportamiento de infraestructura.

- `@EnableWebSocketMessageBroker`
  - Habilita el soporte de mensajeria WebSocket con broker STOMP.
  - Spring configura internamente:
    - canales de entrada y salida
    - resolucion de destinos
    - soporte para `@MessageMapping`
    - broker simple en memoria, si se configura

### Interface implementada

- `WebSocketMessageBrokerConfigurer`
  - Permite personalizar la configuracion del broker y endpoints.

### Metodo `configureMessageBroker`

```java
config.enableSimpleBroker("/topic", "/queue");
config.setApplicationDestinationPrefixes("/app");
config.setUserDestinationPrefix("/user");
```

#### `enableSimpleBroker("/topic", "/queue")`

Activa el broker simple en memoria de Spring para destinos de salida.

- `/topic`
  - se usa para mensajes publicados a multiples clientes
  - en este proyecto, para el estado publico de una partida

- `/queue`
  - se usa para mensajes dirigidos a un usuario concreto
  - en este proyecto, para notificaciones privadas y errores

#### `setApplicationDestinationPrefixes("/app")`

Define el prefijo de destinos que el cliente usa para enviar mensajes al backend.

Ejemplo:

- el cliente envia a `/app/create`
- Spring lo enruta al metodo anotado con `@MessageMapping("/create")`

#### `setUserDestinationPrefix("/user")`

Define el prefijo para destinos privados por usuario o sesion.

Ejemplo:

- el servidor envia a `/user/queue/game-created`
- Spring resuelve internamente a la sesion correcta

### Metodo `registerStompEndpoints`

```java
registry.addEndpoint("/game-websocket")
        .setAllowedOriginPatterns("*");
registry.addEndpoint("/game-websocket")
        .setAllowedOriginPatterns("*")
        .withSockJS();
```

#### Que registra

Expone el endpoint HTTP que inicia el handshake WebSocket.

- Endpoint: `/game-websocket`

#### Version nativa

```java
registry.addEndpoint("/game-websocket")
```

Permite clientes WebSocket nativos.

#### Version SockJS

```java
registry.addEndpoint("/game-websocket").withSockJS();
```

Agrega compatibilidad con SockJS, que sirve como fallback cuando el cliente no usa WebSocket puro o cuando se quiere simplificar la conexion desde ciertos navegadores/librerias.

#### `setAllowedOriginPatterns("*")`

Permite conexiones desde cualquier origen.

Esto es comodo para pruebas y desarrollo, pero en un entorno real convendria restringir dominios.

---

## 5.2 SwaggerConfig

Archivo: [SwaggerConfig.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/config/SwaggerConfig.java)

### Proposito

Define metadatos OpenAPI de la aplicacion.

### Codigo relevante

```java
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
```

### Anotaciones

- `@Configuration`
  - Indica que la clase forma parte de la configuracion de Spring.

- `@OpenAPIDefinition`
  - Anotacion de `springdoc-openapi`.
  - Permite describir metadata general de la API.

- `@Info`
  - Define titulo, version y descripcion visibles en la documentacion OpenAPI.

### Observacion importante

Este proyecto usa principalmente STOMP sobre WebSocket. OpenAPI/Swagger documenta mucho mejor APIs HTTP que mensajeria STOMP, asi que esta configuracion sirve mas como metadata general que como documentacion completa del contrato de juego en tiempo real.

---

## 6. Capa de modelos

En este proyecto no hay una separacion estricta entre entidades, DTOs de salida y DTOs de entrada.

En la practica:

- `Game` funciona como modelo de dominio y tambien como payload de salida.
- `Player` funciona como modelo de dominio y tambien como payload de entrada/salida.
- `GameMove` actua como DTO de entrada para movimientos.
- En `join`, el payload de entrada ni siquiera usa un DTO dedicado, sino `Map<String, String>`.

Eso hace el proyecto mas simple, aunque mezcla responsabilidades.

## 6.1 Game

Archivo: [Game.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/model/Game.java)

### Rol

Representa una partida completa.

### Campos

- `String gameId`
  - identificador unico de la partida
  - se genera automaticamente en el constructor usando `UUID`

- `int[][] board`
  - tablero de 3x3
  - valores esperados:
    - `0`: celda vacia
    - `1`: marca X
    - `2`: marca O

- `Player playerX`
  - jugador creador

- `Player playerO`
  - segundo jugador

- `int currentTurn`
  - turno actual
  - `1` para X
  - `2` para O

- `GameStatus status`
  - estado actual de la partida

- `Integer winnerMark`
  - marca ganadora
  - `1` o `2`
  - `0` en caso de empate
  - `null` mientras no haya resultado

- `String winnerName`
  - nombre del ganador
  - `null` si no existe aun o si hubo empate

- `String message`
  - mensaje de estado para el cliente
  - ejemplo: turno actual, empate, ganador

### Constructor

```java
public Game() {
    this.gameId = UUID.randomUUID().toString();
    this.board = new int[3][3];
    this.currentTurn = 1;
    this.status = GameStatus.WAITING_FOR_PLAYER;
    this.message = "Esperando al segundo jugador";
}
```

Inicializa la partida con reglas base:

- se genera ID
- tablero vacio
- empieza X
- el juego arranca esperando segundo jugador

### Anotaciones

- `@Data` de Lombok
  - genera automaticamente:
    - getters
    - setters
    - `toString()`
    - `equals()`
    - `hashCode()`

- `@Schema`
  - agrega metadata para documentacion OpenAPI

#### `@Schema` en `gameId`

```java
@Schema(description = "ID unica de la sesion", example = "asdasd12313", accessMode = Schema.AccessMode.READ_ONLY)
```

Indica que:

- el campo tiene una descripcion
- aparece un ejemplo
- es de solo lectura desde el punto de vista del cliente

#### `@Schema` en `board`

Agrega descripcion del tablero a la documentacion.

## 6.2 Player

Archivo: [Player.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/model/Player.java)

### Rol

Representa un jugador conectado a una partida.

### Campos

- `String sessionId`
  - identificador de la sesion WebSocket del cliente
  - se usa para validar turnos y enviar mensajes privados

- `String name`
  - nombre visible del jugador

- `Integer mark`
  - marca asignada al jugador
  - `1` para X
  - `2` para O

### Por que `Integer` y no `int`

Se usa `Integer` porque en la entrada JSON puede llegar `null` o no venir el campo. Un `int` primitivo no acepta `null` al deserializar.

### Anotaciones

- `@Data`
  - genera getters/setters y metodos utilitarios

- `@AllArgsConstructor`
  - genera constructor con todos los campos

- `@NoArgsConstructor`
  - genera constructor vacio
  - util para deserializacion y construccion flexible

## 6.3 GameMove

Archivo: [GameMove.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/model/GameMove.java)

### Rol

Es el payload de entrada para un movimiento.

### Estructura

```java
public record GameMove(
    String gameId,
    int x,
    int y
) {}
```

### Significado

- `gameId`: partida objetivo
- `x`: fila
- `y`: columna

### Por que es un `record`

Un `record` en Java es util cuando solo se necesita una estructura inmutable y compacta de datos.

Ventajas aqui:

- menos codigo ceremonioso
- acceso directo por `move.gameId()`, `move.x()`, `move.y()`
- buen ajuste para payloads de entrada pequenos

## 6.4 GameStatus

Archivo: [GameStatus.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/model/GameStatus.java)

### Rol

Enum que modela los estados permitidos de una partida.

### Valores

- `WAITING_FOR_PLAYER`
  - solo existe el jugador creador

- `IN_PROGRESS`
  - hay dos jugadores y la partida esta activa

- `FINISHED`
  - la partida termino por victoria o empate

### Ventaja del enum

Evita usar strings sueltos para estado y reduce errores de escritura o comparacion.

---

## 7. Controller

Archivo: [GameController.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/controller/GameController.java)

### Rol

Es la capa de entrada de mensajes STOMP. Recibe eventos del cliente, delega la logica al service y publica respuestas.

### Anotacion principal

- `@Controller`
  - registra la clase como componente Spring
  - en este caso, no se usa como MVC HTML clasico, sino como controller de mensajeria

### Dependencias inyectadas

```java
private final GameService gameService;
private final SimpMessagingTemplate messagingTemplate;
```

#### `GameService`

Contiene la logica del juego.

#### `SimpMessagingTemplate`

API de Spring para enviar mensajes STOMP programaticamente a destinos como:

- topics
- queues
- destinos privados por usuario

### Constructor

```java
public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate)
```

Spring lo usa para inyeccion de dependencias. Al haber un solo constructor, no hace falta `@Autowired`.

## 7.1 Metodo `create`

```java
@MessageMapping("/create")
public void create(@Payload Player player, SimpMessageHeaderAccessor headerAccessor)
```

### Anotaciones y parametros

- `@MessageMapping("/create")`
  - equivalente STOMP de un endpoint
  - atiende mensajes enviados a `/app/create`

- `@Payload Player player`
  - le dice a Spring que deserialice el cuerpo del mensaje a `Player`

- `SimpMessageHeaderAccessor headerAccessor`
  - permite leer metadatos del mensaje y de la sesion
  - aqui se usa para obtener el `sessionId`

### Logica

1. se asigna el `sessionId` al jugador
2. se crea la partida en el service
3. se responde al usuario creador por `/user/queue/game-created`
4. se publica el estado de la partida en `/topic/game-progress/{gameId}`

### Por que existe la respuesta privada

Antes de conocer el `gameId`, el cliente no puede suscribirse al topic de esa partida. Por eso se envia primero una notificacion privada con el `Game` creado.

## 7.2 Metodo `join`

```java
@MessageMapping("/join")
public void join(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) throws Exception
```

### Entrada

Recibe un `Map<String, String>` con:

- `gameId`
- `playerName`

### Observacion de diseño

Esto funciona, pero no es el diseño mas limpio. Un DTO dedicado, por ejemplo `JoinGameRequest`, seria mas claro, tipado y mantenible.

### Logica

1. extrae `gameId` y `playerName`
2. construye manualmente un `Player`
3. asigna `sessionId`
4. llama al service
5. publica el juego actualizado en el topic de la partida

## 7.3 Metodo `move`

```java
@MessageMapping("/move")
public void move(@Payload GameMove move, SimpMessageHeaderAccessor headerAccessor) throws Exception
```

### Entrada

- `GameMove move`: payload del movimiento
- `headerAccessor`: para identificar la sesion que realiza el movimiento

### Logica

1. obtiene `sessionId`
2. delega al service
3. publica el estado actualizado al topic de la partida

## 7.4 Manejo de errores

```java
@MessageExceptionHandler
@SendToUser("/queue/errors")
public Map<String, String> handleException(Exception exception)
```

### Anotaciones

- `@MessageExceptionHandler`
  - intercepta excepciones lanzadas durante la gestion de mensajes STOMP en este controller

- `@SendToUser("/queue/errors")`
  - envia el resultado del metodo solo al usuario que origino el mensaje

### Comportamiento

Convierte la excepcion en un payload simple:

```json
{ "message": "texto del error" }
```

## 7.5 Metodo auxiliar `createHeaders`

```java
private MessageHeaders createHeaders(String sessionId)
```

### Para que sirve

Construye cabeceras de mensajeria con el `sessionId` necesario para `convertAndSendToUser(...)`.

### Elementos usados

- `SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE)`
  - crea un accesor de cabeceras para un mensaje STOMP saliente

- `setSessionId(sessionId)`
  - asocia la salida con una sesion concreta

- `setLeaveMutable(true)`
  - permite que las cabeceras permanezcan mutables hasta que Spring termine de procesarlas

---

## 8. Service

Archivo: [GameService.java](/home/antonio/Desktop/tictactoe/tictactoe/src/main/java/com/example/tictactoe/service/GameService.java)

### Rol

Contiene toda la logica de negocio del juego.

### Anotacion

- `@Service`
  - registra la clase como bean de servicio dentro del contexto Spring
  - expresa semanticamente que aqui vive la logica de negocio

## 8.1 Estado en memoria

```java
private final Map<String, Game> games = new ConcurrentHashMap<>();
```

### Que representa

Es el almacenamiento de partidas activas.

- clave: `gameId`
- valor: objeto `Game`

### Por que `ConcurrentHashMap`

El backend puede atender multiples conexiones concurrentes. `ConcurrentHashMap` ofrece operaciones seguras para acceso concurrente sin usar sincronizacion manual basica para lectura/escritura del mapa.

### Limitaciones

- no persiste datos
- al reiniciar la app, se pierden las partidas
- no resuelve por si solo toda la concurrencia fina dentro de cada objeto `Game`

## 8.2 Metodo `createGame`

```java
public Game createGame(Player player)
```

### Responsabilidades

- asigna marca `1` al creador
- crea la instancia de `Game`
- asigna al jugador como `playerX`
- deja el estado en espera
- guarda la partida en memoria
- devuelve el `Game`

### Regla de negocio

El primer jugador siempre es `X`.

## 8.3 Metodo `joinGame`

```java
public Game joinGame(String gameId, Player player) throws Exception
```

### Validaciones

- si no existe la partida: error
- si ya hay segundo jugador: error

### Acciones

- asigna marca `2`
- lo guarda como `playerO`
- cambia estado a `IN_PROGRESS`
- actualiza mensaje del turno inicial

### Regla de negocio

El segundo jugador siempre es `O`.

## 8.4 Metodo `makeMove`

```java
public Game makeMove(GameMove move, String sessionId) throws Exception
```

Es el metodo central del juego.

### Paso 1: obtener partida

```java
Game game = games.get(move.gameId());
```

Busca la partida correspondiente al movimiento.

### Paso 2: validar estado del juego

```java
if (game == null || game.getStatus() != GameStatus.IN_PROGRESS)
```

Solo se puede mover si:

- la partida existe
- esta en progreso

### Paso 3: determinar jugador actual

```java
Player currentPlayer = (game.getCurrentTurn() == 1) ? game.getPlayerX() : game.getPlayerO();
```

Si el turno actual es `1`, mueve `playerX`. Si es `2`, mueve `playerO`.

### Paso 4: validar identidad por sesion

```java
if (!currentPlayer.getSessionId().equals(sessionId))
```

Esto evita que un cliente juegue por el otro jugador.

### Paso 5: validar casilla libre

```java
if (game.getBoard()[move.x()][move.y()] != 0)
```

Solo se permite mover en celdas vacias.

### Paso 6: aplicar movimiento

```java
game.getBoard()[move.x()][move.y()] = game.getCurrentTurn();
```

Se escribe `1` o `2` en la casilla.

### Paso 7: verificar resultado

#### Caso victoria

```java
if (checkWinner(game.getBoard(), game.getCurrentTurn()))
```

Acciones:

- estado `FINISHED`
- se guarda `winnerMark`
- se guarda `winnerName`
- se actualiza `message`

#### Caso empate

```java
else if (isBoardFull(game.getBoard()))
```

Acciones:

- estado `FINISHED`
- `winnerMark = 0`
- `winnerName = null`
- mensaje `"Empate"`

#### Caso turno siguiente

Si no hubo victoria ni empate:

- alterna `currentTurn`
- calcula el siguiente jugador
- actualiza `message`

## 8.5 Metodo `markToSymbol`

```java
private String markToSymbol(int mark)
```

Convierte:

- `1` -> `X`
- `2` -> `O`

Se usa para construir mensajes legibles.

## 8.6 Metodo `checkWinner`

```java
private boolean checkWinner(int[][] board, int mark)
```

### Que hace

Comprueba si una marca gano la partida.

### Logica

Revisa:

- 3 filas
- 3 columnas
- diagonal principal
- diagonal secundaria

Si encuentra tres posiciones consecutivas con la misma marca, retorna `true`.

## 8.7 Metodo `isBoardFull`

```java
private boolean isBoardFull(int[][] board)
```

Recorre todo el tablero. Si encuentra algun `0`, aun hay movimientos posibles. Si no encuentra ninguno, el tablero esta lleno.

---

## 9. Flujo completo de mensajeria STOMP

### Crear partida

Cliente envia:

```text
Destination: /app/create
Payload: { "name": "Antonio" }
```

Servidor responde al creador:

```text
Destination: /user/queue/game-created
Payload: Game
```

Servidor publica estado:

```text
Destination: /topic/game-progress/{gameId}
Payload: Game
```

### Unirse a partida

Cliente envia:

```text
Destination: /app/join
Payload: { "gameId": "...", "playerName": "Laura" }
```

Servidor publica:

```text
Destination: /topic/game-progress/{gameId}
Payload: Game actualizado
```

### Hacer movimiento

Cliente envia:

```text
Destination: /app/move
Payload: { "gameId": "...", "x": 0, "y": 2 }
```

Servidor publica:

```text
Destination: /topic/game-progress/{gameId}
Payload: Game actualizado
```

### Error

Si una operacion falla:

```text
Destination: /user/queue/errors
Payload: { "message": "..." }
```

---

## 10. Anotaciones de Spring y librerias usadas

## 10.1 Anotaciones de Spring Boot / Core

- `@SpringBootApplication`
  - arranque y autoconfiguracion

- `@Configuration`
  - declara clases de configuracion

- `@Controller`
  - registra controller de Spring

- `@Service`
  - registra servicio de negocio

## 10.2 Anotaciones de mensajeria STOMP

- `@EnableWebSocketMessageBroker`
  - habilita mensajeria WebSocket/STOMP

- `@MessageMapping`
  - mapea destinos STOMP entrantes a metodos Java

- `@Payload`
  - indica que el contenido del mensaje debe deserializarse al parametro

- `@MessageExceptionHandler`
  - captura excepciones de procesamiento de mensajes

- `@SendToUser`
  - envia la respuesta al usuario que origino la interaccion

## 10.3 Anotaciones de Lombok

- `@Data`
  - getters, setters, `equals`, `hashCode`, `toString`

- `@NoArgsConstructor`
  - constructor vacio

- `@AllArgsConstructor`
  - constructor completo

## 10.4 Anotaciones de OpenAPI

- `@OpenAPIDefinition`
  - metadata general de API

- `@Info`
  - titulo, version, descripcion

- `@Schema`
  - documentacion de campos/modelos

---

## 11. Decisiones de diseño relevantes

## 11.1 Estado en memoria

El sistema evita una base de datos y prioriza simplicidad.

Ventaja:

- implementacion rapida

Costo:

- sin persistencia
- no es adecuado para produccion real si se requiere durabilidad

## 11.2 Identidad por `sessionId`

La seguridad del turno se apoya en el `sessionId` de la conexion WebSocket, no solo en datos enviados por el cliente.

Ventaja:

- el cliente no puede simplemente mentir sobre que jugador es

## 11.3 Modelos reutilizados como payload

El sistema no separa claramente dominio y DTOs.

Ventaja:

- menos clases

Costo:

- acopla mas el contrato externo con la estructura interna
- dificulta evolucionar el backend sin impactar clientes

## 11.4 `Map<String, String>` en `join`

Es una solucion funcional pero debil en tipado.

Una opcion mas solida seria:

```java
public record JoinGameRequest(String gameId, String playerName) {}
```

---

## 12. Limitaciones actuales del backend

Estas limitaciones no impiden que funcione, pero conviene entenderlas:

- no hay persistencia de partidas
- no hay autenticacion real de usuarios
- no hay validacion explicita de rangos para `x` y `y`
- no hay bloqueo fino por partida frente a movimientos simultaneos extremos
- `join` usa `Map` en lugar de DTO tipado
- las excepciones son genericas (`Exception`) en vez de tipos mas precisos

---

## 13. Resumen tecnico

El backend implementa un juego en tiempo real usando Spring WebSocket con STOMP. `GameController` actua como punto de entrada de mensajes, `GameService` concentra la logica de negocio, `WebSocketConfig` define la infraestructura de mensajeria, y los modelos `Game`, `Player`, `GameMove` y `GameStatus` representan el estado y los mensajes del juego.

La aplicacion esta pensada para un caso simple y funcional:

- partidas en memoria
- dos jugadores por sala
- validacion por sesion WebSocket
- publicacion de estado en tiempo real
- manejo de errores por canal privado

Si el siguiente paso es profesionalizar el backend, lo razonable seria separar DTOs, agregar validaciones, definir excepciones propias y mover el almacenamiento a persistencia real.
