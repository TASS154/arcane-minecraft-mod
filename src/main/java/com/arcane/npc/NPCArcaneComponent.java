package com.arcane.npc;

import com.arcane.inscription.Inscription;

/** Conhecimento arcano de um mago NPC: a magia do seu elemento. */
public final class NPCArcaneComponent {
  private MageElement element = MageElement.FIRE;

  public MageElement element() { return element; }

  public void setElement(MageElement element) {
    this.element = element;
  }

  public Inscription selectSpell() {
    return element.spell();
  }
}
