# Arcane — Como instalar e usar no Minecraft

Este guia explica como colocar o mod no jogo, o que é a **Bancada Arcana** e o que fazer nela.

---

## Sobre as texturas (blocos roxo/preto)

Sim: na versão atual **não existem texturas artísticas próprias** para todos os itens. Alguns blocos e itens usam **placeholders** (texturas vanilla reaproveitadas) para o mod funcionar sem erro visual grave.

| Item / bloco        | Aparência atual (placeholder)      |
|---------------------|------------------------------------|
| Bancada Arcana      | Bloco roxo escuro (crying obsidian) |
| Cristal de Mana     | Bloco de ametista                  |
| Pena Arcana         | Pena vanilla                       |
| Tintas              | Saco de tinta vanilla              |
| GUI da bancada      | Fundo roxo escuro simples (sem arte)|

Isso é **normal em desenvolvimento**. O sistema de magia funciona; as artes podem ser adicionadas depois em `assets/arcane/textures/`.

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

Alguns antivírus ou redes corporativas bloqueiam downloads do `maven.neoforged.net`. Tente outra rede, desative inspeção HTTPS do antivírus temporariamente, ou importe o certificado no Java.

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

### 2. Desbloquear glifos para teste

O mod não dá feitiços prontos. Você precisa **conhecer glifos** antes de usá-los. Para testar, use:

```
/arcane know arcane:self
/arcane know arcane:projectile
/arcane know arcane:damage
/arcane know arcane:fire
/arcane know arcane:heal
/arcane know arcane:push
```

Sem isso, a bancada monta a inscrição, mas a conjuração pode falhar por “glifo desconhecido”.

---

## O que é a Bancada Arcana e o que fazer nela

A **Bancada Arcana** (`arcane:arcane_workbench`) é o local onde você **monta suas magias** combinando glifos. É o coração do sistema — não é um crafting table comum.

### Como abrir

1. Coloque o bloco no chão (aparece como um cubo roxo escuro por enquanto).
2. **Clique com o botão direito** no bloco.
3. Abre a interface de construção de magias.

### O que fazer na interface

| Elemento | Função |
|----------|--------|
| Campo de texto no topo | Nome da sua magia (ex: `Bola de Fogo`) |
| **+ Self** | Adiciona glifo de forma “em si mesmo” |
| **+ Projectile** | Adiciona forma de projétil |
| **+ Damage** | Adiciona efeito de dano |
| **+ Fire** | Adiciona efeito de fogo |
| **Clear** | Limpa a lista de glifos |
| **Salvar** | Grava a inscrição no seu grimório |

### Exemplo: montar uma bola de fogo

1. Clique **+ Projectile**
2. Clique **+ Damage**
3. Clique **+ Fire**
4. Digite o nome: `Bola de Fogo`
5. Clique **Salvar**

A ordem importa: `Projétil → Dano → Fogo` é diferente de `Projétil → Fogo → Dano`.

### Depois de salvar

1. Pressione a tecla **V** (conjurar magia ativa).
2. A magia salva mais recentemente vira a **inscrição ativa**.
3. Gasta **mana** ao conjurar.

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

- Você começa com um **grimório flutuante** ao lado do personagem (entidade invisível/simples por enquanto).
- Ao **morrer**, o grimório cai no chão como item. **Recolha-o** — sem ele sua mana máxima cai para **50%**.
- A mana **regenera sozinha** com o tempo.

---

## Outros sistemas (resumo)

| Sistema | Como usar |
|---------|-----------|
| **Cristal de Mana** | Coloque perto de você; magias ficam mais fortes (até ×32 perto do bloco) |
| **Pena Arcana** | Escrever inscrições em blocos do mundo (em desenvolvimento) |
| **Nota de Glifo** | Item de loot; clique direito para aprender um glifo |
| **NPC Mago** | Inimigo que conjura magias; dropa notas ao morrer |

---

## Comandos úteis

```
/arcane know <namespace:glifo>   → aprende um glifo (debug/teste)
/give @s arcane:glyph_note      → nota de pesquisa
```

---

## Estrutura do projeto (para quem for editar)

```
src/main/resources/data/arcane/glyphs/   → definição dos glifos (JSON)
src/main/java/com/arcane/               → código Java
assets/arcane/lang/pt_br.json           → textos em português
```

Para adicionar textura da bancada no futuro:

1. Crie `assets/arcane/textures/block/arcane_workbench.png` (16×16 ou múltiplos de 16)
2. Atualize `assets/arcane/models/block/arcane_workbench.json` para apontar para `arcane:block/arcane_workbench`

---

## Problemas comuns

| Problema | Solução |
|----------|---------|
| Bloco roxo e preto listrado | Textura faltando; use a versão atual com placeholder ou adicione PNG |
| Tecla V não faz nada | Salve uma inscrição na bancada antes; verifique conflito de teclas em Opções → Controles |
| “Mana insuficiente” | Espere regenerar ou reduza glifos na inscrição |
| “Glifo desconhecido” | Use `/arcane know arcane:nome_do_glifo` |
| Mod não aparece no jogo | Confirme Forge 1.20.1 e que o `.jar` está em `.minecraft/mods` |

---

## Próximos passos de desenvolvimento

- Texturas e modelos 3D para bancada, grimório e glifos
- GUI da bancada com lista de glifos **conhecidos** (não só botões fixos)
- Rituais grandes desenhados no chão
- Dungeons com notas de pesquisa

Para detalhes técnicos da arquitetura, veja [ARCHITECTURE.md](ARCHITECTURE.md).
