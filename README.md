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
```

## Testing

Run a local Paper test server with the plugin using the **Run** configuration in IntelliJ, or via CLI:

```bash
./gradlew :modules:clans:clans-paper:runServer
```

This starts a Paper 26.2 server on port 25565 with the plugin loaded. The server runs in the foreground — press `Ctrl+C` to stop it.

Connect via Minecraft client to `localhost:25565` and test commands:

```
/pvp              # Toggle PvP OFF <-> ON
/pvp on           # Enable PvP
/pvp off          # Disable PvP
/pvp status       # Show current PvP mode
```

Server files are stored in `run/`.

## Adding a new module

1. Create the directory: `modules/<domain>/<module-name>/`
2. Add a `build.gradle.kts` in the module directory.
3. Include it in `settings.gradle.kts`: `":modules:<domain>:<module-name>"`.
4. Use package names under `de.tiamop23.minecraft.<domain>.<module>`.
5. Add any new dependencies to `gradle/libs.versions.toml`.
