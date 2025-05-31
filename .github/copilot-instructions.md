## Tech Stack

- Primary Languages: Scala, Scala.js, Scala Native.
- Scala Versions: Prioritize Scala 2.13 syntax for broad compatibility. Scala 2.12 compatible syntax is also acceptable.
- Scala 3: Use Scala 3 specific syntax **only** within `src/{main,test}/scala3` directories.
- Build Tool: sbt (Scala Build Tool).
- Testing Framework: AirSpec.

## Project Structure and Modules

- This is a multi-module sbt project. Most modules are cross-built for JVM, JS, and Native platforms.
- Platform-specific code:
    - JVM: `.jvm/src/main/scala`, `.jvm/src/test/scala`
    - JS: `.js/src/main/scala`, `.js/src/test/scala`
    - Native: `.native/src/main/scala`, `.native/src/test/scala`
    - Shared code (common to all platforms) resides in `src/main/scala` and `src/test/scala`.
- Core module `airframe-core` is designed for minimal dependencies. Avoid adding new library dependencies to `airframe-core` unless absolutely necessary. For other modules, carefully consider the impact of adding new dependencies.

## Coding style

- To properly format Scala code, use `./sbt scalafmtAll`.
- case classes for configuration should have withXXX(...) methods for all fields and noXXX(...) methods for all optional fields.
- In string interpolation, always enclose expr with bracket `${...}` for consistency.
- Returning `Try[A]` is generally discouraged as it forces a monadic style on the caller. Consider using plain exceptions for unrecoverable errors, or domain-specific error types.

## Dependencies

- Check `libraryDependencies` in `build.sbt` for each module to understand its current dependencies.
- When adding a dependency:
    - Ensure it's available for all relevant platforms (JVM, JS, Native) if the module is cross-built.
    - Prefer libraries already used in other modules if a similar functionality is needed.
    - For `airframe-core`, new dependencies are highly discouraged.

## Test code

- Use `./sbt` command to open sbt shell.
- Use `./sbt projectJVM/Test/compile` to compile the source and test code for JVM projects. Similarly, use `projectJS/Test/compile` for JS projects, and `projectNative/Test/compile` for Native projects. Replace `project` with the actual module name (e.g., `logJVM/Test/compile`).
- To run a specific test class: `./sbt '(moduleName)(JVM|JS|Native)/testOnly *TestClassName'`.
    - Example for a JVM test in `airframe-log` module: `./sbt 'logJVM/testOnly *MyLogTest'`
    - Example for a JS test in `airframe-codec` module: `./sbt 'codecJS/testOnly fully.qualified.TestClassName'`
- Use AirSpec testing framework. Key assertion syntaxes include `shouldBe`, `shouldNotBe`, `shouldMatch`.
- Test names should be concise and descriptive, written in plain English.
- Avoid using mock as it increases the maintenance cost.
- Ensure tests cover new functionality and bug fixes. Aim for good test coverage.
- When a test fails, carefully analyze the stack trace and assertion failures from the sbt output. The output will indicate the exact line of failure.

## Git

- Use gh command for github operations.
- For creating a new branch use, `git switch -c feature/$(date +"%Y%m%d_%H%M%S")-your-feature-description`
- The format of commit messages is `feature: xxx` (for new features), `fix` (bug fixes), `internal` (non-user facing changes), or `doc` based on the code change contents. When describing commit messages, focus on `why` part of the change, not `what` or `how`.
  - For example, `feature: Add XXX to improve user experience` is better than `feature: Add XXX class`
- **Branching**: The branch naming convention `feature/$(date +"%Y%m%d_%H%M%S")` can be adapted for other types of changes (e.g., `fix/`, `doc/`, `internal/`) by replacing `feature/` with the appropriate prefix, optionally followed by a brief description: `fix/$(date +"%Y%m%d_%H%M%S")-correct-off-by-one-error`.
- **Pull Requests (PRs)**:
    - Use `gh pr create` for creating pull requests. Provide a clear title and a detailed body, linking to any relevant issues.
    - Follow .github/pull_request_template.md format for PR descriptions.
    - Prefer squashing commits via auto-merge with `gh pr merge --squash --auto` command when merging PRs to maintain a linear and clean history on the main branch.
