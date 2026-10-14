**English** | [Português](CONTRIBUTING.pt-BR.md)

# Contributing

These rules apply to everyone who changes this repository, people and AI agents alike. AI agents also follow [AGENTS.md](AGENTS.md).

Commits and pull requests are in English. Documentation is in English and Portuguese.

## Branches

| Branch | What it holds |
|---|---|
| `production` | What is running in production. It is the default branch. |
| `homologation` | What is deployed to the test servers and being validated. |
| `development` | What was developed and is not in production yet. |

Changes move in one direction:

```text
work branch → development → homologation → production
```

- Work branches are created from `development` and merged back into it through a pull request.
- When a set of changes is ready to be tested, `development` is merged into `homologation` through a pull request.
- After it is validated on the test servers, `homologation` is merged into `production` through a pull request.
- `production` and `homologation` only receive changes through pull requests.

> **TODO:** merging a pull request into `homologation` will upload the files to the test servers and restart them automatically. This automation is not implemented yet.

## Commits

Commits follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) and are written in **English**.

```text
<type>(<scope>): <description>

[body]

[footer]
```

### Header

- `type` is one of the types below.
- `scope` is the module or feature affected, in kebab-case, and matches the feature's documentation folder (`anticheat` ↔ `docs/features/anticheat/`). Omit it only for changes that span the whole repository.
- `description` uses the imperative mood ("add", not "added" or "adds"), starts in lowercase and has no trailing period.
- The whole header fits in 72 characters.

| Type | When to use |
|---|---|
| `feat` | A new feature or a new capability in an existing one. |
| `fix` | A bug fix. |
| `docs` | Documentation only. |
| `refactor` | A code change that neither fixes a bug nor changes behavior. |
| `perf` | A change that improves performance. |
| `test` | Adding or fixing tests only. |
| `build` | Build system or dependencies. |
| `ci` | CI configuration and workflows. |
| `chore` | Maintenance that fits none of the above. |
| `revert` | Reverts a previous commit. |

### Body and footer

- The body explains **why** the change was made, not what the diff already shows. Wrap it at 72 characters.
- Breaking changes add `!` after the type or scope (`feat(core)!: ...`) and a `BREAKING CHANGE:` footer describing what breaks and how to migrate.
- Reference issues in the footer with `Refs #N`.

### Examples

```text
feat(anticheat): flag reach above 3.1 blocks
fix(lobby): keep the selected server when the menu reopens
docs(anticheat): document the reach check thresholds
refactor(core)!: split account storage from profile storage
```

### Organized commits

Catch-all commits are forbidden. Work is split into separate commits, each one grouping a single logical change.

- One logical change per commit. Do not mix unrelated changes, such as a new feature, a bug fix in another module and a dependency bump in the same commit.
- Group by change, not by file: a commit can touch many files when they belong to the same change, and a single change does not need one commit per file.
- Vague descriptions like `update files`, `fixes`, `changes` or `wip` are forbidden. The description says what the commit changes.
- Documentation for a change ships with that change, in the same commit or the same pull request.

```text
feat(anticheat): flag reach above 3.1 blocks
test(anticheat): cover reach at the 3.1 block boundary
docs(anticheat): document the reach check thresholds
```

Not like this:

```text
chore: anticheat, lobby and dependency updates
```

### History

- Never force-push or rewrite history that was already pushed to a shared branch.

## Documentation

Every documentation file exists in **English and Portuguese**. The two versions say the same thing, and neither is merged without the other.

| Language | File |
|---|---|
| English | `README.md`, `CONTRIBUTING.md`, `AGENTS.md`, `docs/**/README.md` |
| Portuguese | `README.pt-BR.md`, `CONTRIBUTING.pt-BR.md`, `AGENTS.pt-BR.md`, `docs/**/README.pt-BR.md` |

`CLAUDE.md` is not documentation: it only points Claude Code to `AGENTS.md`.

### Every feature has its own documentation

Each feature lives in its own folder under [`docs/features/`](docs/features/), named after the commit scope:

```text
docs/features/anticheat/
├── README.md
└── README.pt-BR.md
```

- **New feature:** the pull request that adds it also creates its documentation, starting from [`docs/features/_template/`](docs/features/_template/).
- **Changed feature:** the pull request that changes behavior, commands, permissions, configuration or integrations also updates the feature's documentation, in both languages.
- **Removed feature:** the pull request that removes it also removes its documentation.
- A change with no visible effect (internal refactor, tests, CI) only needs a documentation update if the documentation mentions something that changed.

Documentation describes how the feature works **today**. History lives in git and in pull requests.

## Pull requests

- Work pull requests target `development`. Only promotions target `homologation` and `production`, as described in [Branches](#branches).
- The title follows the commit convention: `feat(anticheat): flag reach above 3.1 blocks`.
- The body follows the [template](.github/pull_request_template.md), which GitHub fills in automatically:
  - **Problem:** what is wrong or missing, and why it matters.
  - **Solution:** what changes, from the player's, staff's or operator's point of view.
  - **How it was done:** the technical approach, the main decisions and the alternatives that were discarded.
  - **Validations:** every check that actually ran, with its result. A check that could not run is listed with the reason.
- The documentation checklist in the template is mandatory.
- Pull requests are written in English.
