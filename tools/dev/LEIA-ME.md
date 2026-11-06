# Teste de telas no jogo real

`DevShots.java.txt` abre o Minecraft num mundo, aprende todos os glifos, abre o Grimório e a Bancada,
salva e conjura uma magia, spawna os 18 magos e tira capturas em `run/screenshots`.
Foi assim que se descobriu que a Bancada nunca abria (menu sem `NetworkHooks.openScreen`).

Como usar:

1. Copie `DevShots.java.txt` para `src/main/java/com/arcane/client/DevShots.java`.
2. Crie um mundo: `./gradlew runGameTestServer`, depois copie `run/world` para `run/saves/ArcaneTest`.
3. `JAVA_TOOL_OPTIONS="-Darcane.devshots=true" ./gradlew runClient -PquickPlay=ArcaneTest`
4. Veja as imagens em `run/screenshots`. Apague o `DevShots.java` antes de commitar.

Usa reflexao em campos privados das telas: se renomear `tab`, `selectedGlyph`, `nameField` ou `save`, ajuste aqui.
