# Estilo de magia único por jogador: desenho para aprovação

Estado: **só desenho, nada implementado.** Pedido de Everton: "cada jogador tem um estilo de magia único" e "reformular/revisar as interações dos glifos para poder criar um sistema de poder único".

## Ideia central
O poder deixa de ser só o custo do glifo. Cada jogador acumula **afinidade** por elementos/escolas (fogo, gelo, raio, vida, vazio, força, terra...) conforme **usa** glifos, e o conjunto das afinidades define o **estilo** dele. O estilo muda as magias dele de formas pequenas e legíveis, não gera números gigantes.

## Proposta (a validar)
1. **Afinidade por elemento (0 a 100):** sobe ao lançar magias daquele elemento (já existe `SpellElement.of(plan)`), com rendimento decrescente. Guardada no grimório do jogador (anexo já existente, sincronizada pelo `S2CGrimoireSyncPacket`).
2. **Estilo = os dois elementos mais altos** (ex.: Fogo + Vazio = "Brasa Negra"). Dá nome, cor de runas e partículas próprios (o visual novo já tem cor por elemento).
3. **Bônus do estilo (pequenos e de tipo diferente):**
   - 1º elemento: +10% a +25% de potência naquele elemento e custo de mana -10%.
   - 2º elemento: um efeito secundário (ex.: Fogo dominante deixa brasa no alvo; Gelo dominante atrasa o alvo).
   - Combinação de dois elementos específicos: uma interação única (ex.: Raio + Água = a Cadeia salta mais longe em alvos molhados).
4. **Custo do especialista:** elementos opostos pesam um contra o outro (fogo alto reduz gelo), para o estilo ser uma escolha e não só acumulação.
5. **Tudo em config central** (curvas, tetos, tabelas de interação) e aplicado pelo `ModifierPipeline`, igual aos outros modificadores.

## Revisão de interações dos glifos
Hoje as interações elemento x alvo estão em `ElementInteractions` e `InteractionRules`, e cada efeito aplica a própria regra. Antes do estilo, vale uma auditoria: listar para cada glifo (a) o elemento, (b) de que parâmetro vem o poder, (c) o que já interage com o quê. Saída: uma tabela única (`docs/INTERACOES.md`) e um ponto único de "poder" lido por todos os efeitos.

## Perguntas para Everton (nesta ordem)
1. A afinidade é por **uso** (cresce lançando magia) ou por **escolha** (o jogador distribui pontos)?
2. O estilo pode **mudar** com o tempo (decai se parar de usar) ou é permanente?
3. Deve existir um **limite de afinidade total** (para obrigar a escolher) ou cada elemento vai a 100 sozinho?
4. Combinações específicas de dois elementos: quer que eu proponha uma lista inicial (uns 8 pares) ou prefere escrever?
5. Isso deve valer também para magos NPC?

## Riscos
- Poder desbalanceado em PvP: o jogo é principalmente solo, então o risco é baixo; ainda assim os bônus ficam pequenos e configuráveis.
- Sincronização: afinidade é estado do jogador, vai pelo mesmo pacote de sincronização do grimório.
