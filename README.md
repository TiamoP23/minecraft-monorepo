# Clans Ecosystem

A Gradle monorepo for a Minecraft plugin ecosystem built on Paper and Velocity.

## Structure

All modules live under `modules/<domain>/<module-name>/`:

```
modules/
├── clans/
│   └── clans-paper/      # PaperMC clans plugin
├── proxy/                 # (future)
└── friends/               # (future)
```

Gradle module paths follow the directory structure: `:modules:clans:clans-paper`.

## Building

```bash
./gradlew :modules:clans:clans-paper:jar        # Build the Paper plugin
./gradlew :modules:clans:clans-paper:runServer   # Run a test server
```

## Adding a new module

1. Create the directory: `modules/<domain>/<module-name>/`
2. Add a `build.gradle.kts` in the module directory.
3. Include it in `settings.gradle.kts`: `":modules:<domain>:<module-name>"`.
4. Add any new dependencies to `gradle/libs.versions.toml`.
