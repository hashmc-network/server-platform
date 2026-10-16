[English](README.md) | **Português**

# Documentação

Cada feature da plataforma é documentada na própria pasta dentro de [`features/`](features/), em inglês e português. As regras estão no [CONTRIBUTING.pt-BR.md](../CONTRIBUTING.pt-BR.md#documentação).

## Desenvolvimento

- [Desenvolvimento](development/README.pt-BR.md): módulos, regras de dependência e como fazer o build.

## Arquitetura

- [Rede](architecture/network/README.pt-BR.md): como a rede é dividida em servidores, hoje e no futuro.
- [Persistência](architecture/persistence/README.pt-BR.md): como os dados são guardados no PostgreSQL e ficam em cache na memória e no Redis.
- [Mensageria](architecture/messaging/README.pt-BR.md): como os servidores trocam pacotes pelo Redis.
- [Motor de jogo](architecture/game-engine/README.pt-BR.md): como as partidas rodam e ficam isoladas dentro de cada servidor de jogo.
- [Segurança](architecture/security/README.pt-BR.md): o que fica exposto e como servidores, serviços e segredos são protegidos.
- [Observabilidade](architecture/observability/README.pt-BR.md): quais métricas a rede mede.

## Features

Nenhuma feature documentada ainda.

<!-- Uma linha por feature, em ordem alfabética: - [Anticheat](features/anticheat/README.pt-BR.md): resumo curto. -->

## Como documentar uma feature nova

1. Copie [`features/_template/`](features/_template/) para `features/<feature>/`, com o mesmo nome do escopo do commit.
2. Preencha o `README.md` e o `README.pt-BR.md`.
3. Adicione a feature na lista acima, tanto neste arquivo quanto no [README.md](README.md).
