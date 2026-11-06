package com.arcane;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protecoes contra erros que so aparecem dentro do jogo e ja aconteceram. */
class CodeGuardsTest {
  /**
   * O menu da Bancada le a posicao do pacote de abertura. Abrir com Player.openMenu (sem dados extras)
   * faz o cliente quebrar (buf nulo) e a Bancada nunca abre. Precisa de NetworkHooks.openScreen.
   */
  @Test
  void workbenchOpensWithExtraData() throws IOException {
    String source = Files.readString(Path.of("src/main/java/com/arcane/block/ArcaneWorkbenchBlock.java"), StandardCharsets.UTF_8);
    assertTrue(source.contains("NetworkHooks.openScreen"), "a Bancada deve abrir com NetworkHooks.openScreen");
    assertFalse(source.contains("player.openMenu("), "openMenu simples nao envia a posicao da bancada");
  }
}
