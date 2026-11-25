# Arcane Mod

Modular glyph-based magic system for Minecraft 1.20.1 (NeoForge/Forge).

**Estado atual do mod:** [docs/STATUS.md](docs/STATUS.md): o que existe, o que não foi visto em jogo e testes instáveis. Histórico das rodadas em [docs/CHANGELOG_DEV.md](docs/CHANGELOG_DEV.md).

**Documento antigo (jul/2026, obsoleto, só referência):** [docs/arquivo/DOCUMENTACAO_COMPLETA.md](docs/arquivo/DOCUMENTACAO_COMPLETA.md).

**Guia completo em português:** [docs/COMO_INSTALAR_E_USAR.md](docs/COMO_INSTALAR_E_USAR.md) — instalação, bancada arcana, primeiros passos no jogo.

**Design do chat × estado do mod (feito, pendente, decisões):** [docs/DESIGN_CHAT.md](docs/DESIGN_CHAT.md)

**Todos os glifos (207), com o que cada um faz:** [docs/GLIFOS.md](docs/GLIFOS.md)

**Checklist de trabalho manual (arte, sons, design):** [docs/TRABALHO_MANUAL.md](docs/TRABALHO_MANUAL.md)

## Requirements

- JDK 17 (the Gradle toolchain is 17; Gradle downloads it if missing)

## Build

```bash
./gradlew build
./gradlew test runGameTestServer   # JUnit + GameTests
./gradlew runClient
```

## Playtest

1. Place **Arcane Workbench**, open GUI, compose glyphs, save
2. Press **V** to cast active inscription
3. `/arcane know all` (operator) — unlock every glyph for testing
   Press **G** for the Grimoire, **V** to cast, **B** to switch spell
4. Place **Mana Crystal** nearby for ambient amplification
5. Use **Arcane Pen** on blocks/chests for world inscriptions

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
