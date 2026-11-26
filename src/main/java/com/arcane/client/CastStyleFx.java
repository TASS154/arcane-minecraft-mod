package com.arcane.client;

import com.arcane.execution.SpellElement;
import com.arcane.fx.CastStyle;
import com.arcane.fx.Fx;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Parte do cliente da maestria e da assinatura (Fase M), so aparencia: o motivo da assinatura (anel, espiral ou
 * pontos, no maximo 20% das particulas da conjuracao) e a camada de acabamento da maestria alta. Desligavel por
 * jogador: a propria assinatura e a dos outros, separadas. NAO visto em jogo.
 */
public final class CastStyleFx {
  private CastStyleFx() {}

  /** Particulas da conjuracao no servidor (Fx.cast pede 10 a 24); a assinatura fica abaixo de 20% disso. */
  static final int CAST_PARTICLES = 14;

  public static void onCast(Vec3 at, int element, float mastery, int signature, int casterId) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    boolean own = mc.player != null && mc.player.getId() == casterId;
    var e = SpellElement.byOrdinal(element);
    boolean show = own ? ClientPrefs.signatureOwn() : ClientPrefs.signatureOthers();
    if (CastStyle.present(signature) && show) motif(mc, at, e, CastStyle.motif(signature), CastStyle.accent(signature, ClientPrefs.colorblind()));
    if (CastStyle.polished(mastery)) polish(mc, at, e);
  }

  private static void motif(Minecraft mc, Vec3 at, SpellElement e, int motif, int accent) {
    int n = Math.max(1, CastStyle.signatureCount(CAST_PARTICLES));
    var opts = Fx.options(e, Fx.Role.TRAIL, 0.7f, accent);
    for (int i = 0; i < n; i++) {
      double f = i / (double) n;
      double a = f * Math.PI * 2;
      double x, y, z;
      switch (motif) {
        case CastStyle.MOTIF_RING -> { x = Math.cos(a) * 1.0; y = 0.1; z = Math.sin(a) * 1.0; }
        case CastStyle.MOTIF_SPIRAL -> { x = Math.cos(a * 2) * (0.4 + f * 0.5); y = 0.3 + f * 1.6; z = Math.sin(a * 2) * (0.4 + f * 0.5); }
        default -> { x = Math.cos(a) * 0.55; y = 2.1 + (i % 2) * 0.15; z = Math.sin(a) * 0.55; } // pontos sobre a cabeca
      }
      mc.level.addParticle(opts, at.x + x, at.y + y, at.z + z, 0, 0.01, 0);
    }
  }

  /** Acabamento da maestria alta: um anel fino e nitido no chao, na cor do proprio elemento. */
  private static void polish(Minecraft mc, Vec3 at, SpellElement e) {
    var opts = Fx.options(e, Fx.Role.IMPACT, 0.4f, -1);
    for (int i = 0; i < 8; i++) {
      double a = i / 8.0 * Math.PI * 2;
      mc.level.addParticle(opts, at.x + Math.cos(a) * 0.7, at.y + 0.05, at.z + Math.sin(a) * 0.7, 0, 0.005, 0);
    }
  }
}
