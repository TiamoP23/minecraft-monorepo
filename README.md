# Minecraft Monorepo

A Gradle monorepo for Minecraft plugins and mods. The current modules target Paper, with room for unrelated plugin or mod domains over time.

## Structure

All modules live under `modules/<domain>/<module-name>/`:

```text
modules/
├── clans/
│   └── clans-paper/      # PaperMC clans plugin
├── proxy/                 # (future)
└── friends/               # (future)
```

Gradle module paths follow the directory structure: `:modules:clans:clans-paper`.

Kotlin/JVM packages follow `de.tiamop23.minecraft.<domain>.<module>`.

## Building

```bash
./gradlew :modules:clans:clans-paper:jar        # Build the Paper plugin
./gradlew :modules:clans:clans-paper:runServer   # Run a test server
```

## Adding a new module

1. Create the directory: `modules/<domain>/<module-name>/`
2. Add a `build.gradle.kts` in the module directory.
3. Include it in `settings.gradle.kts`: `":modules:<domain>:<module-name>"`.
4. Use package names under `de.tiamop23.minecraft.<domain>.<module>`.
5. Add any new dependencies to `gradle/libs.versions.toml`.
