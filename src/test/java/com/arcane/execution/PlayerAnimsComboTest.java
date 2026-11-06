package com.arcane.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerAnimsComboTest {
  @Test
  void comboCyclesAndResetsAfterTheWindow() {
    UUID id = UUID.randomUUID();
    assertEquals(0, PlayerAnims.nextComboStep(id, 100));
    assertEquals(1, PlayerAnims.nextComboStep(id, 120));
    assertEquals(2, PlayerAnims.nextComboStep(id, 150));
    assertEquals(0, PlayerAnims.nextComboStep(id, 170), "depois do golpe final recomeca");
    assertEquals(0, PlayerAnims.nextComboStep(id, 170 + PlayerAnims.COMBO_WINDOW_TICKS + 1), "passou da janela: reinicia");
    PlayerAnims.forget(id);
  }
}
