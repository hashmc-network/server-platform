[English](README.md) | **Português**

# Mensageria

Como os servidores conversam entre si. Toda mensagem é um **pacote**: um objeto pequeno, com um id fixo, serializado em binário e enviado pelo Redis.

## Transporte: Redis Pub/Sub

Os pacotes entre servidores trafegam pelo Redis Pub/Sub. O Plugin Messaging foi considerado e não é usado para isso.

| | Redis Pub/Sub | Plugin Messaging |
|---|---|---|
| Precisa de jogador online | Não | Sim. As mensagens passam pela conexão de um jogador, então um servidor sem jogadores não envia nem recebe. |
| Servidor para servidor | Direto | Só pelo proxy, que precisa repassar cada mensagem. |
| Envio para vários servidores | Uma publicação chega a todos os inscritos | Uma mensagem por servidor, cada uma precisando de um jogador lá. |
| Tamanho da mensagem | Sem limite prático para pacotes | Cerca de 32 KB por mensagem enviada ao proxy. |
| Latência | Abaixo de um milissegundo na mesma rede | Também baixa, mas presa à conexão do jogador. |
| Infraestrutura extra | O Redis, que a rede já usa para cache | Nenhuma. |

Os dois são rápidos o bastante para que a velocidade não decida entre eles. O que decide é a confiabilidade: o Plugin Messaging para de funcionar justamente quando um servidor está vazio, o que é comum em servidores de jogo esperando jogadores e em servidores que acabaram de iniciar.

O Plugin Messaging fica reservado para os casos que precisam de verdade da conexão do jogador, como conversar com mods do lado do cliente.

## Pacotes

- Cada tipo de pacote tem um id fixo e único e o próprio serializador binário. O binário deixa as mensagens pequenas e baratas de ler.
- Todo pacote viaja dentro de um envelope com o id do pacote, o servidor que o enviou e, nas requisições, um id de correlação.
- Um servidor que recebe um id de pacote que não conhece ignora o pacote e registra no log. Isso permite atualizar os servidores um de cada vez sem que um quebre o outro.
- Os pacotes carregam ids e valores pequenos, e não objetos inteiros. Quando quem recebe precisa de mais, lê do cache ou do banco.

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
- Quem enviou espera de forma assíncrona e desiste depois de um tempo limite, então um servidor que nunca responde não trava nada.

## Entrega

O Redis Pub/Sub entrega cada pacote **no máximo uma vez**. Um servidor que está desligado ou reconectando quando o pacote é publicado nunca o recebe.

- Pacotes que podem se perder sem prejuízo são enviados diretamente, como atualizações de status dos servidores e chat entre servidores.
- Pacotes que não podem se perder têm o PostgreSQL por trás. O dado é gravado primeiro no banco, e o pacote só avisa os servidores. Um servidor que perdeu o aviso encontra o dado pendente sozinho, por exemplo quando o jogador entra.

Exemplo: uma compra na loja é salva no PostgreSQL como entrega pendente, e um pacote avisa o servidor onde o jogador está para entregá-la. Se o pacote se perder, a entrega acontece na próxima vez que o jogador entrar em qualquer servidor.

## Regras

- **Os handlers de pacote nunca rodam na thread principal.** Tudo o que mexe no jogo é agendado de volta para a thread principal.
- **O código depende da API de pacotes, e nunca do Redis diretamente.** O transporte fica em um lugar só e pode mudar sem mexer nas features.
- **Um tipo novo de pacote é documentado na feature que o usa.**
