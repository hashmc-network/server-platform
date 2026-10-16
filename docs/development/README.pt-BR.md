[English](README.md) | **Português**

# Desenvolvimento

Como o projeto é organizado, como as dependências são declaradas e como fazer o build.

## Requisitos

- **JDK 25.** Se não estiver instalado, o Gradle baixa sozinho no primeiro build.
- Mais nada. O wrapper do Gradle (`gradlew`) baixa a versão certa do Gradle por conta própria.

## Build

```bash
./gradlew build
```

No Windows, use `gradlew.bat build`.

Isso compila todos os módulos, roda os testes, confere as regras de dependência e monta uma pasta por servidor:

```text
build/servers/
├── proxy/plugins/    hashmc-proxy.jar
├── auth/plugins/     hashmc-core.jar  hashmc-auth.jar
├── lobby/plugins/    hashmc-core.jar  hashmc-lobby.jar
├── bedwars/plugins/  hashmc-core.jar  hashmc-bedwars.jar
└── duels/plugins/    hashmc-core.jar  hashmc-duels.jar
```

Cada pasta tem exatamente os jars daquele servidor: copie o conteúdo dela para a pasta `plugins` do servidor. Os nomes dos jars não têm versão, então um build novo substitui o arquivo antigo. A versão fica dentro do `plugin.yml` ou do `velocity-plugin.json` de cada plugin.

| Comando | O que faz |
|---|---|
| `./gradlew build` | Tudo: compila, testa, confere as regras e monta todos os servidores. |
| `./gradlew assembleServers` | Monta a pasta de todos os servidores, sem rodar os testes. |
| `./gradlew assembleLobbyServer` | Monta um único servidor. Existe uma task por servidor: `assembleProxyServer`, `assembleAuthServer`, `assembleBedwarsServer`, `assembleDuelsServer`. |
| `./gradlew :games:bedwars:build` | Faz o build e testa um único módulo. |

## Módulos

```text
server-platform/
├── build-logic/          convenções de build compartilhadas por todos os módulos
├── common/
│   ├── network/          contratos dos pacotes entre servidores
│   └── model/            modelos compartilhados entre servidores
├── platform/
│   ├── persistence/      PostgreSQL e cache no Redis
│   ├── messaging/        transporte dos pacotes pelo Redis
│   ├── core/             plugin base de todo servidor Paper
│   └── proxy/            plugin do Velocity
├── game-engine/          partidas, arenas, times e participantes
├── games/
│   ├── bedwars/
│   └── duels/
└── servers/
    ├── lobby/
    └── auth/
```

| Módulo | Tipo | O que contém | Onde vai parar |
|---|---|---|---|
| `common:network` | Biblioteca | Os contratos dos pacotes, como descrito em [Mensageria](../architecture/messaging/README.pt-BR.md). | Dentro do `hashmc-core.jar` e do `hashmc-proxy.jar` |
| `common:model` | Biblioteca | Modelos compartilhados entre servidores. | Dentro do `hashmc-core.jar` e do `hashmc-proxy.jar` |
| `platform:persistence` | Biblioteca | O PostgreSQL e o cache no Redis, como descrito em [Persistência](../architecture/persistence/README.pt-BR.md). | Dentro do `hashmc-core.jar` e do `hashmc-proxy.jar` |
| `platform:messaging` | Biblioteca | O transporte dos pacotes pelo Redis. | Dentro do `hashmc-core.jar` e do `hashmc-proxy.jar` |
| `platform:core` | Plugin Paper | A base de todo servidor Paper. Oferece os serviços compartilhados aos outros plugins. | `hashmc-core.jar` |
| `platform:proxy` | Plugin Velocity | O plugin do proxy. | `hashmc-proxy.jar` |
| `game-engine` | Biblioteca que usa a API do Paper | O [motor de jogo](../architecture/game-engine/README.pt-BR.md). | Dentro do jar de cada jogo |
| `games:bedwars`, `games:duels` | Plugin Paper | Um jogo cada. | `hashmc-bedwars.jar`, `hashmc-duels.jar` |
| `servers:lobby`, `servers:auth` | Plugin Paper | O que é específico do lobby e do servidor de autenticação. | `hashmc-lobby.jar`, `hashmc-auth.jar` |

Os pacotes seguem o módulo: `br.com.hashmc.core`, `br.com.hashmc.bedwars`, `br.com.hashmc.engine` e assim por diante.

## Servidores

Quais jars vão para cada servidor é declarado em um lugar só, o [`build.gradle.kts`](../../build.gradle.kts) da raiz:

```kotlin
network {
    server("proxy", projects.platform.proxy)
    server("lobby", projects.platform.core, projects.servers.lobby)
    server("bedwars", projects.platform.core, projects.games.bedwars)
}
```

Adicionar um servidor ao build é uma linha ali. A task `assemble<Nome>Server` dele é criada automaticamente.

## Regras de dependência

Cada módulo só pode depender dos grupos abaixo. O build garante essas regras: uma dependência proibida derruba o build com uma mensagem dizendo o que é permitido.

| Módulo | Pode depender de |
|---|---|
| `common:*` | `common` |
| `platform:persistence`, `platform:messaging` | `common` |
| `platform:core`, `platform:proxy` | `common`, `platform:persistence`, `platform:messaging` |
| `game-engine` | `common`, `platform:core` |
| `games:*` | `common`, `platform:core`, `game-engine` |
| `servers:*` | `common`, `platform:core` |

O que essas regras garantem:

- **Nada depende de um jogo, de um plugin de servidor ou do proxy.** Eles são a ponta da cadeia.
- **Jogos e plugins de servidor nunca dependem uns dos outros.** Remover ou mudar um nunca quebra o outro.
- **O `common` não conhece o resto.** Ele pode ser usado por qualquer módulo, em qualquer plataforma.
- **O core nunca é empacotado duas vezes.** Jogos e plugins de servidor usam o core com `compileOnly`, já que ele já está no servidor em tempo de execução.
- **O motor de jogo vai dentro de cada jogo.** Dois jogos nunca rodam no mesmo servidor, como descrito na [arquitetura da rede](../architecture/network/README.pt-BR.md), então as cópias nunca se encontram.

As regras ficam em [`build-logic/src/main/kotlin/ModuleRules.kt`](../../build-logic/src/main/kotlin/ModuleRules.kt). Um módulo de um grupo que não está registrado ali derruba o build até a regra dele ser adicionada.

## Declarando dependências

- **As versões ficam só no [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).** Os módulos as referenciam como `libs.<nome>` e nunca escrevem uma versão.
- **Os módulos são referenciados com acessores tipados**, como `projects.platform.core` e `projects.gameEngine`. Um erro de digitação vira erro de compilação, e não uma surpresa em tempo de execução.
- **Escolha a configuração pelo lugar de onde as classes vêm em tempo de execução:**

| Configuração | Quando usar | Vai dentro do jar do plugin |
|---|---|---|
| `api` | Uma biblioteca expõe os tipos da dependência na própria API pública. | Sim, no plugin que usa a biblioteca |
| `implementation` | A dependência é usada internamente. | Sim |
| `compileOnly` | O servidor já fornece em tempo de execução: a API do Paper, a API do Velocity ou o core. | Não |

- **Bibliotecas de terceiros empacotadas em um plugin são realocadas** para `br.com.hashmc.libs`, para nunca entrarem em conflito com outro plugin que traga uma versão diferente. A realocação é configurada junto com a primeira biblioteca que precisar dela.
- **Atualizar o Paper** é mudar o `paper` no catálogo de versões. O `api-version` de todo `plugin.yml` acompanha sozinho.
- **A versão do projeto** é o `version` do [`gradle.properties`](../../gradle.properties), compartilhado por todos os módulos.

## Convenções de build

Cada módulo aplica um plugin de convenção do [`build-logic`](../../build-logic) em vez de configurar o build na mão:

| Plugin | Para | O que configura |
|---|---|---|
| `hashmc.java` | Aplicado por todos os outros | Java 25, UTF-8, avisos do compilador, JUnit e AssertJ, e as regras de dependência. |
| `hashmc.library` | Bibliotecas | Uma biblioteca Java simples. |
| `hashmc.paper-library` | Bibliotecas que usam a API do Paper | Uma biblioteca com a API do Paper disponível na compilação. |
| `hashmc.paper-plugin` | Plugins Paper | A API do Paper, o jar empacotado com o nome `hashmc-<módulo>.jar`, e o `plugin.yml` com `${version}` e `${apiVersion}` preenchidos. |
| `hashmc.velocity-plugin` | Plugins Velocity | A API do Velocity, o jar empacotado, e o `velocity-plugin.json` com `${version}` preenchido. |
| `hashmc.network` | O projeto raiz | O bloco `network { }` e as tasks que montam os servidores. |

## Adicionando módulos

### Um jogo novo

Usando o SkyWars como exemplo:

1. Crie `games/skywars/build.gradle.kts`:

   ```kotlin
   plugins {
       id("hashmc.paper-plugin")
   }

   dependencies {
       compileOnly(projects.platform.core)
       implementation(projects.gameEngine)
   }
   ```

2. Crie a classe do plugin em `br.com.hashmc.skywars` e o `src/main/resources/plugin.yml`, usando o `games/duels` como modelo. Ele depende do `HashMC-Core`.
3. Adicione `include(":games:skywars")` no [`settings.gradle.kts`](../../settings.gradle.kts).
4. Adicione `server("skywars", projects.platform.core, projects.games.skywars)` no [`build.gradle.kts`](../../build.gradle.kts) da raiz.
5. Documente o jogo em `docs/features/skywars/`.

O grupo `games` já tem a sua regra, então nada muda no `ModuleRules.kt`.

### Uma biblioteca nova

1. Crie a pasta do módulo com um `build.gradle.kts` aplicando `hashmc.library`, ou `hashmc.paper-library` se ela precisar da API do Paper.
2. Inclua o módulo no [`settings.gradle.kts`](../../settings.gradle.kts).
3. Se ela pertencer a um grupo novo, registre a regra do grupo no `ModuleRules.kt` e na tabela acima.
