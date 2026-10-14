[English](CONTRIBUTING.md) | **Português**

# Como contribuir

Estas regras valem para todo mundo que muda este repositório, pessoas e agentes de IA. Os agentes de IA também seguem o [AGENTS.md](AGENTS.md).

Commits e pull requests são em inglês. A documentação é em inglês e em português.

## Commits

Os commits seguem o [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/) e são escritos em **inglês**.

```text
<type>(<scope>): <description>

[body]

[footer]
```

### Cabeçalho

- `type` é um dos tipos da tabela abaixo.
- `scope` é o módulo ou a feature afetada, em kebab-case, e é igual ao nome da pasta de documentação da feature (`anticheat` ↔ `docs/features/anticheat/`). Só fica de fora em mudanças que atingem o repositório inteiro.
- `description` usa o imperativo em inglês ("add", e não "added" nem "adds"), começa com letra minúscula e não termina com ponto.
- O cabeçalho inteiro cabe em 72 caracteres.

| Tipo | Quando usar |
|---|---|
| `feat` | Uma feature nova ou uma capacidade nova em uma feature existente. |
| `fix` | Correção de bug. |
| `docs` | Só documentação. |
| `refactor` | Mudança de código que não corrige bug nem muda comportamento. |
| `perf` | Mudança que melhora desempenho. |
| `test` | Só adiciona ou corrige testes. |
| `build` | Sistema de build ou dependências. |
| `ci` | Configuração e workflows de CI. |
| `chore` | Manutenção que não se encaixa nos outros tipos. |
| `revert` | Reverte um commit anterior. |

### Corpo e rodapé

- O corpo explica **por que** a mudança foi feita, e não o que o diff já mostra. Quebre as linhas em 72 caracteres.
- Mudanças incompatíveis levam `!` depois do tipo ou do escopo (`feat(core)!: ...`) e um rodapé `BREAKING CHANGE:` dizendo o que quebra e como migrar.
- Referencie issues no rodapé com `Refs #N`.

### Exemplos

```text
feat(anticheat): flag reach above 3.1 blocks
fix(lobby): keep the selected server when the menu reopens
docs(anticheat): document the reach check thresholds
refactor(core)!: split account storage from profile storage
```

### Commits organizados

Commits gerais são proibidos. O trabalho é dividido em commits separados, e cada um agrupa uma única mudança lógica.

- Uma mudança lógica por commit. Não misture mudanças sem relação, como uma feature nova, uma correção de bug em outro módulo e uma atualização de dependência no mesmo commit.
- Agrupe por mudança, e não por arquivo: um commit pode mexer em vários arquivos quando eles fazem parte da mesma mudança, e uma mudança não precisa de um commit por arquivo.
- Descrições vagas como `update files`, `fixes`, `changes` ou `wip` são proibidas. A descrição diz o que o commit muda.
- A documentação de uma mudança vai junto com ela, no mesmo commit ou no mesmo pull request.

```text
feat(anticheat): flag reach above 3.1 blocks
test(anticheat): cover reach at the 3.1 block boundary
docs(anticheat): document the reach check thresholds
```

Assim não:

```text
chore: anticheat, lobby and dependency updates
```

### Histórico

- Nunca faça force-push nem reescreva histórico que já foi enviado para uma branch compartilhada.

## Documentação

Todo arquivo de documentação existe em **inglês e português**. As duas versões dizem a mesma coisa, e nenhuma entra sem a outra.

| Idioma | Arquivo |
|---|---|
| Inglês | `README.md`, `CONTRIBUTING.md`, `AGENTS.md`, `docs/**/README.md` |
| Português | `README.pt-BR.md`, `CONTRIBUTING.pt-BR.md`, `AGENTS.pt-BR.md`, `docs/**/README.pt-BR.md` |

O `CLAUDE.md` não é documentação: ele só aponta o Claude Code para o `AGENTS.md`.

### Toda feature tem documentação própria

Cada feature tem sua própria pasta em [`docs/features/`](docs/features/), com o mesmo nome do escopo do commit:

```text
docs/features/anticheat/
├── README.md
└── README.pt-BR.md
```

- **Feature nova:** o pull request que cria a feature também cria a documentação dela, a partir do [`docs/features/_template/`](docs/features/_template/).
- **Feature alterada:** o pull request que muda comportamento, comandos, permissões, configuração ou integrações também atualiza a documentação da feature, nos dois idiomas.
- **Feature removida:** o pull request que remove a feature também remove a documentação dela.
- Uma mudança sem efeito visível (refatoração interna, testes, CI) só precisa atualizar a documentação se ela citar algo que mudou.

A documentação descreve como a feature funciona **hoje**. O histórico fica no git e nos pull requests.

## Pull requests

- O título segue o padrão de commit: `feat(anticheat): flag reach above 3.1 blocks`.
- O corpo segue o [modelo](.github/pull_request_template.md), que o GitHub preenche sozinho:
  - **Problem:** o que está errado ou faltando, e por que isso importa.
  - **Solution:** o que muda, do ponto de vista do jogador, da equipe ou de quem opera a rede.
  - **How it was done:** a abordagem técnica, as principais decisões e as alternativas descartadas.
  - **Validations:** toda verificação que de fato rodou, com o resultado. Uma verificação que não pôde rodar entra na lista com o motivo.
- O checklist de documentação do modelo é obrigatório.
- Os pull requests são escritos em inglês.
