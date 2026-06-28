# Agent Guidelines

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>

Longer description of what was changed and why, if
the title alone is insufficient.
```

### Types

- `feat` — new feature
- `fix` — bug fix
- `docs` — documentation only
- `style` — formatting, missing semicolons, etc. (no production change)
- `refactor` — code change that neither fixes nor adds
- `perf` — performance improvement
- `test` — adding or fixing tests
- `build` — build system or dependencies
- `ci` — CI configuration
- `chore` — scaffolding, tooling, config, miscellaneous

### Co-authored-by

When working autonomously, append a `Co-authored-by` trailer

---

## Project Structure

This is a Gradle monorepo. All modules follow the directory convention:

```
modules/<domain>/<module-name>/
```

Gradle module paths: `:modules:<domain>:<module-name>` (e.g., `:modules:clans:clans-paper`).

## Version Catalog

All dependency versions are declared in `gradle/libs.versions.toml`. Use `libs.*` accessors in build files — never hardcode versions.

## JVM Target

All modules target JVM 21. Use `jvmToolchain(21)` in each module's `build.gradle.kts`.

## Core Module Policy

Core modules (e.g., `clans-core`, `proxy-core`, `friends-core`) must not depend on Minecraft APIs (Paper, Velocity, etc.). They contain only domain models and business logic.

## Build Commands

```bash
./gradlew projects                                              # List all modules
./gradlew :modules:clans:clans-paper:compileKotlin              # Compile a module
./gradlew :modules:clans:clans-paper:jar                        # Build a plugin jar
./gradlew :modules:clans:clans-paper:runServer                  # Run test server
```
