[English](README.md) | **Português**

# Motor de jogo

A parte da plataforma que roda as partidas dentro de cada servidor de jogo, como o `bedwars` e o `duels`. Cada servidor de jogo roda várias partidas ao mesmo tempo, então o papel principal do motor é mantê-las isoladas umas das outras.

Os nomes das classes são decididos quando o motor for implementado. Este documento define os conceitos e o comportamento que eles precisam ter.

## Conceitos

| Conceito | O que é |
|---|---|
| Modo de jogo | As regras de um jogo, como BedWars solo ou Duels 1v1. Ele também define as políticas do modo, descritas mais abaixo. |
| Modelo de arena | Um mapa configurado para um modo de jogo: o mundo original e seus pontos de spawn, camas, geradores e outras configurações. |
| Instância de arena | Uma cópia jogável de um modelo, usada por uma partida de cada vez. |
| Partida | Um jogo em andamento: os jogadores, os times, a instância de arena e o estado atual. |
| Participante | Um jogador dentro de uma partida, com os dados que só importam durante aquela partida. |

## Isolamento entre partidas

Cada instância de arena roda no próprio mundo. Além disso, o motor garante:

- **Uma partida por jogador.** A cada momento, um jogador pertence a no máximo uma partida, ou ao lobby do jogo.
- **Os eventos ficam dentro da partida.** Todo evento que envolve um jogador, entidade, bloco ou mundo é direcionado à partida dona dele. Uma interação entre duas partidas diferentes, como dano entre jogadores de partidas diferentes, é cancelada.
- **Comunicação restrita.** O chat, a lista de jogadores (tab) e a visibilidade dos jogadores só incluem jogadores da mesma partida.
- **As tarefas pertencem à partida.** Toda tarefa agendada é registrada na partida e cancelada quando ela fecha, então nada continua rodando depois que ela termina.
- **As falhas ficam dentro da partida.** Um erro dentro de uma partida cancela só aquela partida, e nunca as outras nem o servidor.

## Ciclo de vida da partida

```text
WAITING → STARTING → RUNNING → ENDING → CLOSED
   ↑          │
   └──────────┘   (jogadores insuficientes)

qualquer estado → CANCELLED → CLOSED
```

| Estado | O que acontece |
|---|---|
| `WAITING` | A partida aceita jogadores até atingir o mínimo do modo. |
| `STARTING` | Contagem regressiva. Se jogadores saírem e o número cair abaixo do mínimo, a partida volta para `WAITING`. |
| `RUNNING` | O jogo está acontecendo. Ninguém novo entra, exceto jogadores voltando, quando o modo permite. |
| `ENDING` | O vencedor é definido, o resultado e as recompensas são salvos e os jogadores veem o resultado. |
| `CANCELLED` | A partida foi interrompida por uma falha ou um desligamento. A política do modo decide o que acontece com os jogadores. |
| `CLOSED` | Os jogadores voltam para o lobby do jogo, as tarefas são canceladas e a instância de arena é liberada. |

As mudanças de estado só acontecem pelo ciclo de vida, e nunca alterando o estado diretamente. Cada estado define quais ações são permitidas nele.

## Ciclo de vida da arena

```text
AVAILABLE → RESERVED → IN_USE → RESETTING → AVAILABLE
                                    │
                                    └─→ DISABLED   (erro de configuração ou modelo corrompido)
```

- Uma partida reserva uma instância disponível quando é criada e a libera quando fecha.
- Depois de uma partida, a instância é restaurada a partir do modelo antes de poder ser usada de novo.
- **Se a restauração falhar**, a instância é ignorada e a restauração é tentada de novo automaticamente. As outras instâncias continuam atendendo partidas enquanto isso.
- **Só quando a falha vem de um erro de configuração ou de um modelo corrompido** a arena é desativada. O erro é registrado no log, a equipe é avisada, e a arena fica fora de uso até ser corrigida.

## Políticas definidas por cada modo de jogo

O motor não decide estas regras. Cada modo de jogo define as suas:

| Política | Exemplos do que um modo decide |
|---|---|
| Quantidade de jogadores | Mínimo para começar e máximo por partida. |
| Reconexão | Se um jogador que cai no meio da partida pode voltar, e por quanto tempo. |
| Saída antes do início | Se um jogador que sai durante o `WAITING` ou o `STARTING` recebe alguma penalidade. |
| Desligamento planejado | Se as partidas em andamento são terminadas, canceladas ou compensadas quando o servidor é desligado. |
| Crash | Se os jogadores das partidas perdidas em um crash são compensados. |

### Reconexão

Quando um modo permite reconexão, a plataforma registra no Redis a qual partida o jogador pertence, com uma expiração igual à janela de reconexão do modo. Um jogador que volta é mandado de novo para aquela partida enquanto o registro existir.

### Desligamentos e crashes

- **Desligamento planejado:** o servidor para de aceitar partidas novas, aplica a política de desligamento de cada modo às partidas em andamento e só então desliga.
- **Crash:** o estado das partidas em andamento se perde, já que ele só existe em memória. Um modo que compensa jogadores depois de um crash registra o início de cada partida no PostgreSQL. Quando o servidor inicia de novo, ele encontra as partidas que começaram e nunca foram marcadas como terminadas ou canceladas, e aplica a política de crash do modo só a elas.

## Resultados

O fim de uma partida é salvo em **uma única transação no PostgreSQL**, que:

1. registra o resultado, com o id único da partida;
2. concede as recompensas críticas, como moedas e MMR;
3. marca a partida como terminada.

Ou as três coisas são salvas, ou nenhuma é. Um crash nunca deixa recompensas concedidas para uma partida que ainda parece não terminada, então a compensação de crash nunca paga duas vezes. Salvar de novo o mesmo id de partida não tem efeito.

As recompensas só são mostradas aos jogadores depois que a transação é confirmada. As estatísticas detalhadas, que não são críticas, são salvas depois, em lotes, como descrito em [Persistência](../persistence/README.pt-BR.md).

## Regras

- **A lógica da partida roda na thread principal.** Banco, Redis e outros trabalhos lentos rodam fora dela, como descrito em [Persistência](../persistence/README.pt-BR.md).
- **Cada modo de jogo documenta as suas políticas** na documentação da própria feature.
