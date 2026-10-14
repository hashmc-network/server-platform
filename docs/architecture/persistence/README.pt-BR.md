[English](README.md) | **Português**

# Persistência

Como a rede guarda e coloca dados em cache. O objetivo é aguentar muita carga, hoje e conforme a rede crescer, sem deixar cada servidor mais pesado do que o necessário.

## Papéis

| Camada | Tecnologia | Papel |
|---|---|---|
| Memória | A memória de cada servidor | Os dados dos jogadores online naquele servidor. É a leitura mais rápida, usada em quase tudo durante o jogo. |
| Cache compartilhado | Redis | Dados quentes compartilhados entre servidores, como perfis de jogadores online, rankings e status dos servidores. |
| Fonte da verdade | PostgreSQL | Todo dado que precisa sobreviver a um reinício. Se o Redis e o PostgreSQL discordarem, vale o PostgreSQL. |

O Redis também é o transporte das mensagens entre servidores, descrito em [Mensageria](../messaging/README.pt-BR.md).

## Leitura

As leituras vão da camada mais rápida para a mais lenta, e preenchem as camadas mais rápidas no caminho de volta:

```text
memória → Redis → PostgreSQL
```

- Os dados de um jogador são carregados uma vez, quando ele conecta, e ficam em memória enquanto ele estiver naquele servidor.
- Durante o jogo, o código lê da memória. Ele nunca consulta o banco para responder algo que o servidor já sabe.

## Escrita

- As mudanças são aplicadas primeiro na memória, então o jogo nunca espera pelo banco.
- Cada objeto registra o que mudou, e só os dados alterados são gravados.
- As mudanças são salvas no PostgreSQL em segundo plano, em um intervalo fixo e imediatamente quando o jogador sai do servidor.
- Dados que só crescem, como histórico de partidas e estatísticas, são gravados em lotes, e não uma linha por vez.

## Troca de servidor

Quando um jogador vai do `bedwars` para o `lobby`, o servidor novo não pode carregar dados que o antigo ainda não salvou.

1. O servidor que o jogador está deixando salva os dados dele no Redis e depois no PostgreSQL.
2. O servidor para onde o jogador vai lê primeiro do Redis, que já tem os dados mais recentes, e usa o PostgreSQL se não encontrar.
3. Todo registro salvo carrega um número de versão. Uma escrita só é aplicada se a versão dela for mais nova que a guardada, então um salvamento atrasado nunca sobrescreve dados mais novos.

## Onde fica cada tipo de dado

| Dado | Memória | Redis | PostgreSQL |
|---|---|---|---|
| Perfil de jogador online | Sim | Sim, enquanto ele estiver online | Sim |
| Perfil de jogador offline | Não | Não | Sim |
| Rankings | Não | Sim, reconstruídos a partir do PostgreSQL | Sim |
| Histórico de partidas e estatísticas | Não | Não | Sim, gravados em lotes |
| Status dos servidores e contagem de jogadores | Não | Sim, com expiração | Não |

## Regras

- **Nunca bloqueie a thread principal.** Chamadas ao banco e ao Redis sempre rodam fora da thread principal do servidor. Resultados que mexem no jogo são agendados de volta para a thread principal.
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
