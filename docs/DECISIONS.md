# Decisões que esperam o Everton

Pedido de 25 nov: "decide little things and for big ones, leave at a decisions.md". Grande = mudar mecânica ou balanço
já aprovado, estética marcante, cortar ou fundir glifo, ou algo difícil de desfazer. Cada item traz a pergunta, as
opções, a minha recomendação e o que ficou parado esperando. As decisões pequenas ficam em `docs/PERGUNTAS.md`.

## 1. Assinatura da magia sem o sistema de estilo aprovado (Fase M)

**Pergunta.** O adendo pede que a assinatura venha do "estilo único" do jogador (`docs/ESTILO_UNICO_DESENHO.md`). Esse
sistema é só um desenho: as perguntas dele (afinidade por uso ou por escolha, decai ou não, bônus) não foram
respondidas. De onde a assinatura tira o estilo enquanto isso?

**Opções.**
- (a) Uma contagem só cosmética de quantas vezes o jogador conjura cada elemento; o estilo é o par dos dois mais
  usados. Nenhum bônus. Quando o sistema de estilo for aprovado, a assinatura passa a ler dele.
- (b) Só o UUID do jogador: assinatura fixa, sem relação com o que ele conjura.
- (c) Esperar a aprovação do sistema de estilo e não ter assinatura até lá.

**Recomendação:** (a). Já está assim (`StyleTally`, nos dados que sobrevivem à morte). É fácil de trocar: a assinatura
só lê `StyleTally.top`.

**Parado esperando:** nada. Se a resposta for (b) ou (c), é uma troca de poucas linhas.

## 2. Hidra com afinidades (Fase N)

**Pergunta.** A Hidra deve ganhar afinidades e reações? A proposta está em `docs/PROPOSTA_HIDRA_AFINIDADES.md`
(afinidade por cabeça, ou por fase).

**Opções.** Ver a proposta. Resumo: (a) nada muda; (b) afinidade fixa por estágio; (c) afinidade por cabeça viva.

**Recomendação:** (c), só depois de jogar contra o Guardião de Ruína e ver se o quebra-cabeça de elemento funciona
para jogadores de verdade.

**Parado esperando:** qualquer mudança na Hidra (`HydraEntity` não foi tocada, além de passar o selo e a fraqueza pelo
`PhaseGate` sem mudar o comportamento).
