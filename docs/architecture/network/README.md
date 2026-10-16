**English** | [Português](README.pt-BR.md)

# Network architecture

How the HashMC network is split into servers, today and in the plan for the future.

## Phase 1: one server per game

```text
                 Velocity proxy
                       ↓
                   Main lobby
                       ↓
        BedWars              |  Duels
        BedWars lobby        |  Duels lobby
        Arena #1             |  Duels #1
        Arena #2             |  Duels #2
```

The arrows show the player's path. Technically every connection goes through the proxy, which moves the player from one server to another.

The network starts with five processes: one Velocity and four Paper servers.

| Process | Software | Role |
|---|---|---|
| `velocity` | Velocity | Entry point. Receives every connection and moves players between servers. |
| `auth` | Paper | Authentication server. Its role will be detailed when it is implemented. |
| `lobby` | Paper | Main lobby. Where players arrive and choose a game. |
| `bedwars` | Paper | The BedWars lobby and its arenas, running in the same server. |
| `duels` | Paper | The Duels lobby and its matches, running in the same server. |

Every process connects to the shared infrastructure:

| Service | Role |
|---|---|
| PostgreSQL | Database and source of truth. See [Persistence](../persistence/README.md). |
| Redis | Shared cache and transport for packets between servers. See [Persistence](../persistence/README.md) and [Messaging](../messaging/README.md). |

### Why each game runs in its own server

- **Isolation:** each Paper server has its own TPS. A heavy problem in BedWars does not slow down Duels, and a crash in one game does not take the others down.
- **Independent restarts:** one game can be updated and restarted without kicking players from the others.

### Scaling a game

When a game gets popular, more servers run the same game logic side by side:

```text
bedwars-01   bedwars-02   bedwars-03
```

Each one is a full copy of the `bedwars` server, with its own BedWars lobby and arenas. The proxy spreads players between them.

## Phase 2: game lobbies separated from game servers

This is the plan for the future, not what runs today.

```text
                 Velocity proxy
                       ↓
                   Main lobby
                       ↓
        BedWars lobby        |  Duels lobby
              ↓              |       ↓
        bedwars-game-01      |  duels-game-01
        bedwars-game-02      |  duels-game-02
        bedwars-game-N       |  duels-game-N
```

Each game's lobby becomes a server of its own, and the matches run on dedicated game servers.

- **Independent scaling:** more game servers can be started without touching the lobbies.
- **Lighter lobbies:** lobbies only hold players waiting for a match, so they stay fast even when many matches are running.
- **Contained restarts:** restarting a game server only affects the matches running on it, not the players waiting in the lobby.

### Not decided yet

These points will be detailed when phase 2 starts:

- How a game lobby sends players to a game server with a free slot.
- How game servers are started and stopped as demand changes.
- How the servers share data, such as player profiles and match state.
