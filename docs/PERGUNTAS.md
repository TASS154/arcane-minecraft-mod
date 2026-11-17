
## Dúvidas registradas na Fase 0
- **Zonas restauradas:** voltam só quando o conjurador (jogador online ou criatura carregada) existe; se o prazo passou com o mundo fechado, somem sem disparar "Quando a zona acabar". Preferi isso a disparar efeitos fora de contexto. Confirmar.
- **Ao Entrar na Zona:** quem já estava dentro na hora de conjurar não conta como "entrou". Confirmar.
- **Convidados:** a lista é por jogador (não por inscrição) e vale também para animais domesticados. "Dono/Lista" de uma inscrição em bloco usa o conjurador de quem dispara, não de quem escreveu. Confirmar se deve guardar o autor na inscrição.
- **Cristal:** explosões têm teto absoluto (40 e 36 blocos) mesmo com x32, por segurança do servidor.

## Dúvidas registradas na Fase 1
- **Modo de conjuração "na bancada":** escolhi o Grimório (aba Magias) para não mudar o pacote de salvar da Bancada. Se for essencial na Bancada, é uma adição pequena. Confirmar.
- **Carregar:** custo = 1,5x o custo da magia, gasto aos poucos durante 3 s; a magia sai gratuita e até 3x mais forte. Se soltar antes, gasta só o que carregou e sai proporcionalmente mais fraca. Valores provisórios (`CastChannels`).
- **Segurar:** repete a cada 0,5 s pagando o custo cheio; para sem mana. Sem recarga especial.
- **Teclas 1-9** no modo Conjurador consomem as teclas da hotbar (a roda do mouse ainda troca a hotbar).
- **"Usadas recentemente"** usa a última conjuração (por qualquer modo), guardada em tempo de jogo.

## Dúvidas registradas na Fase 2
- **Glifo novo ou Flutuar?** Reaproveitei Flutuar (já era voo, "puxado pela cabeça"). Se quiser um glifo separado "Voo", é só renomear/duplicar com a tabela; o comportamento fica igual.
- **Custo:** 0,25 mana por tick por bloco/tick de velocidade (ex.: velocidade 1 = 5 mana/s; velocidade 3 = 15 mana/s). Provisório, em `flightManaPerTick`.
- **Queda segura:** uma queda, até 60 s depois do voo. Se quiser dano normal ao cair de grandes alturas quando faltar mana (punição), é uma linha.
