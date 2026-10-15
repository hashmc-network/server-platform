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
- As permissões são separadas por serviço. Os servidores de jogo e o proxy se conectam com um usuário que lê e grava dados, mas não altera o schema. As migrações rodam com outro usuário. Nenhum dos dois é superusuário.

## Redis

- Escuta só na rede privada.
- Usa ACLs: cada serviço tem o próprio usuário no Redis, limitado aos comandos, chaves e canais de que precisa. Um serviço que só lê o status dos servidores não consegue publicar pacotes.
- **Acesso ao Redis significa controle da rede:** um usuário com permissão para publicar pacotes consegue mandá-los para todos os servidores. A rede privada é a primeira barreira e as ACLs são a segunda; as duas são obrigatórias.

## Tráfego entre máquinas

Enquanto todos os serviços rodam na mesma máquina, o tráfego fica local. Quando o PostgreSQL, o Redis ou os servidores rodarem em máquinas diferentes, o tráfego entre eles usa TLS, a não ser que já passe por uma rede privada criptografada.

## Segredos

- Senhas, o segredo do forwarding e qualquer outra credencial nunca são commitados.
- Eles são passados por variáveis de ambiente ou por arquivos de configuração locais ignorados pelo git.
- Um segredo que foi exposto, mesmo que por pouco tempo, é trocado.
