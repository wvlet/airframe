# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Airframe is a collection of essential building blocks for developing applications in Scala, including:
- Dependency injection (DI) framework
- HTTP/RPC framework
- Object serialization (JSON/MessagePack)
- Logging framework
- Testing framework (AirSpec)
- SQL parser & analyzer
- Various utilities (metrics, control flow, JDBC, etc.)

## Build System

This is a multi-module sbt project using Scala 3 as the default version, with cross-building support for Scala 2.12, 2.13, and Scala Native/JS platforms.

### Essential Build Commands

```bash
# Open sbt shell
./sbt

# Compile all JVM modules
./sbt projectJVM/compile

# Compile specific module (replace moduleName with actual module, e.g., log, surface, di)
./sbt "moduleNameJVM/compile"

# Run tests for all JVM modules  
./sbt projectJVM/test

# Run tests for specific module
./sbt "moduleNameJVM/test"

# Run a specific test class
./sbt 'moduleNameJVM/testOnly *TestClassName'

# Run a single test case (AirSpec) - use quotes around test name pattern
./sbt 'moduleNameJVM/testOnly *TestClassName -- "test name pattern"'

# Compile for other platforms
./sbt projectJS/compile      # Scala.js modules
./sbt projectNative/compile  # Scala Native modules

# Switch Scala version
./sbt "++ 2.13" projectJVM/compile  # Use Scala 2.13
./sbt "++ 2.12" projectJVM/compile  # Use Scala 2.12
./sbt "++ 3" projectJVM/compile     # Use Scala 3
```

### Code Formatting and Linting

```bash
# Format all Scala code
./sbt scalafmtAll

# Format specific module
./sbt "moduleNameJVM/scalafmtAll"

# Check formatting without applying
./sbt scalafmtCheck

# Apply Scalafix rules
./sbt scalafixAll
```

### Testing Framework

This project uses **AirSpec** as the primary testing framework. Key assertion patterns:
- `shouldBe` - equality assertion
- `shouldNotBe` - inequality assertion  
- `shouldMatch` - pattern matching assertion

## Project Structure

```
airframe/
├── airframe-*/           # Core modules (di, log, surface, codec, etc.)
│   ├── src/
│   │   ├── main/scala/  # Shared code across platforms
│   │   └── test/scala/  # Shared test code
│   ├── .jvm/           # JVM-specific code
│   ├── .js/            # Scala.js-specific code
│   └── .native/        # Scala Native-specific code
├── airspec/            # Testing framework
├── sbt-airframe/       # sbt plugin
├── examples/           # Example applications
├── docs/              # Documentation
└── build.sbt          # Main build configuration
```

### Module Naming Conventions

- JVM modules: `moduleNameJVM` (e.g., `logJVM`, `surfaceJVM`)
- JS modules: `moduleNameJS` (e.g., `logJS`, `surfaceJS`)
- Native modules: `moduleNameNative` (e.g., `logNative`, `surfaceNative`)

## Key Modules

### Core Infrastructure
- `airframe-core` - A new core module of Airframe for Scala 3
- `airframe-di` - Dependency injection framework
- `airframe-log` - Logging framework (minimal dependencies)
- `airframe-surface` - Object structure extraction

### Serialization & Data
- `airframe-codec` - MessagePack-based codec
- `airframe-msgpack` - Pure-Scala MessagePack library
- `airframe-json` - JSON parser

### HTTP & RPC
- `airframe-http` - REST and RPC framework
- `airframe-http-netty` - Netty backend
- `airframe-http-grpc` - gRPC backend
- `airframe-http-finagle` - Finagle backend (Scala 2 only)
- `airframe-http-okhttp` - OkHttp client

### Data Processing
- `airframe-sql` - SQL parser & analyzer
- `airframe-parquet` - Parquet format support
- `airframe-jdbc` - JDBC connection pool

### Utilities
- `airframe-control` - Retry and control flow utilities
- `airframe-metrics` - Duration, size, time window representations
- `airframe-ulid` - ULID generator
- `airframe-rx` - Reactive stream interface

## Development Guidelines

### Scala Version Compatibility
- Default: Scala 3
- Use Scala 2.13 compatible syntax for broad compatibility
- Scala 3 specific syntax only in `src/{main,test}/scala3` directories
- Platform-specific code in `.jvm/`, `.js/`, `.native/` subdirectories

### Code Style
- Run `./sbt scalafmtAll` before committing
- Use `${...}` for string interpolation (not `$var`)
- Case classes for configuration should have `withXXX()` and `noXXX()` methods
- Avoid `Try[A]` return types - use plain exceptions or domain-specific errors

### Dependencies
- `airframe-core` and `airframe-log` should have minimal dependencies
- Check existing `libraryDependencies` in `build.sbt` before adding new ones
- Ensure dependencies are available for all target platforms (JVM/JS/Native)

### Testing Best Practices
- Use AirSpec framework for tests
- Test names should be concise and descriptive
- Avoid mocks to reduce maintenance cost
- Run tests sequentially for JMX-related modules to avoid registration conflicts

## Git Workflow

### Branch Creation
```bash
# Feature branch
git switch -c feature/$(date +"%Y%m%d_%H%M%S")-description

# Bug fix branch  
git switch -c fix/$(date +"%Y%m%d_%H%M%S")-description
```

### Commit Message Format
- `feature:` - New features
- `fix:` - Bug fixes
- `internal:` - Non-user facing changes
- `doc:` - Documentation updates

Focus on "why" not "what" in commit messages.

### Pull Requests
```bash
# Create PR
gh pr create --title "Title" --body "Description"

# Merge with squash (preferred)
gh pr merge --squash --auto
```

## Binary Compatibility

Check binary compatibility before releases:
```bash
./sbt mimaReportBinaryIssues
```

The comparison baseline is set in `AIRFRAME_BINARY_COMPAT_VERSION` in build.sbt.

## Release Process

Releases are managed through GitHub Actions:
1. Create release tag: `ruby ./scripts/release.rb`
2. Artifacts automatically published to Maven Central
3. Release notes generated via release-drafter

## Common Development Tasks

### Adding a New Module
1. Add module definition in `build.sbt`
2. Follow existing module structure patterns
3. Add to appropriate project aggregation (jvmProjects, jsProjects, etc.)

### Running Benchmarks
```bash
./sbt "benchmark/run"
./sbt "benchmark/jmh:run -i 10 -wi 10 -f1 BenchmarkClass"
```

### Publishing Snapshots
```bash
./sbt publishSnapshots
```

### Local Publishing
```bash
./sbt publishAllLocal
```

## Troubleshooting

- JMX test failures: Tests in JMX-related modules run sequentially by default
- Scala 2.13 fork: Some tests fork JVM for Scala 2.13 compatibility
- Binary compatibility: Run `mimaReportBinaryIssues` to check compatibility

## Resources

- [Documentation](https://wvlet.org/airframe/docs)
- [Release Notes](https://wvlet.org/airframe/docs/release-notes.html)
- [GitHub Repository](https://github.com/wvlet/airframe)
- [Maven Central](https://search.maven.org/search?q=g:org.wvlet.airframe)