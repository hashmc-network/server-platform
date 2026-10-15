[English](README.md) | **Português**

# Observabilidade

O que a rede mede, para que problemas de desempenho sejam vistos com números, e não com palpites. Este documento define **o que** é medido. As ferramentas que coletam e mostram as métricas serão escolhidas depois.

## Servidores

| Métrica | O que mostra |
|---|---|
| MSPT (milissegundos por tick) | Quanto tempo cada tick leva em cada servidor Paper. Acima de 50 ms o servidor não consegue mais manter 20 TPS. |
| TPS | Ticks por segundo que cada servidor Paper de fato atinge. |
| Jogadores online | Por servidor e na rede inteira. |
| Memória e pausas de garbage collection | Uso do heap e a duração de cada pausa. Pausas longas congelam todas as partidas do servidor e podem fazê-lo perder a posse das sessões. |

## Partidas e arenas

| Métrica | O que mostra |
|---|---|
| Partidas ativas | Por servidor e por modo de jogo. |
| Jogadores esperando partida | Por modo de jogo. |
| Tempo de restauração das arenas | Quanto tempo cada instância de arena leva para ser restaurada depois de uma partida. |
| Falhas de restauração | Restaurações com falha por arena, e arenas desativadas no momento. |
| Partidas canceladas | Por modo de jogo e por motivo. |

## Persistência

| Métrica | O que mostra |
|---|---|
| Tempo das transações | Quanto tempo as transações do PostgreSQL levam, por tipo de operação. |
| Espera por conexão | Quanto tempo o código espera por uma conexão livre no pool, e quantas conexões estão em uso. Espera aqui significa que o pool é pequeno demais ou que alguma consulta está lenta demais. |
| Transações com falha | Por tipo de operação. |
| Escritas recusadas | Escritas recusadas porque a geração da sessão do servidor estava desatualizada. Qualquer valor acima de zero precisa de atenção. |
| Latência do Redis | Quanto tempo os comandos do Redis levam. |

## Mensageria

| Métrica | O que mostra |
|---|---|
| Pacotes enviados e recebidos | Por tipo de pacote e por servidor. |
| Requisições sem resposta | Requisições que não tiveram resposta a tempo, por tipo de pacote. |
| Pacotes recusados | Pacotes recusados por serem grandes demais, ou ignorados por terem um tipo ou versão de protocolo desconhecidos. |
| Reconexões ao Redis | Quantas vezes cada servidor perdeu a conexão com o Redis. |

## Troca de servidor

| Métrica | O que mostra |
|---|---|
| Transferências | Por servidor de origem e de destino. |
| Tempo de transferência | Do momento em que a origem para de aceitar mudanças até o destino carregar o jogador. |
| Transferências com falha | Por motivo, como tempo esgotado ao salvar ou falha ao assumir a sessão. |

## Regras

- **Medir nunca deixa o jogo mais lento.** As métricas são coletadas sem bloquear a thread principal e com custo baixo por amostra.
- **Uma feature documenta as próprias métricas** quando acrescentar alguma, na documentação da feature.
- **Um número sem referência diz pouco.** Quando a rede estiver rodando, o valor normal de cada métrica é registrado, para que as mudanças fiquem evidentes.
