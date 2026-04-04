# Game-RussianRoulette-Service

Microservicio de juego **Liar's Bar** para la plataforma ECIBET. Implementa la lógica completa del juego de cartas con ruleta rusa, comunicación en tiempo real mediante WebSockets y gestión de salas multijugador.

---

## Descripción del juego

Liar's Bar es un juego de cartas por turnos donde los jugadores deben declarar qué cartas juegan, pudiendo mentir. El siguiente jugador puede creerles o acusarlos de mentir. El perdedor de cada acusación enfrenta una ruleta rusa. El último jugador vivo gana el pozo.

### Mecánica

- Cada ronda tiene una **carta activa** (KING, ACE o QUEEN) que alterna entre rondas.
- En su turno, el jugador selecciona entre 1 y 3 cartas y declara cuántas y de qué tipo son.
- El siguiente jugador puede **creer** (jugar sus propias cartas) o **acusar** de mentira.
- Si la acusación es correcta → el que mintió enfrenta el revólver.
- Si la acusación es incorrecta → el que acusó enfrenta el revólver.
- El revólver tiene 1 bala en 6 posiciones. Las probabilidades aumentan con cada disparo fallido.
- Si un jugador se queda sin cartas y no es acusado (o es acusado pero decía la verdad y sobrevive) → **gana la partida**.

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.2.0 | Framework base |
| Spring WebSocket + STOMP | — | Comunicación en tiempo real |
| Spring WebFlux | — | Cliente HTTP reactivo hacia Wallets-Service |
| Resilience4j | 2.1.0 | Circuit breaker para llamadas externas |
| Lombok | — | Reducción de boilerplate |
| Maven | 3.x | Gestión de dependencias |

---

## Arquitectura

El estado del juego se mantiene **en memoria** mediante `RoomManager`. No requiere base de datos.

---

## Endpoints REST

### Lobby

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/games/liars-bar/rooms` | Listar todas las salas disponibles |
| `GET` | `/api/games/liars-bar/rooms/{roomId}` | Obtener detalle de una sala |
| `POST` | `/api/games/liars-bar/rooms` | Crear una nueva sala |
| `POST` | `/api/games/liars-bar/rooms/{roomId}/join` | Unirse a una sala |
| `DELETE` | `/api/games/liars-bar/rooms/{roomId}/leave` | Salir de una sala |

### Juego

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/games/liars-bar/rooms/{roomId}/state` | Obtener estado actual de la partida |
| `GET` | `/api/games/liars-bar/rooms/{roomId}/hand` | Obtener las cartas del jugador (`?userId=`) |

---

## WebSocket (STOMP)

**Endpoint de conexión:** `ws://localhost:8091/ws`

### Mensajes del cliente → servidor

| Destino | Payload | Descripción |
|---|---|---|
| `/app/room/{roomId}/start` | `String userId` | Iniciar partida (solo el host) |
| `/app/room/{roomId}/play` | `PlayCardsRequest` | Jugar cartas |
| `/app/room/{roomId}/accuse` | `AccuseRequest` | Acusar al jugador anterior |
| `/app/room/{roomId}/shoot` | `String userId` | Jalar el gatillo |

### Suscripciones del cliente

| Topic | Descripción |
|---|---|
| `/topic/room/{roomId}` | Eventos del juego en tiempo real |
| `/topic/lobby` | Actualizaciones de la lista de salas |

### Tipos de eventos (`GameStateResponse.type`)

| Tipo | Descripción |
|---|---|
| `GAME_STARTED` | La partida comenzó |
| `CARDS_PLAYED` | Un jugador jugó cartas |
| `ACCUSED` | Un jugador fue acusado, se revelan las cartas |
| `SHOT_RESULT` | Resultado del disparo (eliminado o sobrevivió) |
| `GAME_OVER` | La partida terminó, hay un ganador |
| `LOBBY_UPDATE` | Cambio en la lista de salas |
| `ERROR` | Error en una acción |

---

## Configuración

```yaml
ecibet:
  wallets-service:
    url: ${WALLETS_SERVICE_URL:http://localhost:8082}
  game:
    max-rooms: 5
    max-players-per-room: 4
    min-players-to-start: 2
    turn-timer-seconds: 30
    default-buy-in: 10000
```

| Variable de entorno | Valor por defecto | Descripción |
|---|---|---|
| `PORT` | `8091` | Puerto del servidor |
| `WALLETS_SERVICE_URL` | `http://localhost:8082` | URL del servicio de billeteras |

---

## Mazo de cartas

El mazo contiene **30 cartas** distribuidas así:

| Carta | Cantidad | Descripción |
|---|---|---|
| KING | 8 | Carta regular |
| ACE | 8 | Carta regular |
| QUEEN | 8 | Carta regular |
| JOKER | 6 | Comodín, vale como cualquier carta declarada |

Con un máximo de 4 jugadores (5 cartas cada uno = 20 cartas), el mazo tiene suficiente margen.

---

## Ejecución local

### Prerrequisitos

- Java 17+
- Maven 3.x

### Pasos

```bash
# Clonar el repositorio
git clone <repo-url>
cd Game-RussianRoulette-Service

# Compilar
mvn clean install -DskipTests

# Ejecutar
mvn spring-boot:run
```

El servicio estará disponible en `http://localhost:8091`.

### Con Docker

```bash
docker-compose up -d
```

> **Nota:** El `docker-compose.yml` requiere que la imagen `ecibet-wallets-service:latest` esté construida localmente.

---

## Dependencias con otros servicios

| Servicio | Puerto | Obligatorio | Descripción |
|---|---|---|---|
| Wallets-Service | 8082 | No* | Débito/crédito de buy-in y premios |

> *Las llamadas a Wallets-Service están deshabilitadas en desarrollo (`[DEV] Skipping wallet debit/credit`). El juego funciona sin él.

---

## Estructura del proyecto

```
src/main/java/com/ecibet/russianroulette/
├── config/
│   ├── CorsConfig.java
│   ├── GlobalExceptionHandler.java
│   └── WebSocketConfig.java
├── controller/
│   └── LobbyController.java
├── dto/
│   ├── request/
│   │   ├── AccuseRequest.java
│   │   ├── CreateRoomRequest.java
│   │   ├── JoinRoomRequest.java
│   │   └── PlayCardsRequest.java
│   └── response/
│       ├── GameStateResponse.java
│       └── RoomResponse.java
├── model/
│   ├── Card.java
│   ├── GameState.java
│   ├── LastPlay.java
│   ├── Player.java
│   ├── Revolver.java
│   ├── Room.java
│   └── RoomStatus.java
├── service/
│   ├── DeckService.java
│   ├── GameService.java
│   ├── RoomManager.java
│   └── TurnTimerService.java
├── websocket/
│   └── GameWebSocketHandler.java
└── EcibetRussianRouletteApplication.java
```

---

## Autores

Desarrollado como parte del proyecto académico **ECIBET** — Plataforma de apuestas en línea.
