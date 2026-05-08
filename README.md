# LagProtector

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/project/lagprotector)

Paper and Folia plugin (1.21.4 and newer) that applies **per-chunk limits** to living mobs and other entities, optional **per-material caps** for mechanics-style blocks, and periodic **auto-clean** (cull) when enabled.

## Requirements

- **Server**: [Paper](https://papermc.io/software/paper) or [Folia](https://papermc.io/software/folia) **1.21.4+**
- **Build**: JDK **21** (Gradle uses the Java 21 toolchain)

## Install

1. Build the release JAR: `./gradlew shadowJar`
2. Copy `build/libs/LagProtector-1.0.0.jar` into your server’s `plugins/` folder
3. Start the server once, then edit `plugins/LagProtector/config.yml`
4. Run `/lagprotector reload` or restart

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/lagprotector reload` | `lagprotector.reload` (default: op) | Reload `config.yml` and restart scheduled cull tasks |

## Configuration

See **[docs/configuration.md](docs/configuration.md)** for every option, limits, and how spawn checks interact with `EntityTypes`, `BlockTypes`, and `AutoClean`.

## Documentation

- [docs/README.md](docs/README.md) — documentation index
- [docs/configuration.md](docs/configuration.md) — full configuration reference

## Development

```bash
./gradlew shadowJar           # production jar (tools package excluded)
./gradlew generateDefaultConfig  # overwrites src/main/resources/config.yml with a minimal template
./gradlew compileJava            # compile only
```

The `generateDefaultConfig` task runs `GenerateDefaults` and **replaces** the path configured in `build.gradle` (by default `src/main/resources/config.yml`). Use only when you intend to refresh the checked-in default.

## License

Licensed under the [Apache License, Version 2.0](LICENSE). See [`NOTICE`](NOTICE) for copyright and third-party attribution.
