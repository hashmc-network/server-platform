**English** | [Português](README.pt-BR.md)

# HashMC Platform

HashMC network platform for modern Minecraft versions.

Successor to [`server-plugin`](https://github.com/hashmc-network/server-plugin), which remains the legacy 1.8.8 network.

## Current state

Early development. The modules and the build are set up, with no features yet. There is no CI yet.

```bash
./gradlew build
```

The build assembles the plugins of each server in `build/servers/`. See [Development](docs/development/README.md).

The planned architecture is documented in [docs/](docs/README.md#architecture): the [network](docs/architecture/network/README.md), [persistence](docs/architecture/persistence/README.md), [messaging](docs/architecture/messaging/README.md), [game engine](docs/architecture/game-engine/README.md), [security](docs/architecture/security/README.md) and [observability](docs/architecture/observability/README.md).

## Contributing

- [CONTRIBUTING.md](CONTRIBUTING.md): commits, documentation and pull request standards.
- [docs/](docs/README.md): documentation for every feature.
- [AGENTS.md](AGENTS.md): rules for AI agents working on this repository.
