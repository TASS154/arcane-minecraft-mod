# Arcane Mod

Modular glyph-based magic system for Minecraft 1.20.1 (NeoForge/Forge).

**Guia completo em português:** [docs/COMO_INSTALAR_E_USAR.md](docs/COMO_INSTALAR_E_USAR.md) — instalação, bancada arcana, primeiros passos no jogo.

**Checklist de trabalho manual (arte, sons, design):** [docs/TRABALHO_MANUAL.md](docs/TRABALHO_MANUAL.md)

## Requirements

- JDK 17+
- Set `JAVA_HOME` to JDK 17

## Build

```bash
./gradlew build
./gradlew runClient
```

## Playtest

1. Place **Arcane Workbench**, open GUI, compose glyphs, save
2. Press **V** to cast active inscription
3. `/arcane know arcane:fire` — unlock glyph for testing
4. Place **Mana Crystal** nearby for ambient amplification
5. Use **Arcane Pen** on blocks/chests for world inscriptions

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
