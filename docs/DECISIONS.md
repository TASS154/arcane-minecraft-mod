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

## 3. Números do Corte (prompt 3, B4)

**Pergunta.** O Corte passa a ser dano caro, de alvo único e com bônus pela armadura do alvo. Com que números?

**Opções.** Tabela completa em `docs/PERGUNTAS.md`, "Corte: proposta de números". Resumo: (A) dano padrão 6, +4% por
ponto de armadura do alvo, custo 4; (B) dano 3, +5% por ponto, custo 3; (C) dano 6, ignora metade da armadura, custo 4.

**Recomendação:** (A). Pior que o Dano contra alvo nu (1,5 contra 2 HP por mana), melhor contra netherite (0,77
contra 0,47). E ligar o "alvo único" junto com os números.

**Parado esperando:** os números e o alvo único do Corte. Nada mudou no código do Corte (ele já não tem raio, então a
Área não age nele, e não está na lista do Cadeia).

## 4. Forma Chuva (prompt 3, B5)

**Pergunta.** O desenho da forma Chuva está bom para implementar?

**Opções.** O desenho está em `docs/PROPOSTA_FORMA_CHUVA.md` (entrega, "sem teto", tick, Zona e Segurar, custo,
ritual, reações, exemplos e presença). Os pontos com escolha: teto parcial (cada criatura sozinha pelo centro do topo,
ou por qualquer canto) e Chuva dentro de casa (a nuvem desce até o teto, ou falha).

**Recomendação:** o desenho como está; teto pelo centro do topo; dentro de casa a nuvem desce.

**Parado esperando:** a forma inteira (glifo `rainfall`, handler, sons, testes). O efeito Chuva já saiu (vira Molhar
nas magias salvas). A Nuvem de Vapor continua com os outros dois gatilhos; o de "Chuva + Fogo" já aponta para
`rainfall` e volta a valer quando a forma existir.
