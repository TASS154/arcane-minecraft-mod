# Arcane (mod de magia por glifos)

Minecraft 1.20.1, Forge 47.1.3, Java 17. Responda em português do Brasil.

## Leia primeiro

1. `docs/STATUS.md`: o que existe, o que NÃO foi visto em jogo e quais testes são instáveis (curto, mantido em dia).
2. `docs/GLIFOS.md`: todos os glifos. É **gerado**: nunca edite à mão (ver Geradores).
3. `docs/PERGUNTAS.md`: dúvidas de design em aberto e os padrões adotados.
4. `docs/DESIGN_CHAT.md`: decisões de design.
5. `docs/CHANGELOG_DEV.md`: log longo de cada rodada (histórico; o STATUS é a fonte do estado atual).

Não use `docs/arquivo/DOCUMENTACAO_COMPLETA.md` como fonte: está obsoleto (jul/2026).

## Regras

- Não altere mecânica nem números de balanceamento sem registrar a dúvida em `docs/PERGUNTAS.md` e parar para perguntar.
- Pare e pergunte se a tarefa exigir remover glifo fora de uma lista aprovada, mudar balanceamento ou decidir estética.
- **Não duplicar glifo:** antes de criar um glifo, procure em `tools/glyph_table.py` e `docs/GLIFOS.md` um que já faça o mesmo
  (ou que vire o mesmo com um parâmetro ou `kind`). Ids antigos renomeados ou fundidos entram no mapa de aliases, para
  grimórios e inscrições salvos continuarem válidos.
- Todo código novo vem com GameTest (`src/main/java/com/arcane/gametest/`, uma classe por área, utilitários em
  `GameTestSupport`) ou JUnit (`src/test/java/`). Teste que mexe em estado global (condutor, zonas) ganha lote próprio.
- Jogador falso em GameTest: crie com `TestPlayers.add(h, player)` (sai do mundo quando o teste acaba).
- Se um teste instável falhar, investigue a causa raiz; não só rode de novo.
- GameTest `required = false` documenta uma regra ainda não cumprida (falha sem derrubar a suite; ex.: `GlyphAuditGameTests`).
  Quando a correção for aprovada e feita, torne-o obrigatório.
- O que for visual, sonoro ou de UX e não tiver sido visto ou ouvido por um humano: escreva "NÃO visto em jogo" e liste em
  `docs/PENDENTE_HUMANO.md`.
- Nunca aceite EULA, nunca apague `run/world` nem saves, não adicione dependências sem perguntar.
- Um commit por fase ou rodada, mensagem em português no estilo do repo (frase descritiva, sem prefixo). Atualize
  `docs/STATUS.md` ao fim de cada fase.
- Partícula ou som de elemento: use a fachada `com.arcane.fx.Fx` (e `data/arcane/elements/<id>.json`), nunca
  `ParticleTypes`/`SoundEvents` direto nos caminhos quentes (`VanillaFxUsageTest` confere).
- Valores provisórios ficam em config (`ArcaneConfig`) ou em constantes nomeadas, nunca espalhados.

## Build e testes

Use um JDK 17 (o toolchain do Gradle pede 17). Nesta máquina:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.11+9"
.\gradlew.bat test runGameTestServer --offline   # JUnit + GameTests (log: build/gametest-run/logs/latest.log)
.\gradlew.bat build --offline                    # jar em build/libs
.\gradlew.bat runClient --offline -PquickPlay=ArcaneTest
```

- O cliente com `run/arcane_autotest.flag` roda o roteiro de `client/DevAutoTest` e salva capturas em `run/screenshots/`.
  Apague o arquivo para jogar normalmente.
- Antes de abrir outro cliente, feche os `java.exe` antigos do `runClient`.

## Onde ficam as coisas

- Código: `src/main/java/com/arcane/` (`execution/` motor de magia e handlers, `glyph/`, `grimoire/`, `ritual/`, `entity/`,
  `client/` render, HUD e telas, `gametest/`).
- Dados: `src/main/resources/data/arcane/` (`glyphs/`, `interactions/`, receitas, estruturas).
- Assets: `src/main/resources/assets/arcane/` (texturas, modelos, sons, `lang/`).
- Geradores (Python 3, sem dependências externas salvo indicação no topo do arquivo), em `tools/`:
  - `glyph_table.py`: **fonte única dos glifos** (id, categoria, custo, tier, parâmetros, nomes e descrições PT/EN, ícone).
  - `gen_glyphs.py`: gera `data/arcane/glyphs/*.json` e as chaves `glyph.arcane.*` dos idiomas.
  - `gen_glyph_docs.py`: gera `docs/GLIFOS.md`.
  - `gen_textures.py`, `gen_art_vfx.py`, `gen_art_anim.py`, `gen_art_ritual.py`, `gen_item_models.py`,
    `gen_hydra_texture.py`, `gen_mage_skins.py`, `gen_seal_texture.py`, `gen_summon_models.py`, `gen_scepter_models.py`,
    `gen_ruins_assets.py`: arte.
  - `gen_sounds.py`: sons sintetizados (`--elements` só os de elemento). `gen_art_vfx.py --element-fx`: sprites de elemento.
  - `dev/contact_sheet.py`: folha de contato elemento × forma (escreva `elementos` em `run/arcane_autotest.flag`).
  - `gen_data.py`: receitas e dados. `gen_test_structure.py`: estruturas dos testes.
- Depois de mudar `glyph_table.py`: `python tools/gen_glyphs.py` e `python tools/gen_glyph_docs.py`.
