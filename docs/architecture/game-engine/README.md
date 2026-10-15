**English** | [Português](README.pt-BR.md)

# Game engine

The part of the platform that runs matches inside each game server, such as `bedwars` and `duels`. Each game server runs many matches at the same time, so the engine's main job is to keep them isolated from each other.

Class names are decided when the engine is implemented. This document defines the concepts and the behavior they must have.

## Concepts

| Concept | What it is |
|---|---|
| Game mode | The rules of a game, such as BedWars solo or Duels 1v1. It also defines the mode's policies, described below. |
| Arena template | A map configured for a game mode: the original world and its spawn points, beds, generators and other settings. |
| Arena instance | A playable copy of a template, used by one match at a time. |
| Match | One game being played: its players, teams, arena instance and current state. |
| Participant | A player inside a match, with the data that only matters during that match. |

## Isolation between matches

Every arena instance runs in its own world. On top of that, the engine enforces:

- **One match per player.** At any moment a player belongs to at most one match, or to the game's lobby.
- **Events stay inside their match.** Every event involving a player, entity, block or world is routed to the match that owns it. An interaction between two different matches, such as damage between players of different matches, is cancelled.
- **Scoped communication.** Chat, the tab list and player visibility only include players of the same match.
- **Tasks belong to a match.** Every scheduled task is registered to its match and cancelled when the match closes, so nothing keeps running after it ends.
- **Failures stay inside their match.** An error inside a match cancels that match only, never the other matches or the server.

## Match lifecycle

```text
WAITING → STARTING → RUNNING → ENDING → CLOSED
   ↑          │
   └──────────┘   (not enough players)

any state → CANCELLED → CLOSED
```

| State | What happens |
|---|---|
| `WAITING` | The match accepts players until the mode's minimum is reached. |
| `STARTING` | Countdown. If players leave and the count drops below the minimum, the match goes back to `WAITING`. |
| `RUNNING` | The game is being played. No new players join, except returning players when the mode allows it. |
| `ENDING` | The winner is decided, the result and rewards are saved, and players see the outcome. |
| `CANCELLED` | The match was stopped by a failure or a shutdown. The mode's policy decides what happens to the players. |
| `CLOSED` | Players are sent back to the game's lobby, tasks are cancelled and the arena instance is released. |

State changes only happen through the lifecycle, never by setting the state directly. Each state defines which actions are allowed in it.

## Arena lifecycle

```text
AVAILABLE → RESERVED → IN_USE → RESETTING → AVAILABLE
                                    │
                                    └─→ DISABLED   (configuration error or corrupted template)
```

- A match reserves an available instance when it is created and releases it when it closes.
- After a match, the instance is reset from its template before it can be used again.
- **If a reset fails**, the instance is skipped and the reset is retried automatically. Other instances keep serving matches in the meantime.
- **Only when the failure is caused by a configuration error or a corrupted template** is the arena disabled. The error is logged and the staff is alerted, and the arena stays out of use until it is fixed.

## Policies defined by each game mode

The engine does not decide these rules. Each game mode defines its own:

| Policy | Examples of what a mode decides |
|---|---|
| Player count | Minimum to start and maximum per match. |
| Reconnection | Whether a player who disconnects during a match can come back, and for how long. |
| Leaving before the start | Whether a player who leaves during `WAITING` or `STARTING` gets any penalty. |
| Planned shutdown | Whether running matches are finished, cancelled or compensated when the server is stopped. |
| Crash | Whether players of matches lost in a crash are compensated. |

### Reconnection

When a mode allows reconnection, the platform records in Redis which match the player belongs to, with an expiration equal to the mode's reconnection window. A returning player is sent back to that match while the record exists.

### Shutdowns and crashes

- **Planned shutdown:** the server stops accepting new matches, applies each mode's shutdown policy to its running matches and only then stops.
- **Crash:** the state of running matches is lost, since it only lives in memory. A mode that compensates players after a crash records the start of each match in PostgreSQL. When the server starts again, it finds the matches that started and were never marked as finished or cancelled, and applies the mode's crash policy to them only.

## Results

The end of a match is saved in **one PostgreSQL transaction** that:

1. records the result, under the match's unique id;
2. grants the critical rewards, such as coins and MMR;
3. marks the match as finished.

Either all three are saved or none is. A crash can never leave rewards granted for a match that still looks unfinished, so crash compensation never pays twice. Saving the same match id again has no effect.

Rewards are shown to the players only after the transaction commits. Detailed statistics that are not critical are saved afterwards in batches, as described in [Persistence](../persistence/README.md).

## Rules

- **Match logic runs on the main thread.** Database, Redis and other slow work runs outside it, as described in [Persistence](../persistence/README.md).
- **Each game mode documents its policies** in its own feature documentation.
