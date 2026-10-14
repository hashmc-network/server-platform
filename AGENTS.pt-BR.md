[English](AGENTS.md) | **Português**

# Agentes de IA

Regras para agentes de IA que trabalham neste repositório. Tudo o que está no [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md) também vale para os agentes; este arquivo acrescenta o que é específico deles.

Este arquivo vai receber mais instruções conforme o projeto crescer. Quando uma regra daqui contradiz um hábito ou padrão genérico, vale este arquivo.

Os agentes leem a versão em inglês, o [AGENTS.md](AGENTS.md). As duas versões dizem a mesma coisa e são atualizadas juntas.

## Comentários no código

Comentários são proibidos, exceto `TODO` e `FIXME`.

- Não escreva comentário de nenhum tipo: nada de Javadoc, comentário de bloco ou comentário de linha.
- `TODO` e `FIXME` são permitidos quando necessário: `TODO` marca algo que ainda falta, `FIXME` marca algo que se sabe estar errado. Diga o que está pendente e por quê, e cite a issue quando houver uma: `// TODO: retry the transfer when the proxy reconnects (#42)`.
- Não deixe código comentado.
- Deixe que os nomes e a estrutura expliquem o código. Quando um trecho parecer precisar de explicação, extraia-o para um método ou variável cujo nome explique o que ele faz.
- Não remova cabeçalhos de licença.

## Testes

- Toda mudança de comportamento vem com testes que a cobrem.
- Uma correção de bug começa com um teste que reproduz o bug e falha antes da correção.
- Teste a regra de negócio com testes unitários rápidos sobre as classes de domínio e de serviço, usando implementações em memória. Use um ambiente que simula o servidor só quando o comportamento depende de verdade do runtime do servidor.
- Comportamentos que dependem do PostgreSQL ou do Redis, como consultas, migrações, transações, cache e pacotes, são cobertos por testes de integração com instâncias reais iniciadas pelo Testcontainers. Implementações em memória não substituem esses testes.
- Fluxos que passam por mais de um servidor, como levar um jogador de um servidor para outro pelo Velocity, são cobertos por testes de integração quando o projeto tiver a infraestrutura para isso.
- Os testes são determinísticos: não dependem de horário real, rede, ordem de execução ou aleatoriedade sem controle.
- Nunca apague, pule, desative ou enfraqueça um teste ou uma asserção para ele passar. Se o teste estiver errado, corrija-o e explique o motivo no pull request.
- Rode os testes que cobrem a mudança antes de dar a tarefa como pronta, e informe os comandos e os resultados.
- Nunca diga que uma validação passou sem tê-la rodado. Quando algo não puder ser rodado, diga isso e explique o motivo.

## Branches

- Siga o fluxo de branches do [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md#branches).
- Crie as branches de trabalho a partir da `development` e abra os pull requests para a `development`.
- Nunca faça commit direto na `production` ou na `homologation`.

## Commits

- Siga o padrão de commit do [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md#commits): Conventional Commits, em inglês, com o escopo igual ao nome da pasta de documentação da feature.
- Commits gerais são proibidos. Divida o trabalho em commits organizados, um por mudança lógica, como descrito em [Commits organizados](CONTRIBUTING.pt-BR.md#commits-organizados). Agrupe por mudança, e não por arquivo.
- Não inclua arquivos sem relação nem mudanças feitas por outra pessoa.
- A atualização da documentação vai junto com a mudança que ela descreve.
- Nunca faça force-push, nunca reescreva histórico já enviado e nunca pule hooks.

## Documentação

- Siga as regras de documentação do [CONTRIBUTING.pt-BR.md](CONTRIBUTING.pt-BR.md#documentação).
- Toda mudança de documentação é feita em inglês e em português, na mesma mudança.
- Antes de terminar uma tarefa, confira se alguma documentação de feature descreve algo que você mudou, e atualize-a.

## Pull requests

- Preencha todas as seções do [modelo de pull request](.github/pull_request_template.md).
- **Validations** lista só as verificações que de fato rodaram, com os resultados.
