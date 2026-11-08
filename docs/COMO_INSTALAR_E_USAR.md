# Arcane — Como instalar e usar no Minecraft

Este guia explica como colocar o mod no jogo, o que é a **Bancada Arcana** e o que fazer nela.

---

## Texturas, sons e arte

Todos os blocos, itens, a GUI da bancada, o grimório, o projétil, o Mago NPC, a armadura e as partículas têm **arte própria** em pixel art (paleta roxa/ciano). Os sons também são próprios (sintetizados).

São artes **provisórias de qualidade**: coerentes entre si e no formato exato do Minecraft. Para trocar qualquer uma, substitua o PNG/OGG mantendo o mesmo caminho e tamanho. Os mais de 40 glifos vêm de uma única tabela (`tools/glyph_table.py`), que gera os JSONs, os textos, os ícones e o `GLIFOS.md`. Os 18 magos são gerados proceduralmente (nenhum arquivo de terceiros). Os geradores ficam em `tools/`:

```bash
pip install pillow numpy soundfile
python tools/gen_textures.py   # texturas em assets/arcane/textures
python tools/gen_sounds.py     # sons em assets/arcane/sounds
python tools/gen_data.py       # receitas, loot, tags e worldgen
python tools/gen_glyphs.py     # glifos (JSON + idiomas)
python tools/gen_glyph_docs.py # docs/GLIFOS.md
```

---

## Requisitos

- **Minecraft 1.20.1**
- **NeoForge ou Minecraft Forge 47.1.3+** (o projeto foi gerado com o MDK NeoForge 1.20.1)
- **Java 17** instalado (JDK, não só JRE)

---

## Opção A — Jogar pelo ambiente de desenvolvimento (mais fácil para testar)

1. Instale o **JDK 17** (Eclipse Temurin recomendado).
2. Abra o terminal na pasta do projeto (`mod/`).
3. Configure o Java 17:

   **Windows (PowerShell):**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
   ```

4. Compile e abra o jogo:
   ```powershell
   .\gradlew.bat runClient
   ```

   Na primeira vez o Gradle baixa o Minecraft e as dependências (pode demorar vários minutos).

5. O mod **Arcane** já estará carregado. Crie um mundo no criativo para testar.

### Se der erro de SSL ao compilar

Erro `PKIX path building failed` significa que o Java não confia no certificado da rede (antivírus ou proxy com inspeção HTTPS). Faça o Java usar os certificados do Windows antes de compilar:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\gradlew.bat build
```

### Se der `Failed setting boot class path`

O JDK apontado está corrompido (falta `lib/modules`). Reinstale o JDK 17 ou aponte `JAVA_HOME` para outro. O caminho em `gradle.properties` (`org.gradle.java.home`) precisa existir na sua máquina; se não existir, apague a linha ou passe `-Dorg.gradle.java.home=<caminho do JDK 17>`.

---

## Opção B — Instalar o `.jar` no Minecraft normal

1. Compile o mod:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
   .\gradlew.bat build
   ```

2. Pegue o arquivo gerado em:
   ```
   build/libs/arcane-0.1.0.jar
   ```

3. Instale o **Forge 1.20.1** (versão **47.1.3** ou compatível) no launcher oficial:
   - [https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)

4. Copie `arcane-0.1.0.jar` para a pasta `mods` do Minecraft:
   ```
   %appdata%\.minecraft\mods\
   ```

5. No launcher, escolha o perfil **Forge 1.20.1** e inicie o jogo.

> **Servidor:** o mesmo `.jar` vai na pasta `mods` do servidor Forge 1.20.1. Cliente e servidor precisam do mod instalado.

---

## Primeiros passos no jogo

### 1. Pegar os itens

Abra o inventário criativo e vá na aba **Arcane**, ou use comandos:

```
/give @s arcane:arcane_workbench
/give @s arcane:arcane_pen
/give @s arcane:common_ink
/give @s arcane:mana_crystal
```

No sobrevivência, tudo tem receita (veja o livro de receitas). Os **Cristais de Mana** aparecem nas cavernas (entre Y -56 e Y 24); minerar com picareta de ferro dá fragmentos de ametista, com Toque de Seda dá o cristal. **Notas de Glifo** aparecem em baús de masmorras, minas abandonadas, bibliotecas de fortaleza, templos, mansões e cidades antigas.

### 2. Glifos

Você começa conhecendo **Eu, Projétil e Dano** e com uma magia pronta, a **Faísca** (Projétil → Dano). Os outros 39 glifos você descobre jogando (veja [GLIFOS.md](GLIFOS.md) para a lista completa, com o que cada um faz):

- **Veja um mago conjurar** perto de você: você passa a suspeitar dos glifos dele.
- **Experimente na Bancada** com glifos suspeitos (`?`): salvar não grava, mas aumenta o entendimento até identificar.
- **Estude notas de glifo** (botão direito): identifica na hora. Vêm de baús e dos magos mortos.

Para testar tudo de uma vez (precisa de permissão de operador):

```
/arcane know all
/arcane know arcane:fire
```

---

## O que é a Bancada Arcana e o que fazer nela

A **Bancada Arcana** (`arcane:arcane_workbench`) é o local onde você **monta suas magias** combinando glifos. É o coração do sistema — não é um crafting table comum.

### Como abrir

1. Coloque o bloco no chão.
2. **Clique com o botão direito** no bloco.

### O que fazer na interface

| Área | Função |
|------|--------|
| Campo no topo | Nome da magia (ex: `Bola de Fogo`) |
| **Glifos conhecidos** (esquerda) | Só aparecem os glifos que você conhece. Clique para adicionar. Role com a roda do mouse. Passe o mouse para ver categoria, custo e descrição. Glifos com **?** ainda não foram identificados |
| **Composição** (direita) | A inscrição em ordem. Clique numa linha para selecioná-la |
| **↑ ↓** | Move o glifo selecionado (a ordem muda o resultado) |
| **✕** ou Delete | Remove o glifo selecionado |
| **Limpar** | Esvazia a composição |
| Custo e prévia | Soma de mana e aviso de erro (ex: Forma fora do início). A validação final é do servidor |
| **Salvar** | Valida e grava no grimório. A magia salva vira a ativa |
| **Conjurar** | Conjura a magia ativa com a precisão da bancada |
| **◀ ▶** (rodapé) | Troca a magia ativa entre as salvas |
| **Editar** (rodapé) | Carrega a magia ativa na composição. Salvar com o mesmo nome **substitui** a antiga |
| **Roda do mouse** sobre uma linha | Ajusta o parâmetro principal do glifo (dano, duração, raio...). Shift: passo 5x maior. O custo na tela acompanha |

Composições com glifos ainda não identificados **não são salvas**, mas contam como experimento: cada tentativa aumenta seu entendimento (Suspeito → Parcial → Identificado).

### Exemplo: montar uma bola de fogo

1. Clique em **Projétil**, depois **Dano**, depois **Fogo**.
2. Digite o nome `Bola de Fogo`.
3. Clique **Salvar**.

A ordem importa: `Projétil → Dano → Fogo` é diferente de `Projétil → Fogo → Dano`.

### Depois de salvar

1. Pressione **V** para conjurar a magia ativa.
2. Pressione **B** para trocar entre as magias salvas.
3. Cada conjuração gasta **mana** (barra ciano à direita da hotbar).

### Por que usar a bancada em vez de conjurar no mundo?

| Na bancada | Fora da bancada |
|------------|-----------------|
| 100% de poder | 30% de poder |
| Custo normal de mana | ~75% a mais de mana |
| Salva no grimório | Não salva automaticamente |
| Precisão máxima | Desenho impreciso |

**Sempre desenhe e salve magias importantes na bancada.**

---

## Grimório, mana e morte

- Você começa com um **grimório flutuante** (livro roxo que gira e folheia) ao lado do personagem.
- Ao **morrer**, o grimório cai no chão como item. **Recolha-o**: sem ele sua mana máxima cai para **50%** (o HUD avisa).
- A mana **regenera sozinha** com o tempo.

### O grimório sempre cai (e leva tudo)

- Ao **morrer**, o grimório **sempre** cai no chão **com todo o seu conteúdo** (magias, descobertas, diário, páginas). Ele não some com o tempo e resiste a fogo e lava (só o vazio o destrói), e fica brilhando.
- Você fica com um **grimório provisório** em branco (e metade da mana) até recuperar o original ou até passarem **3 dias de jogo**; depois disso o provisório passa a ser o seu.
- **Pegue-o do chão** (ou Shift + botão direito): tudo volta, somado ao que você aprendeu nesse meio tempo.
- **Outro jogador** que o encontrar pode, com Shift + botão direito, **estudá-lo**: copia as fórmulas, mas só passa a *suspeitar* dos glifos (precisa pesquisar para usar).
- **Páginas:** o grimório guarda tantas magias quantas páginas tiver (começa com 8). **Páginas Arcanas** (achadas em estruturas de nível 2+) acrescentam 2 cada, e o grimório evolui visualmente: Caderninho, Livro comum, Livro grosso, Livro flutuante, Páginas em órbita, Só páginas, Grimório infinito.
- **Pergaminho:** na aba Magias, **Copiar** escreve a magia num pergaminho (1 papel + 1 tinta comum). Quem o recebe pode lançá-lo ou, com Shift + botão direito, estudá-lo. É assim que magos compartilham magias.
- A aba Magias mostra o **selo** da magia: formas encaixadas, cada glifo envolvendo o anterior (triângulo = Forma, círculo = Efeito, quadrado = Filtro, losango = Modificador, pentágono = Condição).

### HUD

À direita da hotbar ficam a barra de mana, o nome da magia ativa e avisos de zona: **Zona de mana ×N** perto de cristais e **Mana Poisoning!** quando a exposição está te ferindo. A barra fica azul durante o envenenamento.

---

## Outros sistemas (resumo)

| Sistema | Como usar |
|---------|-----------|
| **Cristal de Mana** | Magias ficam mais fortes perto dele (até ×32). Ficar muito tempo perto causa Mana Poisoning |
| **Máscara Arcana / Manto Isolante** | Armaduras (cabeça / peito) que reduzem o Mana Poisoning em 30% / 40% |
| **Grimório** (item) | Botão direito ou tecla **G** abre o grimório: aba **Magias** (ativar e apagar), **Glifos** (os que você conhece e quantos faltam) e **Diário** (como você descobriu cada um). Craft: livro + ametista + tinta comum. Se o dono morrer, o item cai e só ele o recupera |
| **Página Arcana** | Botão direito: +2 páginas no grimório (espaço para mais magias) |
| **Barreira Arcana** | Bloco criado pela forma Barreira: parede translúcida temporária (até 30 s) |
| **Pena Arcana** | Pena na mão principal e tinta na secundária, clique direito num bloco: grava a magia ativa. Em baú dispara **ao abrir**; em qualquer outro bloco dispara **quando uma criatura pisa** (ex.: Eu + Dano vira armadilha). Agachado apaga. Quebrar o bloco também apaga. Consome 1 tinta (fora do criativo); tinta de sangue custa 2 de vida; com tinta invisível a inscrição não solta faíscas |
| **Nota de Glifo** | Clique direito para aprender o glifo indicado na dica do item |
| **Mago NPC** | 18 variantes (6 elementos × 3 estilos): Fogo, Gelo, Tempestade, Veneno, Estouro e Vento. Cada elemento conjura uma magia diferente com o mesmo motor do jogador. Aparece naturalmente no Overworld (raro) ou pelo ovo gerador. Ao morrer, dropa uma nota de um glifo da magia dele |
| **Projetor de Glifos / Espelho de Simetria** | Registrados e com arte, mas **ainda sem função** (dependem de design de rituais) |
| **Glifo Ativar** | Dispara um ritual armazenado num altar por perto (até 48 blocos). Veja a seção Rituais abaixo |

---

## Rituais

Um ritual executa uma magia com poder e área ampliados, num círculo desenhado no chão em volta de um **Altar Arcano**.

1. Coloque o **Altar Arcano** no chão.
2. Com o **Giz Arcano** (ou a **Bússola Arcana**, que desenha o círculo inteiro de uma vez gastando tinta), trace as células do círculo. O tamanho do círculo define o nível.
3. Com a magia ativa no grimório, use o altar (clique direito). Magias com glifos exclusivos de ritual só funcionam aqui.
4. Depois de executado, os traços do círculo acendem (brilho ciano) e soltam partículas.

| Nível | Raio | Poder | Área |
|-------|------|-------|------|
| Menor | 4 | ×1,5 | ×1,5 |
| Médio | 8 | ×3 | ×2,5 |
| Maior | 16 | ×10 | ×4 |
| Lendário | 16 | ×10 | ×4 (regra especial, sujeita à sua confirmação) |

- O círculo precisa estar pelo menos **85% completo**; traços faltando reduzem a chance de funcionar.
- Alguns glifos pedem **componentes físicos** consumidos no altar (ver `docs/GLIFOS.md`, coluna ritual).
- Depois de cada ritual o altar tem um tempo de recarga de 5 s.
- Os valores (×1,5 / ×3 / ×10, 85%) ficam em `RitualSettings` e são provisórios.

## Visuais

- Cada magia tem um **elemento visual** pelo primeiro efeito com identidade própria: fogo (brasas), gelo, raio, veneno, força, vazio e vida. Ele define a cor do projétil e as partículas das formas, do impacto e da conjuração. Não muda a mecânica.
- O **grimório flutuante** muda de capa conforme o estágio (1 a 4), cresce e solta faíscas a partir do estágio 3.
- A **bancada** tem modelo 3D próprio, o cristal de mana é animado, e as GUIs foram redesenhadas.

---

## Abrir direto num mundo (desenvolvimento)

```bash
./gradlew runClient -PquickPlay=NomeDoMundo   # mundo em run/saves/NomeDoMundo
```

Para conferir as telas sem jogar, veja `tools/dev/LEIA-ME.md` (teste que abre o jogo e tira capturas).

---

## Testes automáticos

```bash
./gradlew test               # testes unitários, integridade de assets, compilador e sincronização
./gradlew runGameTestServer  # 28 testes dentro de um mundo (cada glifo, mago, cristal, pena)
```

---

## Comandos úteis

```
/arcane know <namespace:glifo>   → aprende um glifo (precisa ser operador)
/arcane know all                 → aprende todos os glifos (precisa ser operador)
/give @s arcane:glyph_note      → nota de pesquisa
```

---

## Estrutura do projeto (para quem for editar)

```
src/main/resources/data/arcane/glyphs/   → definição dos glifos (JSON)
src/main/java/com/arcane/               → código Java
assets/arcane/lang/pt_br.json           → textos em português
```

Os geradores de texturas, sons e dados ficam em `tools/`. Para trocar uma textura, substitua o PNG em `assets/arcane/textures/` mantendo o nome e o tamanho. Novos glifos via datapack precisam de um ícone em `textures/gui/glyphs/<nome>.png` e das chaves `glyph.<ns>.<nome>` e `glyph.<ns>.<nome>.desc` no arquivo de idioma.

---

## Problemas comuns

| Problema | Solução |
|----------|---------|
| Bloco roxo e preto listrado | Textura faltando: confira o caminho do PNG (glifos novos precisam de ícone próprio) |
| Tecla V diz "Nenhuma magia ativa" | Salve uma inscrição na bancada ou troque com **B**; verifique conflito de teclas em Opções → Controles |
| “Mana insuficiente” | Espere regenerar ou reduza glifos na inscrição |
| “Você ainda não identificou um destes glifos” | Estude uma nota, experimente na bancada, ou (operador) `/arcane know arcane:nome_do_glifo` |
| Mod não aparece no jogo | Confirme Forge 1.20.1 e que o `.jar` está em `.minecraft/mods` |

---

## Próximos passos de desenvolvimento

Veja [TRABALHO_MANUAL.md](TRABALHO_MANUAL.md): o que já foi feito e o que ainda depende de decisão humana (balanceamento, rituais, estruturas, arte final).

Para detalhes técnicos da arquitetura, veja [ARCHITECTURE.md](ARCHITECTURE.md).

## Novidades desta rodada (resumo de uso)
- **Juramentos** (Quietude, Sangue, Luz): modificadores que aumentam a força, mas exigem algo ao conjurar. Quietude: parado no chão. Sangue: custa 4 de vida. Luz: só de dia com o céu aberto.
- **Ao Morrer**: condição que arma a magia em você e a dispara quando morrer.
- **Resíduos e assinatura**: magias de custo 5+ deixam um brilho no ponto de impacto. Agachado + botão direito com o **Projetor de Glifos** analisa o mais próximo.
- **Projetor de Glifos** (botão direito): holograma da magia ativa. **Espelho de Simetria** (botão direito no Altar): completa traços do círculo por simetria.
- **Grimório flutuante**: agachado + botão direito com o grimório vazio mostra ou guarda.
- **Interações**: raio rende na chuva, fogo falha na água, gelo apaga fogo, veneno rende no pântano (tudo na config).

## Juramentos personalizáveis
Cada juramento tem o parâmetro **Promessa (1 a 5)**. Fórmula: `bônus = k × promessa^0,85` e `multiplicador = 1 + bônus` (k: Quietude 0,28, Sangue 0,30, Luz 0,25).

| Promessa | Quietude | Sangue | Luz |
|---|---|---|---|
| 1 | +28% | +30% | +25% |
| 2 (padrão) | +50% | +54% | +45% |
| 3 | +71% | +76% | +64% |
| 4 | +91% | +97% | +81% |
| 5 | +110% | +118% | +98% |

Restrições por promessa: Quietude exige ficar parado (promessa-1) segundos; Sangue custa 2 × promessa de vida; Luz: 1 dia, 2 céu aberto, 3 sem chuva, 4 manhã/tarde, 5 perto do meio-dia. No máximo 2 juramentos diferentes por magia; com 2, os bônus se multiplicam (teto cerca de +360%).

## Como montar um ritual
1. **Altar Arcano** (obsidiana, bloco de ametista, tijolos de pedra e ouro). Coloque num chão plano e firme.
2. **Desenhar o círculo**: Compasso Arcano (ouro, bússola, ametista) com botão direito no altar; agachado troca o tamanho (raio 4 menor, 8 médio, 16 maior); gasta Tinta Comum (1 por 16 traços). Alternativa: Giz Arcano, traço a traço. Traço só existe sobre chão firme.
3. **Circularidade**: o círculo precisa de 85% dos traços (config). Espelho de Simetria completa o que faltar.
4. **Magia**: ative no grimório uma magia. Glifos de ritual exigem nível e componentes: Invocar Lobos (menor, 6 ossos), Invocar Golem (médio, 2 blocos de ferro e 1 abóbora esculpida), Dobrar (maior, 4 pérolas do Ender e 1 obsidiana chorosa), Clima (lendário, 1 para-raios e 1 Estrela do Nether).
5. **Conjurar**: botão direito no altar. Poder = multiplicador do nível (1,5 / 3 / 10) × completude do círculo. Agachado + direito guarda a magia no altar; o glifo Ativar a dispara de até 48 blocos.

## Juramento Personalizado (você escolhe o castigo e o tempo)
Glifo `vow_oath` com dois parâmetros: **tipo** (1 imobilidade, 2 silêncio, 3 mana selada, 4 fragilidade +50% de dano recebido, 5 cegueira) e **segundos** (3 a 600). O castigo começa depois da magia, fica salvo no jogador (sobrevive a relogar; morrer encerra).
Fórmula: `bônus = min(1,5; k × ln(1 + segundos/5))`, k = 0,30 / 0,22 / 0,35 / 0,25 / 0,20. Exemplo: imobilidade por 30 s = +58%; mana selada por 120 s = +111%.
Conta no limite de 2 juramentos por magia. Rituais no altar não são bloqueados pelo silêncio.

## Animações do personagem
Visíveis em 3ª pessoa (F5), para você e para quem estiver perto. Seis clipes encadeados de quadros-chave: conjuração com a mão direita, com a esquerda e golpe final com as duas mãos (três conjurações direcionais seguidas formam um combo; 2 s sem conjurar reinicia), magia em si mesmo (braços cruzam e abrem), magia de área (braços abertos e palmas para baixo) e ritual (sete tempos, termina com golpe no chão). Só visual. Código: `client/anim/PlayerClips.java` (edite os quadros) e `execution/PlayerAnims.java` (escolha do clipe).

## Parâmetros com dois valores (Juramento Personalizado)
Na Bancada, a **roda do mouse** sobre a linha ajusta o 1º parâmetro (o **tipo**, 1 a 5); **Ctrl + roda** ajusta o 2º (os **segundos**, 3 a 600). Shift multiplica o passo por 5. A linha mostra `tipo/segundos`.

## Escrever em blocos (Pena Arcana)
Pena Arcana (ametista, pena e pepita de ouro) na mão principal, uma **tinta** na outra, botão direito num bloco: grava a magia ativa nele. Em baú dispara ao abrir; em qualquer outro bloco dispara quando uma criatura pisa em cima (a criatura vira o alvo, então Eu + Dano numa pedra é uma armadilha). Agachado + botão direito apaga; quebrar o bloco também. Tinta Comum (gasta 1), Tinta de Sangue (mana mais barata, custa 2 de vida) ou Tinta Invisível (sem partículas).

## Regiões: formatos e origem valem para qualquer forma
Toda forma com área (Aura, Onda, Pilar, Barreira) monta uma **Região** (formato, centro, raio, altura) em `execution/Region.java`, e usa a mesma Região para escolher criaturas, criar blocos e mostrar o efeito. Por isso os glifos de formato e origem funcionam em todas elas.
- **Formato natural:** Aura = esfera, Onda = cone, Pilar = coluna, Barreira = parede.
- **Glifos de formato** (trocam o natural): Domo, Esfera, Anel, Cone, Coluna, Parede. Ex.: `Aura, Cone, Dano` fere só quem está à frente; `Onda, Esfera, Dano` fere todos em volta; `Barreira, Coluna` cria um cilindro oco.
- **Origem:** Mira (centro onde você olha, até 32 blocos) e Eu (centro em você, mesmo no Pilar ou na parede).
- **Raio:** Aura e Pilar usam o parâmetro `radius`; a Onda usa o alcance; a Barreira usa `radius` (2º parâmetro, Ctrl + roda). **Área** dobra o raio de todas.
- **Casca de blocos (Barreira):** teto de 2500 blocos. Raio máximo: domo 16, esfera 12, anel 24, cone 12, coluna 16, parede 12.
- Os glifos seguintes atingem os **blocos e as criaturas dentro ou encostadas** (`Barreira, Domo, Cura` cura quem está dentro). O efeito vale uma vez, na conjuração.

## Alvos: criatura, bloco e ponto vazio (e a prévia da bancada)

Toda forma entrega até três tipos de alvo, e cada efeito sabe o que fazer com cada um:

| Efeito | Criatura | Bloco | Ponto vazio (raio que erra, centro do Pilar) |
|---|---|---|---|
| Dano / Corte | fere | quebra blocos muito frágeis (vidro, folhas) / corta plantas | nada |
| Fogo | queima | acende TNT, fogueira, derrete gelo | chama no chão logo abaixo |
| Acender | nada | fogo em cima | chama no chão |
| Cura | cura | faz plantas crescer (como farinha de osso) | idem, no chão |
| Veneno, Lentidão, Velocidade, Escudo… | aplica | deixa uma **nuvem** de efeito ali (2 blocos, 5 s) | idem |
| Empurrar | empurra | onda de choque em quem está perto | idem |
| Congelar | congela | água vira gelo em volta | idem |
| Raio / Explosão | atinge | atinge | atinge o ponto |

O **Toque** nunca entrega ponto vazio (só alcança o que está colado). Efeitos que alteram blocos respeitam `mobGriefing` quando quem conjura não é jogador.

### Prévia na bancada
Na bancada, **passe o mouse sobre a linha de estado** (Pronto / ⚠ Veja a prévia, embaixo da lista de glifos). O servidor calcula e mostra: a forma e o que ela alcança, o formato e a origem, e o que cada efeito faz. Combinações que não fazem nada viram **avisos dourados**, por exemplo: Eu + Escavar (Escavar só age em blocos), Raio + Domo (o Raio ignora formato e origem), Toque + Hostil + Escavar (o filtro deixa só criaturas).

## Zonas e eventos (qualquer forma pode ser persistente)

- **Zona** (modificador, 3 de custo): coloque depois da forma. A forma se repete **do mesmo lugar** a cada intervalo, pelo tempo escolhido, reaplicando os efeitos em quem está dentro. Parâmetros: segundos (2–60, padrão 10) e intervalo (0,5–5 s, padrão 1 s); a roda do mouse ajusta o primeiro e Ctrl+roda o segundo.
  - `Aura + Zona + Fogo` = campo em chamas parado onde você estava. `Barreira + Zona` = parede contínua. `Projétil + Zona` = torre que atira. `Aura + Zona + Cura` = área de cura.
  - Limites: 2 zonas suas ao mesmo tempo (a mais antiga acaba), 64 no servidor. Invocações ignoram a Zona. Zonas vivem na memória: somem ao fechar o mundo ou se você morrer.
- **Quando a Zona Acabar** (condição): o trecho seguinte só roda no fim da zona, uma vez. Ex.: `Aura + Zona + Quando a Zona Acabar + Explosão` = uma mina que explode no fim. Sem Zona, nunca acontece (a prévia avisa).
- Por baixo, as zonas usam o **barramento de eventos** (`SpellEvents`): qualquer sistema pode emitir uma chave e qualquer trecho pode esperar por ela. Novos eventos (por exemplo "ao quebrar a barreira") só precisam emitir a chave.

## Tabela de interações, custo e proteção do terreno

- **Interações elemento × ambiente** agora vivem em `data/arcane/interactions/default.json` (recarregável por datapack, `/reload`). Cada regra tem `element`, `when` (lista de condições que precisam valer), `damage_add` (somado ao multiplicador de dano, vezes `elementInteractionStrength`) e/ou `after` (ação depois de acertar). Condições: `raining`, `wet`, `frozen`, `on_fire`, `hot_biome`, `lush_biome`, `biome_tag:<id>`. Ação: `extinguish`. Regras inválidas são ignoradas e registradas no log.
- **Custo da Zona**: cada pulso reaplica os efeitos, então o custo sobe ×(1 + 0,15 por pulso além do primeiro), até ×6. Ex.: Zona de 10 s a cada 1 s = ×2,35. O número na bancada já inclui isso.
- **Config contra griefing** (`arcane-server.toml`): `worldAlteringSpells` (false = nenhuma magia coloca ou muda blocos) e `blockBreakingSpells` (false = Escavar, Cortar e Dano em blocos frágeis não destroem nada).
- **Teste de matriz**: um GameTest conjura todas as combinações forma × efeito, forma × filtro, região × formato/origem × efeito e forma × Zona (centenas de combinações) e exige que nenhuma lance exceção nem seja inválida.

## Imunidade do conjurador, Explosão Destrutiva e Cadeia
- **Imunidade:** efeitos nocivos (dano, corte, fogo, lentidão, veneno, cegueira, definhar, gelo, gravidade, empurrar, atrair, raio, explosões) lançados por formas de área (Aura, Barreira, Pilar, Raio...) **não ferem quem conjurou**. Suas explosões, seus relâmpagos e suas nuvens de poção nociva também o poupam. A forma **Eu** continua ferindo de propósito. Consequência: Pilar/Eu-origem + Fogo não acende mais o chão debaixo de você (chamas não nascem a menos de 1,5 bloco do conjurador).
- **Explosão Destrutiva (`blast`, custo 8, parâmetro raio 1 a 6):** como Explodir, mas **quebra blocos** (se a zona/regra de proteção permitir; senão só causa dano). Explode em cada alvo (no máximo 2 explosões por magia) e você não é atingido.
- **Cadeia (modificador, custo 3):** aplica-se ao efeito anterior. Em Dano, faz o golpe saltar em arcos para criaturas próximas (até 3 saltos, alcance 5, 60% do dano a cada salto). Em Explodir/Explosão Destrutiva, gera novas explosões menores (70% do raio, 5 ticks depois, até 8 extras). Em outros efeitos a prévia avisa que não tem efeito.

## Rodada de ajustes (potência, caneta, círculo à mão, invocados)
- **Potência pela escrita:** a magia escrita na **Bancada** tem 100% de potência, **e mantém isso lançada de qualquer lugar**. Escrita no campo (caneta em papel ou em bloco) fica com **30%**. O custo de mana é o mesmo que a tela mostra (a sobretaxa escondida de 1,75x foi removida: era o motivo de "preciso de 21, tenho 24 e não dispara").
- **Escrever em papel:** no Grimório, selecione a magia e use **Escrever em papel** (precisa de Pena Arcana no inventário, 1 papel e 1 tinta comum; grátis no criativo). O pergaminho sai com 30% de potência.
- **Caneta num bloco:** botão direito num bloco com a Pena (tinta na mão secundária) **abre a interface de desenho**: você monta a magia que quer escrever ali (não usa a magia selecionada) e clica em **Inscrever**. Fica com 30% de potência. Agachado + botão direito apaga.
- **Círculo à mão:** Pena + botão direito num Altar Arcano (sem agachar) desenha o círculo do ritual **sem Compasso**, com **raio 4, 8, 16, 32 ou 64** (até 128x128) gastando **metade da tinta**. Agachado + botão direito no ar troca o raio. Acima de 16 conta como ritual maior. O Compasso continua em 4/8/16.
- **Magia guardada no altar não some mais:** o glifo Ativar dispara de novo a cada vez (respeitando o descanso do altar) e cobra a mana de cada disparo.
- **Colunas e anéis:** a coluna do Pilar e a do ritual agora **descem do céu**; Pilar, ritual e Raio têm um **círculo de runas** na base.
- **Livro flutuante:** acompanha posição e visão do jogador com atraso suave (ease), em 1ª e 3ª pessoa; as páginas aparecem em 1ª pessoa.
- **Invocados próprios:** Invocar Lobos agora tem `count` (1–6) e `size` (1–3: Espectral, Alfa, Fenrir); Invocar Golem tem `count` (1–3) e `size` (1–3: Guardião, Colosso, Titã). Cada tamanho tem **modelo e textura próprios** (chifres, ombreiras, coroa, cristais), mais vida e dano. Gerados por `tools/gen_summon_models.py`.
- **Diário:** campo para escrever suas próprias anotações na aba Diário do Grimório.
- **Glifos novos no criativo:** ao entrar no mundo em modo criativo, glifos adicionados depois (como Explosão Destrutiva e Cadeia) entram no grimório sozinhos. Fora do criativo: `/arcane know all`.
