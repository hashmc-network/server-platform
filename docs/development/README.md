**English** | [Português](README.pt-BR.md)

# Development

How the project is organized, how dependencies are declared and how to build it.

## Requirements

- **JDK 25.** If it is not installed, Gradle downloads it automatically the first time it builds.
- Nothing else. The Gradle wrapper (`gradlew`) downloads the right Gradle version by itself.

## Building

```bash
./gradlew build
```

On Windows, use `gradlew.bat build`.

This compiles every module, runs the tests, checks the dependency rules and assembles one folder per server:

```text
build/servers/
├── proxy/plugins/    hashmc-proxy.jar
├── auth/plugins/     hashmc-core.jar  hashmc-auth.jar
├── lobby/plugins/    hashmc-core.jar  hashmc-lobby.jar
├── bedwars/plugins/  hashmc-core.jar  hashmc-bedwars.jar
└── duels/plugins/    hashmc-core.jar  hashmc-duels.jar
```

Each folder holds exactly the jars of that server: copy its contents into the server's `plugins` folder. Jar names carry no version, so a new build replaces the old file. The version is inside each plugin's `plugin.yml` or `velocity-plugin.json`.

| Command | What it does |
|---|---|
| `./gradlew build` | Everything: compile, test, check rules and assemble every server. |
| `./gradlew assembleServers` | Assembles every server folder, without running tests. |
| `./gradlew assembleLobbyServer` | Assembles a single server. There is one task per server: `assembleProxyServer`, `assembleAuthServer`, `assembleBedwarsServer`, `assembleDuelsServer`. |
| `./gradlew :games:bedwars:build` | Builds and tests a single module. |

## Modules

```text
server-platform/
├── build-logic/          build conventions shared by every module
├── common/
│   ├── network/          packet contracts between servers
│   └── model/            models shared between servers
├── platform/
│   ├── persistence/      PostgreSQL and Redis cache
│   ├── messaging/        packet transport over Redis
│   ├── core/             base plugin of every Paper server
│   └── proxy/            Velocity plugin
├── game-engine/          matches, arenas, teams and participants
├── games/
│   ├── bedwars/
│   └── duels/
└── servers/
    ├── lobby/
    └── auth/
```

| Module | Kind | What it holds | Where it ends up |
|---|---|---|---|
| `common:network` | Library | Packet contracts, as described in [Messaging](../architecture/messaging/README.md). | Inside `hashmc-core.jar` and `hashmc-proxy.jar` |
| `common:model` | Library | Models shared between servers. | Inside `hashmc-core.jar` and `hashmc-proxy.jar` |
| `platform:persistence` | Library | PostgreSQL and the Redis cache, as described in [Persistence](../architecture/persistence/README.md). | Inside `hashmc-core.jar` and `hashmc-proxy.jar` |
| `platform:messaging` | Library | Packet transport over Redis. | Inside `hashmc-core.jar` and `hashmc-proxy.jar` |
| `platform:core` | Paper plugin | The base of every Paper server. Offers the shared services to the other plugins. | `hashmc-core.jar` |
| `platform:proxy` | Velocity plugin | The proxy plugin. | `hashmc-proxy.jar` |
| `game-engine` | Library that uses the Paper API | The [game engine](../architecture/game-engine/README.md). | Inside each game's jar |
| `games:bedwars`, `games:duels` | Paper plugin | One game each. | `hashmc-bedwars.jar`, `hashmc-duels.jar` |
| `servers:lobby`, `servers:auth` | Paper plugin | What is specific to the lobby and to the authentication server. | `hashmc-lobby.jar`, `hashmc-auth.jar` |

Packages follow the module: `br.com.hashmc.core`, `br.com.hashmc.bedwars`, `br.com.hashmc.engine` and so on.

## Servers

Which jars go to each server is declared in one place, the root [`build.gradle.kts`](../../build.gradle.kts):

```kotlin
network {
    server("proxy", projects.platform.proxy)
    server("lobby", projects.platform.core, projects.servers.lobby)
    server("bedwars", projects.platform.core, projects.games.bedwars)
}
```

Adding a server to the build is one line there. Its `assemble<Name>Server` task is created automatically.

## Dependency rules

Each module may only depend on the groups below. The build enforces these rules: a forbidden dependency fails the build with a message saying what is allowed.

| Module | May depend on |
|---|---|
| `common:*` | `common` |
| `platform:persistence`, `platform:messaging` | `common` |
| `platform:core`, `platform:proxy` | `common`, `platform:persistence`, `platform:messaging` |
| `game-engine` | `common`, `platform:core` |
| `games:*` | `common`, `platform:core`, `game-engine` |
| `servers:*` | `common`, `platform:core` |

What these rules guarantee:

- **Nothing depends on a game, a server plugin or the proxy.** They are the end of the chain.
- **Games and server plugins never depend on each other.** Removing or changing one never breaks another.
- **`common` knows nothing about the rest.** It can be used by any module, on any platform.
- **The core is never bundled twice.** Games and server plugins use it with `compileOnly`, since the core is already on the server at runtime.
- **The game engine is bundled inside each game.** Two games never run on the same server, as described in the [network architecture](../architecture/network/README.md), so their copies never meet.

The rules live in [`build-logic/src/main/kotlin/ModuleRules.kt`](../../build-logic/src/main/kotlin/ModuleRules.kt). A module in a group that is not registered there fails the build until its rule is added.

## Declaring dependencies

- **Versions live only in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).** Modules reference them as `libs.<name>` and never write a version.
- **Modules are referenced with type-safe accessors**, such as `projects.platform.core` and `projects.gameEngine`. A typo is a compile error, not a runtime surprise.
- **Pick the configuration by where the classes come from at runtime:**

| Configuration | When to use | Bundled into the plugin jar |
|---|---|---|
| `api` | A library exposes the dependency's types in its own public API. | Yes, in the plugin that uses the library |
| `implementation` | The dependency is used internally. | Yes |
| `compileOnly` | The server already provides it at runtime: the Paper API, the Velocity API or the core. | No |

- **Third-party libraries bundled into a plugin are relocated** to `br.com.hashmc.libs`, so they never conflict with another plugin that ships a different version. The relocation is configured together with the first library that needs it.
- **Updating Paper** is changing `paper` in the version catalog. The `api-version` of every `plugin.yml` follows automatically.
- **The project version** is `version` in [`gradle.properties`](../../gradle.properties), shared by every module.

## Build conventions

Every module applies one convention plugin from [`build-logic`](../../build-logic) instead of configuring the build by hand:

| Plugin | For | What it configures |
|---|---|---|
| `hashmc.java` | Applied by all the others | Java 25, UTF-8, compiler warnings, JUnit and AssertJ, and the dependency rules. |
| `hashmc.library` | Libraries | A plain Java library. |
| `hashmc.paper-library` | Libraries that use the Paper API | A library with the Paper API available at compile time. |
| `hashmc.paper-plugin` | Paper plugins | The Paper API, the bundled jar named `hashmc-<module>.jar`, and `plugin.yml` with `${version}` and `${apiVersion}` filled in. |
| `hashmc.velocity-plugin` | Velocity plugins | The Velocity API, the bundled jar, and `velocity-plugin.json` with `${version}` filled in. |
| `hashmc.network` | The root project | The `network { }` block and the server assembly tasks. |

## Adding modules

### A new game

Using SkyWars as an example:

1. Create `games/skywars/build.gradle.kts`:

   ```kotlin
   plugins {
       id("hashmc.paper-plugin")
   }

   dependencies {
       compileOnly(projects.platform.core)
       implementation(projects.gameEngine)
   }
   ```

2. Create the plugin class in `br.com.hashmc.skywars` and `src/main/resources/plugin.yml`, using `games/duels` as a model. It depends on `HashMC-Core`.
3. Add `include(":games:skywars")` to [`settings.gradle.kts`](../../settings.gradle.kts).
4. Add `server("skywars", projects.platform.core, projects.games.skywars)` to the root [`build.gradle.kts`](../../build.gradle.kts).
5. Document the game in `docs/features/skywars/`.

The `games` group already has its rule, so nothing changes in `ModuleRules.kt`.

### A new library

1. Create the module folder with a `build.gradle.kts` applying `hashmc.library`, or `hashmc.paper-library` if it needs the Paper API.
2. Include it in [`settings.gradle.kts`](../../settings.gradle.kts).
3. If it belongs to a new group, register the group's rule in `ModuleRules.kt` and in the table above.
