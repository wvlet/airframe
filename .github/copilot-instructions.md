## Tech Stack

- Scala, Scala.js, and Scala Native
- Use Scala 2.13, Scala 2.12 compatible syntaxes. Scala 3 syntaxes can be used only in src/{main,test}/scala3 folders.
- Each module is a cross-build project. To add JVM/JS/Native-specific code, use .jvm, .js, and .native folders respectively.
  - airframe is designed to have minimal dependencies. Avoid using libraries that are not in the core module.

## Coding style

- To properly format Scala code, use `./sbt scalafmtAll`.
- case classes for configuration should have withXXX(...) methods for all fields and noXXX(...) methods for all optional fields.
- In string interpolation, always enclose expr with bracket `${...}` for consistency.
- Returning Try[A] is a bad design as it forces the user to use the monadic-style. 

## Test code

- Use `./sbt` command to open sbt shell.
- Use `./sbt projectJVM/Test/compile` to compile the source and test code for JVM projects. `./sbt projectJS/Test/compile` for JS projects, and `./sbt projectNative/Test/compile` for Native projects.
- Use `./sbt '(project name)/testOnly *.(test class name)'` to run a specific test class. For cross-build modules, say `xxx`, `(project name) can be `xxxJVM` (for JVM), `xxxJS` (for JS), or `xxxNative` (for Native).
- Use AirSpec testing framework and `shouldBe`, `shouldMatch` assertion syntax. Test names should be concise and descriptive, written in plain English.
- Avoid using mock as it increases the maintenance cost.
- Ensure tests cover new functionality and bug fixes. Aim for good test coverage.
- When a test fails, analyze the stack trace and assertion failures from the sbt output.

## Git

- Use gh command for github operations.
- For creating a new branch use, `git switch -c feature/$(date +"%Y%m%d_%H%M%S")`
- The format of commit messages is `feature: xxx` (for new features), `fix` (bug fixes), `internal` (non-user facing changes), or `doc` based on the code change contents. When describing commit messages, focus on `why` part of the change, not `what` or `how`.
  - For example, `feature: Add XXX to improve user experience` is better than `feature: Add XXX class`
- **Branching**: The branch naming convention `feature/$(date +"%Y%m%d_%H%M%S")` can be adapted for other types of changes (e.g., `fix/`, `doc/`, `internal/`) by replacing `feature/` with the appropriate prefix, optionally followed by a brief description: `fix/$(date +"%Y%m%d_%H%M%S")-correct-off-by-one-error`.
- **Pull Requests (PRs)**:
    - Use `gh pr create` for creating pull requests. Provide a clear title and a detailed body, linking to any relevant issues.
    - Follow .github/pull_request_template.md format for PR descriptions.
    - Prefer squashing commits via auto-merge with `gh pr merge --squash --auto` command when merging PRs to maintain a linear and clean history on the main branch.
