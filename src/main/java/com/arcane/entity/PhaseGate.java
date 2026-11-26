package com.arcane.entity;

import net.minecraft.world.damagesource.DamageSource;

import java.util.function.Predicate;

/**
 * Portao de fase generico (Fase N), extraido da Hidra sem mudar o comportamento dela: uma casca que absorve todo dano
 * enquanto esta de pe e so cai por um estado ou uma reacao, seguida de uma janela de vulnerabilidade em que o dano
 * e multiplicado.
 *
 * <ul>
 *   <li>Hidra: a casca e o selo (cai quando os selos do covil sao quebrados); so dano que ignora invulnerabilidade
 *       (o /kill) atravessa; a janela e o "momento de fraqueza" (x2) depois de uma magia grande.</li>
 *   <li>Guardiao de Ruina: a casca de gelo cai com a reacao configurada no JSON (Fogo em alvo Molhado = Vapor); a
 *       janela dura alguns segundos e a casca volta.</li>
 * </ul>
 */
public final class PhaseGate<T> {
  private final Predicate<T> shellUp;
  private final Predicate<DamageSource> passes;
  private final Predicate<T> windowOpen;
  private final float windowMultiplier;

  public PhaseGate(Predicate<T> shellUp, Predicate<DamageSource> passes, Predicate<T> windowOpen, float windowMultiplier) {
    this.shellUp = shellUp;
    this.passes = passes;
    this.windowOpen = windowOpen;
    this.windowMultiplier = windowMultiplier;
  }

  /** A casca absorve este golpe? (de pe e o dano nao e dos que atravessam) */
  public boolean blocks(T owner, DamageSource source) {
    return shellUp.test(owner) && !passes.test(source);
  }

  /** Dano depois da janela de vulnerabilidade (aberta: vezes o multiplicador). */
  public float scale(T owner, float amount) {
    return windowOpen.test(owner) ? amount * windowMultiplier : amount;
  }

  public boolean shellUp(T owner) { return shellUp.test(owner); }

  public boolean windowOpen(T owner) { return windowOpen.test(owner); }

  public float windowMultiplier() { return windowMultiplier; }
}
