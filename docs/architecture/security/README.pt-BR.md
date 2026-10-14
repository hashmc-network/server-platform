[English](README.md) | **Português**

# Segurança da rede

Como a rede protege os seus servidores e serviços. O princípio é simples: os jogadores só alcançam o proxy, e todo o resto é privado.

## O que fica exposto

| Serviço | Acessível pelos jogadores | Acessível por |
|---|---|---|
| Velocity | Sim, é a única porta de entrada pública | Todo mundo |
| Servidores Paper | Não | Só o proxy |
| PostgreSQL | Não | Só os servidores da rede |
| Redis | Não | Só os servidores da rede |

## Proxy e servidores Paper

- O Velocity autentica os jogadores com a Mojang (`online-mode` ligado no proxy).
- O Velocity repassa a identidade dos jogadores para os servidores Paper usando o **modern forwarding**, assinado com um segredo compartilhado entre o proxy e cada servidor. Um servidor Paper só aceita conexões com uma assinatura válida.
- As portas dos servidores Paper ficam fechadas para a internet pelo firewall, ou ligadas só a uma rede privada. Mesmo com o modern forwarding, um servidor Paper acessível pela internet é um risco que não é aceito.

## PostgreSQL

- Escuta só na rede privada e só aceita conexões dos servidores da rede.
- Os servidores se conectam com um usuário dedicado, que tem só os privilégios de que precisam. Ele nunca é superusuário.

## Redis

- Escuta só na rede privada, com autenticação ligada.
- **Acesso ao Redis significa controle da rede:** qualquer processo que consiga publicar no Redis consegue mandar pacotes para todos os servidores. Os servidores confiam nos pacotes que recebem porque só os próprios servidores da rede alcançam o Redis, então manter o Redis privado é obrigatório.

## Segredos

- Senhas, o segredo do forwarding e qualquer outra credencial nunca são commitados.
- Eles são passados por variáveis de ambiente ou por arquivos de configuração locais ignorados pelo git.
- Um segredo que foi exposto, mesmo que por pouco tempo, é trocado.
