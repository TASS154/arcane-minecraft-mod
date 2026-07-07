# Arcane — Modular Glyph Magic

## Core principles

- **Inscription**: ordered list of glyph instances is the only spell format
- **Compiler**: transforms inscriptions into `CompiledExecutionPlan` at compile-time
- **SpellPipeline**: single executor for player cast, NPC, items, blocks, rituals
- **GlyphRegistry**: datapack-driven glyph definitions in `data/arcane/glyphs/`
- **GrimoireState**: player knowledge, spellbook, discovery log
- **EnvironmentalSnapshot**: ambient potency from mana crystals (no glyph hardcoding)

## Packages

| Package | Role |
|---|---|
| `glyph` | Definitions, registry, handler binding |
| `inscription` | Composition, validation, compilation |
| `execution` | Runtime pipeline and handlers |
| `grimoire` | Player grimório state and lifecycle |
| `inscribing` | World/item inscription anchors |
| `ritual` | Large-scale stored inscriptions |
| `ambient` | Crystal zones and mana poisoning |
| `discovery` | Knowledge acquisition and research |

## Extension

Add a JSON glyph + register a handler in `GlyphHandlerRegistry.bootstrap()`.
