**English** | [Português](AGENTS.pt-BR.md)

# AI agents

Rules for AI agents working on this repository. Everything in [CONTRIBUTING.md](CONTRIBUTING.md) also applies to agents; this file adds what is specific to them.

This file will receive more instructions as the project grows. When a rule here conflicts with a generic habit or default, this file wins.

## Code comments

Comments are forbidden, except `TODO` and `FIXME`.

- Do not write comments of any kind: no Javadoc, no block comments, no inline comments.
- `TODO` and `FIXME` are allowed when needed: `TODO` marks something that is still missing, `FIXME` marks something known to be wrong. Say what is pending and why, and reference the issue when there is one: `// TODO: retry the transfer when the proxy reconnects (#42)`.
- Do not leave commented-out code.
- Let names and structure carry the meaning. When a piece of code seems to need an explanation, extract it into a method or variable whose name explains it.
- Do not remove license headers.

## Tests

- Every behavior change comes with tests that cover it.
- A bug fix starts with a test that reproduces the bug and fails before the fix.
- Test business logic with fast unit tests over domain and service classes, using in-memory fakes. Use a server runtime harness only when the behavior really depends on the server runtime.
- Behavior that depends on PostgreSQL or Redis, such as queries, migrations, transactions, caching and packets, is covered by integration tests against real instances started with Testcontainers. In-memory fakes do not replace them.
- Flows that cross servers, such as moving a player through Velocity, are covered by integration tests once the project has the infrastructure for them.
- Tests are deterministic: no dependency on real time, network, execution order or uncontrolled randomness.
- Never delete, skip, disable or weaken a test or an assertion to make it pass. If a test is wrong, fix it and explain why in the pull request.
- Run the tests that cover the change before declaring it done, and report the commands and results.
- Never claim a validation passed without running it. When something cannot be run, say so and say why.

## Branches

- Follow the branch flow in [CONTRIBUTING.md](CONTRIBUTING.md#branches).
- Create work branches from `development` and open pull requests against `development`.
- Never commit directly to `production` or `homologation`.

## Commits

- Follow the commit standard in [CONTRIBUTING.md](CONTRIBUTING.md#commits): Conventional Commits, in English, with the scope matching the feature's documentation folder.
- Catch-all commits are forbidden. Split the work into organized commits, one per logical change, as described in [Organized commits](CONTRIBUTING.md#organized-commits). Group by change, not by file.
- Do not include unrelated files or changes made by someone else.
- Documentation updates ship with the change they describe.
- Never force-push, rewrite pushed history or skip hooks.

## Documentation

- Follow the documentation rules in [CONTRIBUTING.md](CONTRIBUTING.md#documentation).
- Every documentation change is made in English and Portuguese in the same change.
- Before finishing a task, check whether any feature documentation describes something you changed, and update it.

## Pull requests

- Fill in every section of the [pull request template](.github/pull_request_template.md).
- **Validations** lists only checks that actually ran, with their results.
