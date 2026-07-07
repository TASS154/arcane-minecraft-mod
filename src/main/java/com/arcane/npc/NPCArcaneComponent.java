package com.arcane.npc;

import com.arcane.grimoire.GrimoireState;
import com.arcane.inscription.Inscription;

public final class NPCArcaneComponent {
  private final GrimoireState grimoire;

  public NPCArcaneComponent() {
    this.grimoire = GrimoireState.createDefault();
  }

  public GrimoireState grimoire() { return grimoire; }

  public Inscription selectSpell() {
    return grimoire.activeInscriptionId().flatMap(grimoire::getInscription).orElse(Inscription.testProjectileDamageFire());
  }
}
