[English](README.md) | **Português**

# Persistência

Como a rede guarda e coloca dados em cache. O objetivo é aguentar muita carga, hoje e conforme a rede crescer, sem deixar cada servidor mais pesado do que o necessário e sem nunca perder um dado que o jogador foi informado de que está salvo.

## Papéis

| Camada | Tecnologia | Papel |
|---|---|---|
| Memória | A memória de cada servidor | Os dados dos jogadores online naquele servidor. É a leitura mais rápida, usada em quase tudo durante o jogo. |
| Cache compartilhado | Redis | Dados quentes compartilhados entre servidores, como perfis de jogadores online, rankings e status dos servidores. |
| Fonte da verdade | PostgreSQL | Todo dado que precisa sobreviver a um reinício. Se o Redis e o PostgreSQL discordarem, vale o PostgreSQL. |

O Redis também é o transporte das mensagens entre servidores, descrito em [Mensageria](../messaging/README.pt-BR.md).

## Tipos de dado

Nem todo dado merece a mesma garantia. Cada tipo tem a própria estratégia de escrita:

| Tipo | Exemplos | Estratégia |
|---|---|---|
| Crítico | Moedas, compras, recompensas, MMR | Gravado no PostgreSQL em uma transação **antes** de a operação ser confirmada ao jogador. |
| Não crítico | Preferências de cosméticos, configurações, opções | Aplicado na memória e gravado em segundo plano. Um crash pode perder os últimos segundos de mudanças, e isso é aceito. |
| Estado da partida | Times, kills, camas, timers de uma partida em andamento | Só na memória, no servidor que roda a partida. |
| Fim de partida | Resultado final, recompensas críticas | Salvos em uma única transação, que também marca a partida como terminada. Veja [Motor de jogo](../game-engine/README.pt-BR.md#resultados). |
| Histórico | Estatísticas detalhadas | Gravadas em lotes, com um id único por registro, para que uma nova tentativa nunca crie duplicata. |

## Leitura

As leituras vão da camada mais rápida para a mais lenta, e preenchem as camadas mais rápidas no caminho de volta:

```text
memória → Redis → PostgreSQL
```

- Os dados de um jogador são carregados uma vez, quando ele conecta, e ficam em memória enquanto ele estiver naquele servidor.
- Durante o jogo, o código lê da memória. Ele nunca consulta o banco para responder algo que o servidor já sabe.

## Escrita

### Dados críticos

- A operação roda como uma transação no PostgreSQL, fora da thread principal. O jogador só vê o resultado depois que a transação é confirmada.
- O jogo não trava enquanto espera: a thread principal continua rodando, e o resultado é aplicado quando a transação volta.
- Saldos mudam por operações atômicas no banco, e nunca sobrescrevendo um valor lido antes. Uma compra é `subtrair 2.000 se o saldo for de pelo menos 2.000`, e não `definir o saldo como 3.000`.
- Se a transação falhar, a operação falha e o jogador é avisado. Nada aparece como concluído antes de estar salvo.

### Dados não críticos

- As mudanças são aplicadas primeiro na memória, então o jogo nunca espera pelo banco.
- Cada objeto registra o que mudou, e só os dados alterados são gravados.
- As mudanças são salvas no PostgreSQL em segundo plano, em um intervalo fixo e quando o jogador sai do servidor.

### Histórico

- Dados que só crescem, como estatísticas detalhadas, são gravados em lotes, e não uma linha por vez.
- O fim de uma partida não vai em lote: o resultado e as recompensas críticas são salvos juntos, em uma transação.
- Cada registro tem um id único, gerado quando o evento acontece. Gravar o mesmo registro duas vezes não tem efeito.

## Dono da sessão

A cada momento, os dados de cada jogador online têm exatamente **um dono**: o servidor onde o jogador está.

- O dono fica registrado no PostgreSQL, junto com um número de **geração** que só aumenta.
- Um servidor assume a posse com uma única atualização atômica, que o define como dono e aumenta a geração. A atualização só funciona se a sessão estiver livre ou se o dono atual não estiver mais vivo.
- Toda escrita nos dados daquele jogador carrega a geração que o servidor recebeu. O PostgreSQL recusa qualquer escrita cuja geração não seja a atual.
- Um servidor que tem uma escrita recusada perdeu a posse: ele para de mudar os dados daquele jogador e desconecta o jogador de si.
- Cada servidor publica um sinal de vida (heartbeat) no Redis, com uma expiração curta. Um servidor está vivo enquanto o sinal dele existir.
- Se o dono cair, o sinal de vida dele expira e o próximo servidor assume a posse com uma geração nova, carregando o jogador do PostgreSQL. Os dados críticos já estão lá; só as mudanças não críticas recentes podem se perder.

Isso mantém um único escritor mesmo quando um servidor trava por muito tempo, como numa pausa longa de garbage collection. Quando ele volta, a geração dele está desatualizada e toda escrita que ele tentar é recusada.

### Quando o Redis está indisponível

- As sessões que já têm dono continuam funcionando, porque a posse e a conferência da geração ficam no PostgreSQL.
- As trocas de servidor planejadas continuam funcionando, porque a origem libera a sessão explicitamente.
- Assumir a sessão de um servidor que não a liberou fica esperando o Redis voltar, já que não há como saber se o dono antigo ainda está vivo. O jogador é avisado para tentar de novo em instantes.

## Troca de servidor

Quando um jogador vai do `bedwars` para o `lobby`, o destino precisa carregar exatamente o que a origem tinha.

1. A origem para de aceitar mudanças nos dados daquele jogador.
2. A origem grava no PostgreSQL toda mudança pendente, com a sua geração.
3. A origem atualiza os dados do jogador no Redis e libera a sessão no PostgreSQL.
4. A transferência é confirmada, e o destino assume a posse com uma geração nova e carrega os dados, primeiro do Redis e, se não encontrar, do PostgreSQL.

Se o passo 2 falhar ou passar do tempo limite, a transferência é cancelada: o jogador continua na origem, o dono não muda e o jogador é avisado de que a transferência falhou. Uma transferência nunca acontece com dados não salvos.

## Onde fica cada tipo de dado

| Dado | Memória | Redis | PostgreSQL |
|---|---|---|---|
| Perfil de jogador online | Sim | Sim, enquanto ele estiver online | Sim |
| Perfil de jogador offline | Não | Não | Sim |
| Moedas e outros saldos | Sim, como cópia de leitura | Sim, como cópia de leitura | Sim, toda mudança em uma transação |
| Rankings | Não | Sim, reconstruídos a partir do PostgreSQL | Sim |
| Estado de uma partida em andamento | Sim | Não | Não |
| Resultado da partida e recompensas críticas | Não | Não | Sim, em uma transação |
| Estatísticas detalhadas | Não | Não | Sim, gravadas em lotes |
| Dono da sessão e geração | Não | Não | Sim |
| Sinal de vida dos servidores | Não | Sim, com expiração | Não |
| Status dos servidores e contagem de jogadores | Não | Sim, com expiração | Não |

## Regras

- **Nunca bloqueie a thread principal.** Chamadas ao banco e ao Redis sempre rodam fora da thread principal do servidor. Resultados que mexem no jogo são agendados de volta para a thread principal.
- **Dado crítico só é confirmado depois de salvo.** Nunca mostre ao jogador uma compra, recompensa ou mudança de saldo que não esteja confirmada no PostgreSQL.
- **Toda escrita de dados do jogador carrega a geração do dono.** Uma escrita sem ela, ou com uma geração antiga, é um bug.
- **Dado durável nunca fica só no Redis.** O Redis é um cache e pode ser esvaziado a qualquer momento sem perder nada.
- **Dado temporário no Redis sempre expira.** Toda chave que não é cache de um dado do PostgreSQL tem tempo de expiração.
- **As chaves do Redis seguem um prefixo:** `hashmc:<área>:<id>`, como `hashmc:profile:<uuid>`.
- **O schema só muda por migrações versionadas.** Nenhum servidor cria ou altera tabelas por conta própria ao iniciar.
- **Nada de consulta dentro de loop.** Carregue o que for preciso em uma consulta só.
- **Toda consulta frequente tem um índice.**
- **Os pools de conexão são pequenos.** Cada conexão com o PostgreSQL é um processo no servidor do banco, então poucas conexões por servidor bastam quando nenhuma consulta fica muito tempo travada.

## Bibliotecas planejadas

| Biblioteca | Uso | Por quê |
|---|---|---|
| HikariCP | Pool de conexões do PostgreSQL | Rápido, leve e o pool padrão na JVM. |
| Flyway | Migrações do banco | Mudanças de schema versionadas e reproduzíveis. |
| Lettuce | Cliente do Redis | Assíncrono, e uma única conexão pode ser compartilhada por todas as threads. |
| Caffeine | Cache em memória | Cache em memória rápido, com expiração e limite de tamanho. |

Nenhum ORM pesado é usado. As consultas são escritas em SQL, para que fique sempre claro quanto cada uma custa.

## Quando a rede crescer

Estes pontos não são necessários na fase 1 e serão revistos na fase 2 da [arquitetura da rede](../network/README.pt-BR.md):

- Um pooler de conexões, como o PgBouncer, na frente do PostgreSQL, quando muitos servidores de jogo tiverem cada um as próprias conexões.
- Particionar por data as tabelas grandes que só crescem, como o histórico de partidas.
- Réplicas de leitura para consultas pesadas que só leem, como páginas de rankings e estatísticas.
