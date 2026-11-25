<!-- Analise escrita a mao em 25 nov; o resto (a partir de "Balanço automático") e gerado por BalanceSimulationGameTests. -->
# Leitura do balanço automático (25 nov 2026)

Como rodar de novo: crie `build/gametest-run/arcane_balance.flag` e rode `.\gradlew.bat runGameTestServer --offline`; o
relatório sai em `build/gametest-run/balance_report.md`. Sem o arquivo, o teste passa na hora. **Nada foi alterado.**

Achados (registrados em `docs/PERGUNTAS.md`):
1. **Explosão domina a eficiência.** Todas as 6 magias mais eficientes têm Explosão: 10 a 13 de dano por mana, contra a
   mediana de ~0,8. O custo dela não cresce com o raio do jeito que o dano em área cresce (3 alvos atingidos por um
   efeito de custo fixo).
2. **Enfraquecer não enfraquece a Explosão:** multiplica `amount/strength/power`, e a Explosão usa `radius` (e
   o dano dela não é parâmetro). Somado ao achado da auditoria (Enfraquecer encarece em vez de baratear), o glifo não faz
   o que a descrição diz.
3. **Corte com Toque é barato para o dano** (6,6 de dano por mana): é dano físico, e a armadura reduziria; os aldeões
   não têm armadura, então este número superestima.
4. **Nenhum laço:** zero trechos descartados pelo orçamento de reentrância nas 120 magias. As mais lentas (~75 ms) somam
   Explosão, Gravidade e Aura grande no mesmo tick.
5. "Caras e sem efeito" são magias de controle (Empurrão, Levitação) contra alvos parados: esperado, não é defeito.

Limite do método: alvos parados, sem armadura, de dia, sem chuva; dano medido em 30 ticks; mana é o custo base do
plano (sem sobretaxa de campo nem bônus de cristal).

---

# Balanço automático (Fase F)

Gerado por `BalanceSimulationGameTests` (semente 20261125, 120 inscrições válidas aleatórias). Alvo: 3 aldeões de 1000 de vida parados à frente. Dano somado nos 3. **Nenhum valor do jogo foi mudado.**

- Conjurações que funcionaram: 120 de 120.
- Eficiência mediana: 0.82 de dano por mana.

## Mais eficientes (dano por mana, acima de 3x a mediana)

| Dano 30 t | Mana | Dano/mana | Magia |
|---|---|---|---|
| 381 | 29 | 13.1 | aura(radius=2.9) + explode(radius=4.9) + area |
| 181 | 18 | 10.0 | touch(range=4.3) + explode(radius=4.6) |
| 248 | 30 | 8.3 | aura(radius=5.7) + explode(radius=5.4) + weaken |
| 53 | 8 | 6.6 | touch(range=3.0) + cut(amount=6.6) |
| 91 | 14 | 6.5 | ray(range=7.8) + explode(radius=2.8) + freeze(duration=26.0 radius=0.6) |
| 150 | 26 | 5.8 | wave(range=9.6) + explode(radius=3.5) + levitate(duration=102.0) |
| 244 | 45 | 5.4 | pillar(radius=3.4) + explode(radius=5.3) |
| 66 | 13 | 5.1 | ray(range=11.8) + slow(duration=263.0 level=2.0) + chain |
| 48 | 11 | 4.3 | touch(range=5.5) + damage(amount=9.8) + fire(duration=68.0) |
| 115 | 27 | 4.3 | ray(range=39.9) + rumble(damage=6.0) + explode(radius=3.8) |
| 61 | 18 | 3.4 | touch(range=4.8) + cut(amount=15.0) |
| 43 | 13 | 3.3 | aura(radius=10.1) + cut(amount=1.3) |
| 176 | 56 | 3.1 | projectile(speed=0.8) + lifesteal(amount=8.4 ratio=0.5) + overcharge + levitate(duration=24.0) + explode(radius=4.5) |
| 18 | 6 | 3.1 | wave(range=4.0) + levitate(duration=51.0) |
| 12 | 4 | 3.0 | projectile(speed=1.5) + poison(duration=37.0) |

## Mais dano numa conjuração

| Dano 5 t | Dano 30 t | Mana | Magia |
|---|---|---|---|
| 381 | 381 | 29 | aura(radius=2.9) + explode(radius=4.9) + area |
| 319 | 319 | 243 | pillar(radius=4.9) + explode(radius=4.5) + overcharge + trail(damage=1.3 duration=552.0) + explode(radius=5.8) |
| 248 | 248 | 30 | aura(radius=5.7) + explode(radius=5.4) + weaken |
| 244 | 244 | 45 | pillar(radius=3.4) + explode(radius=5.3) |
| 207 | 213 | 82 | wave(range=5.2) + poison(duration=322.0 level=0.0) + power + explode(radius=5.6) + gravity(strength=0.7) + random |
| 165 | 210 | 108 | wave(range=15.6) + lifesteal(amount=9.9) + explode(radius=4.6) + push(strength=1.8) |
| 177 | 181 | 18 | touch(range=4.3) + explode(radius=4.6) |
| 176 | 176 | 56 | projectile(speed=0.8) + lifesteal(amount=8.4 ratio=0.5) + overcharge + levitate(duration=24.0) + explode(radius=4.5) |
| 150 | 150 | 26 | wave(range=9.6) + explode(radius=3.5) + levitate(duration=102.0) |
| 128 | 138 | 723 | pillar(radius=4.9) + explode(radius=3.1) + weaken + lifesteal(amount=7.7) + more_range + freeze(duration=221.0 radius=3.8) + area |

## Caras e sem efeito nos alvos (mana alta, dano zero)

| Mana | Magia |
|---|---|
| 190 | pillar(radius=5.0 range=4.8) + levitate(duration=188.0) + push(strength=2.7) + repeat(times=4.0) |
| 129 | pillar(radius=4.0) + push(strength=2.1) + pierce + levitate(duration=89.0) + slow(duration=374.0) |
| 18 | ray(range=24.9) + trail(damage=2.2 duration=268.0) + power + push(strength=1.7) |
| 11 | projectile(speed=1.6) + electrify(duration=455.0) |

## Laços e lentidão (trechos descartados ou conjuração acima de 20 ms)

| Descartados | ms | Magia |
|---|---|---|
| 0 | 78.0 | wave(range=5.2) + poison(duration=322.0 level=0.0) + power + explode(radius=5.6) + gravity(strength=0.7) + random |
| 0 | 69.0 | aura(radius=10.0) + freeze(duration=144.0 radius=4.7) + overcharge + electrify(duration=130.0 mode=1.0) |
| 0 | 51.3 | wave(range=15.6) + lifesteal(amount=9.9) + explode(radius=4.6) + push(strength=1.8) |

## Recusadas pelo jogo (válidas na gramática, mas a conjuração falhou)

| Motivo | Magia |
|---|---|

## Todas

| Mana | Dano 5 t | Dano 30 t | ms | Magia |
|---|---|---|---|---|
| 82 | 207 | 213 | 78.0 | wave(range=5.2) + poison(duration=322.0 level=0.0) + power + explode(radius=5.6) + gravity(strength=0.7) + random |
| 42 | 0 | 4 | 69.0 | aura(radius=10.0) + freeze(duration=144.0 radius=4.7) + overcharge + electrify(duration=130.0 mode=1.0) |
| 108 | 165 | 210 | 51.3 | wave(range=15.6) + lifesteal(amount=9.9) + explode(radius=4.6) + push(strength=1.8) |
| 42 | 0 | 1 | 4.3 | projectile(speed=0.8) + wither(duration=191.0 level=1.0) + lengthen + fire(duration=22.0) + gravity(strength=1.2) + area |
| 49 | 4 | 6 | 0.7 | projectile(speed=2.8) + wither(duration=68.0 level=2.0) + power + trail(damage=2.9) + repeat(times=3.0) |
| 10 | 12 | 12 | 0.8 | wave(range=3.2) + damage(amount=10.5) |
| 58 | 18 | 18 | 1.2 | ray(range=23.0) + lifesteal(amount=5.9) + lifesteal(amount=5.0) + cut(amount=11.7) + pierce |
| 27 | 112 | 115 | 10.2 | ray(range=39.9) + rumble(damage=6.0) + explode(radius=3.8) |
| 57 | 11 | 11 | 0.7 | touch(range=2.3) + rumble(damage=8.7 radius=1.5) + repeat(times=2.0) + poison(duration=75.0) |
| 50 | 0 | 57 | 1.5 | wave(range=3.8) + push(strength=2.9) + random + slow(duration=197.0) + electrify(duration=352.0 mode=0.0) + more_range |
| 85 | 0 | 4 | 1.8 | aura(radius=6.6) + wither(duration=155.0 level=1.0) + gravity(strength=0.8) + repeat(times=3.0) + curse(duration=499.0) + power |
| 70 | 46 | 46 | 1.9 | aura(radius=7.3) + cut(amount=12.0) + lengthen + lifesteal(amount=12.2 ratio=0.2) |
| 70 | 47 | 53 | 2.9 | pillar(radius=3.6 range=5.9) + damage(amount=12.5) + satellites(count=4.0 damage=2.1 duration=225.0) + acid(amount=7.9 corrode=18.0) |
| 54 | 11 | 26 | 1.3 | aura(radius=4.0) + lifesteal(amount=4.9 ratio=0.6) + electrify(duration=528.0) + pierce + fire(duration=122.0) |
| 15 | 1 | 9 | 0.9 | aura(radius=4.2) + curse(duration=279.0) + pierce |
| 27 | 8 | 8 | 0.7 | wave(range=2.9) + lifesteal(amount=14.1) + cut(amount=3.0) |
| 48 | 24 | 39 | 0.8 | touch(range=5.4) + poison(duration=394.0 level=2.0) + satellites(damage=4.7 duration=201.0) + acid(amount=9.9 corrode=32.0) |
| 24 | 23 | 35 | 0.9 | projectile(speed=1.0) + curse(duration=422.0) + power + cut(amount=9.8) |
| 6 | 7 | 18 | 1.0 | wave(range=4.0) + levitate(duration=51.0) |
| 63 | 33 | 60 | 1.0 | touch(range=3.7) + satellites(damage=6.1 duration=278.0) + damage(amount=9.5) + power + acid(amount=9.4) |
| 14 | 12 | 15 | 0.7 | touch(range=4.7) + electrify(duration=121.0 mode=0.0) + cut(amount=3.8) + fire(duration=112.0) |
| 34 | 8 | 23 | 0.7 | touch(range=5.2) + trail(damage=2.2 duration=489.0) + slow(duration=548.0 level=0.0) + more_range |
| 63 | 35 | 38 | 1.4 | touch(range=3.7) + push(strength=0.9) + more_range + freeze(duration=95.0 radius=2.1) + rumble(damage=11.4 radius=4.8) |
| 723 | 128 | 138 | 18.5 | pillar(radius=4.9) + explode(radius=3.1) + weaken + lifesteal(amount=7.7) + more_range + freeze(duration=221.0 radius=3.8) + area |
| 81 | 65 | 77 | 1.2 | ray(range=4.3) + gravity(strength=3.8) + random + curse(duration=432.0) + lifesteal(amount=14.9) |
| 11 | 20 | 48 | 0.9 | touch(range=5.5) + damage(amount=9.8) + fire(duration=68.0) |
| 18 | 4 | 10 | 1.9 | wave(range=13.8) + satellites(count=2.0 damage=1.5) + chain |
| 42 | 6 | 9 | 1.3 | aura(radius=6.7) + electrify(duration=333.0 mode=0.0) + wither(duration=245.0) + weaken |
| 83 | 94 | 94 | 11.1 | wave(range=4.2) + gravity(strength=2.9) + power + explode(radius=3.0) + power + cut(amount=11.2) |
| 14 | 16 | 29 | 1.3 | aura(radius=4.0) + damage(amount=7.7) + electrify(duration=334.0) |
| 156 | 29 | 34 | 5.8 | wave(range=8.3) + rumble(damage=2.1) + overcharge + damage(amount=7.1) + rumble(damage=10.8) + more_range |
| 8 | 6 | 10 | 0.4 | touch(range=3.3) + trail(damage=2.2) + weaken |
| 14 | 5 | 11 | 0.7 | pillar(radius=2.3) + wither(duration=173.0) |
| 38 | 36 | 41 | 3.8 | pillar(radius=3.0) + cut(amount=11.7) + pierce |
| 108 | 12 | 15 | 1.1 | pillar(radius=1.1) + curse(duration=1679.0) + rumble(damage=3.7 radius=7.0) + overcharge + trail(damage=3.0) + weaken |
| 5 | 6 | 6 | 0.5 | projectile(speed=2.9) + damage(amount=2.5) |
| 52 | 13 | 20 | 2.7 | wave(range=11.7) + trail(damage=2.6 duration=83.0) + gravity(strength=1.6) + more_range |
| 106 | 7 | 7 | 0.7 | aura(radius=2.1) + freeze(duration=254.0 radius=4.1) + weaken + lifesteal(amount=9.2) + repeat(times=2.0) + cut(amount=10.1) |
| 18 | 0 | 0 | 0.5 | ray(range=24.9) + trail(damage=2.2 duration=268.0) + power + push(strength=1.7) |
| 253 | 3 | 3 | 0.5 | pillar(radius=4.4) + curse(duration=1493.0) + more_range + curse(duration=2171.0) |
| 30 | 248 | 248 | 8.2 | aura(radius=5.7) + explode(radius=5.4) + weaken |
| 16 | 34 | 34 | 0.6 | ray(range=24.6) + trail(damage=0.8) + gravity(strength=2.9) |
| 45 | 244 | 244 | 8.2 | pillar(radius=3.4) + explode(radius=5.3) |
| 20 | 19 | 19 | 1.7 | aura(radius=11.3) + slow(duration=97.0 level=2.0) + random |
| 52 | 7 | 9 | 0.5 | ray(range=24.6) + curse(duration=736.0) + gravity(strength=1.9) + pierce + fire(duration=114.0) |
| 10 | 5 | 5 | 0.6 | ray(range=5.4) + gravity(strength=1.6) + random |
| 28 | 2 | 5 | 0.4 | touch(range=4.2) + levitate(duration=176.0) + wither(duration=28.0) + poison(duration=198.0 level=2.0) |
| 47 | 42 | 43 | 1.2 | ray(range=26.3) + rumble(damage=11.5 radius=3.3) + area + trail(damage=0.8 duration=405.0) |
| 8 | 1 | 1 | 0.5 | ray(range=19.2) + electrify(duration=308.0 mode=1.0) |
| 10 | 2 | 4 | 0.5 | ray(range=25.2) + fire(duration=154.0) |
| 16 | 19 | 20 | 0.6 | projectile(speed=0.7) + damage(amount=14.2) + push(strength=1.3) |
| 78 | 1 | 1 | 0.5 | aura(radius=2.0) + trail(damage=2.8 duration=95.0) + satellites(count=2.0 damage=7.4) + lifesteal(amount=9.6 ratio=0.6) + chain |
| 14 | 90 | 91 | 2.5 | ray(range=7.8) + explode(radius=2.8) + freeze(duration=26.0 radius=0.6) |
| 27 | 23 | 25 | 0.5 | projectile(speed=1.6) + electrify(duration=396.0) + cut(amount=9.8) + area |
| 8 | 4 | 4 | 1.0 | ray(range=24.9) + satellites(damage=2.2 duration=90.0) |
| 14 | 2 | 6 | 1.1 | aura(radius=9.9) + curse(duration=185.0) |
| 33 | 34 | 45 | 2.1 | wave(range=9.2) + rumble(damage=2.5 radius=2.9) + rumble(damage=9.1) |
| 190 | 0 | 0 | 1.1 | pillar(radius=5.0 range=4.8) + levitate(duration=188.0) + push(strength=2.7) + repeat(times=4.0) |
| 30 | 12 | 15 | 1.2 | aura(radius=3.4) + lifesteal(amount=3.1 ratio=0.5) + area + electrify(duration=126.0) + random |
| 39 | 11 | 11 | 1.0 | pillar(radius=3.8 range=33.7) + rumble(damage=3.2 radius=7.3) + pierce |
| 56 | 176 | 176 | 0.6 | projectile(speed=0.8) + lifesteal(amount=8.4 ratio=0.5) + overcharge + levitate(duration=24.0) + explode(radius=4.5) |
| 17 | 22 | 22 | 0.5 | projectile(speed=2.8) + gravity(strength=3.5) |
| 7 | 0 | 5 | 0.5 | ray(range=5.0) + poison(duration=150.0 level=1.0) |
| 101 | 68 | 70 | 0.6 | projectile(speed=1.4) + curse(duration=1830.0) + repeat(times=2.0) + explode(radius=2.4) |
| 42 | 0 | 8 | 0.6 | pillar(radius=3.5) + gravity(strength=1.0) + levitate(duration=31.0) + poison(duration=128.0) + lengthen |
| 24 | 20 | 23 | 0.5 | ray(range=19.9) + electrify(duration=250.0) + gravity(strength=3.9) |
| 12 | 17 | 20 | 0.8 | aura(radius=4.2) + cut(amount=9.1) |
| 6 | 9 | 9 | 0.6 | pillar(radius=1.8 range=22.5) + damage(amount=4.0) |
| 11 | 0 | 0 | 0.5 | projectile(speed=1.6) + electrify(duration=455.0) |
| 113 | 17 | 26 | 1.2 | wave(range=7.9) + lifesteal(amount=3.1) + power + slow(duration=534.0 level=0.0) + trail(damage=2.2) + more_range |
| 164 | 8 | 11 | 0.7 | touch(range=2.3) + freeze(duration=86.0 radius=0.8) + chain + damage(amount=19.3) + repeat(times=3.0) + levitate(duration=43.0) + random |
| 116 | 45 | 94 | 2.3 | wave(range=9.3) + damage(amount=12.1) + more_range + push(strength=1.4) + lengthen + wither(duration=231.0) |
| 12 | 2 | 14 | 2.7 | aura(radius=4.9) + fire(duration=159.0) + more_range |
| 18 | 177 | 181 | 2.8 | touch(range=4.3) + explode(radius=4.6) |
| 10 | 6 | 10 | 0.6 | ray(range=47.9) + gravity(strength=0.9) + curse(duration=407.0) |
| 39 | 28 | 28 | 0.8 | touch(range=5.9) + lifesteal(amount=3.9 ratio=0.9) + rumble(damage=7.5) + freeze(duration=234.0 radius=1.5) |
| 71 | 25 | 28 | 0.6 | projectile(speed=1.3) + rumble(damage=6.7 radius=4.4) + repeat(times=3.0) + slow(duration=366.0 level=1.0) |
| 28 | 22 | 45 | 0.5 | pillar(radius=2.4) + satellites(damage=5.9 duration=458.0) + overcharge |
| 13 | 5 | 43 | 1.7 | aura(radius=10.1) + cut(amount=1.3) |
| 19 | 22 | 37 | 0.5 | projectile(speed=0.7) + freeze(duration=60.0 radius=1.0) + satellites(count=3.0 damage=3.0 duration=44.0) + overcharge |
| 243 | 319 | 319 | 17.8 | pillar(radius=4.9) + explode(radius=4.5) + overcharge + trail(damage=1.3 duration=552.0) + explode(radius=5.8) |
| 13 | 44 | 66 | 0.5 | ray(range=11.8) + slow(duration=263.0 level=2.0) + chain |
| 8 | 18 | 23 | 0.5 | projectile(speed=1.8) + freeze(duration=121.0 radius=1.3) + lengthen |
| 14 | 16 | 25 | 0.4 | touch(range=3.8) + gravity(strength=1.7) + poison(duration=254.0 level=1.0) |
| 13 | 13 | 26 | 0.8 | aura(radius=6.6) + wither(duration=105.0) + random |
| 20 | 18 | 35 | 1.1 | aura(radius=9.5) + wither(duration=117.0 level=0.0) |
| 46 | 0 | 50 | 0.7 | wave(range=6.6) + electrify(duration=591.0 mode=1.0) + push(strength=2.7) + push(strength=2.2) + lengthen |
| 9 | 7 | 18 | 0.4 | touch(range=5.8) + wither(duration=148.0 level=1.0) |
| 50 | 29 | 31 | 0.5 | ray(range=11.5) + levitate(duration=193.0) + gravity(strength=4.0) + chain |
| 96 | 90 | 105 | 1.8 | aura(radius=7.9) + trail(damage=1.6 duration=479.0) + damage(amount=16.0) + power + levitate(duration=57.0) + more_range |
| 8 | 23 | 53 | 0.5 | touch(range=3.0) + cut(amount=6.6) |
| 52 | 17 | 33 | 0.6 | ray(range=4.6) + cut(amount=15.9) + damage(amount=18.1) + fire(duration=146.0) |
| 26 | 150 | 150 | 7.9 | wave(range=9.6) + explode(radius=3.5) + levitate(duration=102.0) |
| 51 | 15 | 15 | 0.5 | ray(range=5.1) + curse(duration=782.0) + slow(duration=363.0) + trail(damage=5.9 duration=527.0) |
| 97 | 22 | 22 | 0.4 | projectile(speed=0.7) + curse(duration=257.0) + more_range + rumble(damage=10.9) + pierce + gravity(strength=0.8) |
| 19 | 15 | 22 | 1.1 | aura(radius=3.4) + freeze(duration=359.0 radius=2.9) |
| 6 | 9 | 11 | 0.4 | ray(range=33.3) + poison(duration=141.0) |
| 7 | 9 | 12 | 0.5 | aura(radius=4.3) + poison(duration=118.0 level=0.0) |
| 48 | 23 | 27 | 7.2 | aura(radius=9.3) + damage(amount=6.1) + area |
| 29 | 381 | 381 | 13.3 | aura(radius=2.9) + explode(radius=4.9) + area |
| 18 | 35 | 61 | 0.6 | touch(range=4.8) + cut(amount=15.0) |
| 53 | 23 | 25 | 0.4 | projectile(speed=1.5) + trail(damage=3.6 duration=419.0) + damage(amount=15.1) + satellites(count=2.0 damage=7.1) |
| 4 | 11 | 12 | 0.4 | projectile(speed=1.5) + poison(duration=37.0) |
| 83 | 50 | 65 | 1.4 | aura(radius=7.6) + cut(amount=2.6) + gravity(strength=3.2) + lifesteal(amount=13.2 ratio=0.4) |
| 129 | 0 | 0 | 0.6 | pillar(radius=4.0) + push(strength=2.1) + pierce + levitate(duration=89.0) + slow(duration=374.0) |
| 97 | 32 | 51 | 1.2 | pillar(radius=6.0) + acid(amount=8.4) + fire(duration=99.0) + lengthen |
| 27 | 33 | 42 | 0.5 | ray(range=31.4) + gravity(strength=1.9) + lifesteal(amount=12.1 ratio=0.2) |
| 47 | 12 | 16 | 0.4 | touch(range=1.7) + poison(duration=36.0 level=1.0) + levitate(duration=109.0) + gravity(strength=3.8) + area |
| 208 | 41 | 44 | 0.7 | pillar(radius=5.3 range=28.2) + gravity(strength=3.9) + wither(duration=260.0) + push(strength=1.2) + more_range |
| 50 | 113 | 113 | 3.9 | pillar(radius=1.0) + explode(radius=3.2) + pierce + cut(amount=15.2) |
| 78 | 47 | 57 | 0.6 | pillar(radius=5.9 range=36.4) + gravity(strength=3.7) + lengthen |
| 7 | 7 | 11 | 0.5 | ray(range=11.1) + fire(duration=105.0) |
| 6 | 7 | 10 | 0.4 | projectile(speed=1.1) + cut(amount=3.4) |
| 50 | 16 | 30 | 0.6 | touch(range=5.9) + rumble(damage=5.2 radius=3.9) + repeat(times=3.0) + levitate(duration=104.0) |
| 43 | 1 | 13 | 0.6 | projectile(speed=2.8) + push(strength=0.9) + area + trail(damage=5.7 duration=495.0) + freeze(duration=89.0 radius=4.2) |
| 53 | 11 | 11 | 0.7 | pillar(radius=1.7 range=5.2) + levitate(duration=154.0) + slow(duration=310.0 level=3.0) + repeat(times=3.0) |
| 75 | 14 | 14 | 1.2 | wave(range=7.7) + curse(duration=2149.0) + more_range |
| 31 | 16 | 31 | 0.7 | wave(range=6.8) + fire(duration=99.0) + chain + levitate(duration=119.0) + weaken |
| 12 | 10 | 17 | 0.5 | projectile(speed=0.9) + levitate(duration=114.0) + cut(amount=2.1) |
| 40 | 14 | 14 | 0.5 | ray(range=18.0) + push(strength=0.6) + levitate(duration=165.0) + curse(duration=1075.0) |
