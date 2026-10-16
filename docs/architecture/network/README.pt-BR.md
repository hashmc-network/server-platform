[English](README.md) | **Português**

# Arquitetura da rede

Como a rede HashMC é dividida em servidores, hoje e no plano para o futuro.

## Fase 1: um servidor por jogo

```text
                 Proxy Velocity
                       ↓
                Lobby principal
                       ↓
        BedWars              |  Duels
        Lobby do BedWars     |  Lobby do Duels
        Arena #1             |  Duels #1
        Arena #2             |  Duels #2
```

As setas mostram o caminho do jogador. Tecnicamente, toda conexão passa pelo proxy, que leva o jogador de um servidor para outro.

A rede começa com cinco processos: um Velocity e quatro servidores Paper.

| Processo | Software | Papel |
|---|---|---|
| `velocity` | Velocity | Porta de entrada. Recebe todas as conexões e leva os jogadores de um servidor para outro. |
| `auth` | Paper | Servidor de autenticação. O papel dele será detalhado quando ele for implementado. |
| `lobby` | Paper | Lobby principal. Onde os jogadores chegam e escolhem um jogo. |
| `bedwars` | Paper | O lobby do BedWars e as arenas dele, rodando no mesmo servidor. |
| `duels` | Paper | O lobby do Duels e as partidas dele, rodando no mesmo servidor. |

Todos os processos se conectam à infraestrutura compartilhada:

| Serviço | Papel |
|---|---|
| PostgreSQL | Banco de dados e fonte da verdade. Veja [Persistência](../persistence/README.pt-BR.md). |
| Redis | Cache compartilhado e transporte dos pacotes entre servidores. Veja [Persistência](../persistence/README.pt-BR.md) e [Mensageria](../messaging/README.pt-BR.md). |

### Por que cada jogo roda no próprio servidor

- **Isolamento:** cada servidor Paper tem o próprio TPS. Um problema pesado no BedWars não deixa o Duels lento, e um crash em um jogo não derruba os outros.
- **Reinício independente:** um jogo pode ser atualizado e reiniciado sem tirar os jogadores dos outros.

### Escalando um jogo

Quando um jogo fica popular, mais servidores rodam a mesma lógica de jogo lado a lado:

```text
bedwars-01   bedwars-02   bedwars-03
```

Cada um é uma cópia completa do servidor `bedwars`, com o próprio lobby do BedWars e as próprias arenas. O proxy distribui os jogadores entre eles.

## Fase 2: lobbies dos jogos separados dos servidores de jogo

Este é o plano para o futuro, e não o que roda hoje.

```text
                 Proxy Velocity
                       ↓
                Lobby principal
                       ↓
        Lobby do BedWars     |  Lobby do Duels
              ↓              |       ↓
        bedwars-game-01      |  duels-game-01
        bedwars-game-02      |  duels-game-02
        bedwars-game-N       |  duels-game-N
```

O lobby de cada jogo vira um servidor próprio, e as partidas rodam em servidores de jogo dedicados.

- **Escala independente:** dá para subir mais servidores de jogo sem mexer nos lobbies.
- **Lobbies mais leves:** os lobbies só recebem jogadores esperando partida, então continuam rápidos mesmo com muitas partidas rodando.
- **Reinícios contidos:** reiniciar um servidor de jogo só afeta as partidas que estão nele, e não os jogadores esperando no lobby.

### Ainda não decidido

Estes pontos serão detalhados quando a fase 2 começar:

- Como o lobby de um jogo manda os jogadores para um servidor de jogo com vaga.
- Como os servidores de jogo são ligados e desligados conforme a demanda.
- Como os servidores compartilham dados, como perfis de jogadores e o estado das partidas.
