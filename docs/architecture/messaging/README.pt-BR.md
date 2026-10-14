[English](README.md) | **Português**

# Mensageria

Como os servidores conversam entre si. Toda mensagem é um **pacote**: um objeto pequeno, com um tipo, enviado pelo Redis dentro de um envelope padrão.

## Transporte: Redis Pub/Sub

Os pacotes entre servidores trafegam pelo Redis Pub/Sub. O Plugin Messaging foi considerado e não é usado para isso.

| | Redis Pub/Sub | Plugin Messaging |
|---|---|---|
| Precisa de jogador online | Não | Sim. As mensagens passam pela conexão de um jogador, então um servidor sem jogadores não envia nem recebe. |
| Servidor para servidor | Direto | Só pelo proxy, que precisa repassar cada mensagem. |
| Envio para vários servidores | Uma publicação chega a todos os inscritos | Uma mensagem por servidor, cada uma precisando de um jogador lá. |
| Tamanho da mensagem | Limitado pelo tamanho máximo de pacote definido pela plataforma | Cerca de 32 KB por mensagem enviada ao proxy. |
| Latência | Normalmente em torno de um milissegundo ou menos na mesma rede. Precisa ser medida na infraestrutura real. | Também baixa, mas presa à conexão do jogador. |
| Infraestrutura extra | O Redis, que a rede já usa para cache | Nenhuma. |

Os dois são rápidos o bastante para que a velocidade não decida entre eles. O que decide é a confiabilidade: o Plugin Messaging para de funcionar justamente quando um servidor está vazio, o que é comum em servidores de jogo esperando jogadores e em servidores que acabaram de iniciar.

O Plugin Messaging fica reservado para os casos que precisam de verdade da conexão do jogador, como conversar com mods do lado do cliente.

## Envelope

Todo pacote viaja dentro de um envelope com:

| Campo | Para que serve |
|---|---|
| Versão do protocolo | Permite a um servidor perceber pacotes de uma versão mais nova ou mais antiga da plataforma. |
| Tipo do pacote | Diz a quem recebe como ler o conteúdo. |
| Id da operação | Único por operação. Quem recebe o mesmo id de operação duas vezes aplica só uma. |
| Origem | O servidor que enviou o pacote. |
| Id de correlação | Presente nas requisições e nas respostas, para ligar uma à outra. |
| Conteúdo | Os dados do próprio pacote. |

- Um pacote maior que o tamanho máximo da plataforma é recusado no envio, e nunca é publicado.
- Um servidor que recebe um tipo de pacote ou uma versão de protocolo que não entende ignora o pacote e registra no log. Isso permite atualizar os servidores um de cada vez sem que um quebre o outro.
- Os pacotes carregam ids e valores pequenos, e não objetos inteiros. Quando quem recebe precisa de mais, lê do cache ou do banco.

### Formato de serialização

O contrato acima vem primeiro; o formato é escolhido quando os primeiros pacotes forem implementados. Ele começa com um formato existente, como JSON ou MessagePack. Um formato binário próprio só é adotado se uma medição mostrar que o formato existente é um gargalo de verdade.

## Canais

Cada servidor só se inscreve nos canais que dizem respeito a ele, então nenhum servidor gasta tempo lendo pacotes destinados a outros.

| Canal | Quem recebe |
|---|---|
| `hashmc:packets:broadcast` | Todos os servidores, inclusive o proxy. |
| `hashmc:packets:proxy` | O proxy. |
| `hashmc:packets:group:<grupo>` | Todos os servidores de um grupo, como `bedwars` ou `duels`. |
| `hashmc:packets:server:<servidor>` | Um único servidor, como `bedwars-01`. |

## Requisições e respostas

Alguns pacotes esperam uma resposta, como "este jogador está online no seu servidor?".

- A requisição carrega um id de correlação. A resposta volta com o mesmo id.
- Quem enviou espera de forma assíncrona e desiste depois de um tempo limite. Toda requisição define o que acontece quando o tempo acaba: tentar de novo, buscar em outra fonte ou falhar a operação e avisar o jogador.

## Entrega

O Redis Pub/Sub entrega cada pacote **no máximo uma vez**. Um servidor que está desligado ou reconectando quando o pacote é publicado nunca o recebe.

- Pacotes que podem se perder sem prejuízo são enviados diretamente, como atualizações de status dos servidores e chat entre servidores.
- Pacotes que não podem se perder têm o PostgreSQL por trás. O dado é gravado primeiro no banco, e o pacote só avisa os servidores para que ajam na hora.
- Um aviso perdido pode atrasar uma ação, mas nunca fazê-la se perder. Os servidores se reconciliam sozinhos:
  - quando um jogador entra, o servidor confere as pendências dele;
  - enquanto a rede roda, cada servidor confere periodicamente as pendências dos seus jogadores online.

Exemplo: uma compra na loja é salva no PostgreSQL como entrega pendente, e um pacote avisa o servidor onde o jogador está para entregá-la. Se o pacote se perder, a conferência periódica encontra a entrega alguns segundos depois, mesmo que o jogador nunca saia do servidor. A entrega é marcada como feita na mesma transação que a concede, então nunca é entregue duas vezes.

## Reconexão

Quando um servidor perde a conexão com o Redis:

1. Ele continua rodando o jogo. O que depende do Redis espera ou falha com uma mensagem clara.
2. Ele reconecta e se inscreve de novo nos seus canais.
3. Ele reconcilia o que pode ter perdido: publica de novo o próprio status e confere as pendências dos seus jogadores online.

## Regras

- **Os handlers de pacote nunca rodam na thread principal.** Tudo o que mexe no jogo é agendado de volta para a thread principal.
- **Handlers com efeito são idempotentes.** Receber o mesmo id de operação duas vezes não muda nada na segunda.
- **O código depende da API de pacotes, e nunca do Redis diretamente.** O transporte fica em um lugar só e pode mudar sem mexer nas features.
- **Um tipo novo de pacote é documentado na feature que o usa.**
