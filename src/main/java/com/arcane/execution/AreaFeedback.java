package com.arcane.execution;

import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CShapeFxPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Mostra o tamanho REAL de uma explosao: a explosao do jogo desenha sempre a mesma nuvem, mesmo com raio 32, entao um
 * anel de luz no raio verdadeiro e, quando a area foi ampliada (cristal, carga, Area), a barra de acao diz o fator.
 */
public final class AreaFeedback {
  private AreaFeedback() {}

  public static void explosion(ExecutionContext context, Vec3 center, double radius) {
    if (radius < 3.5) return;
    SpellElement element = SpellElement.of(context.plan());
    ModNetwork.sendShape(context.level(), S2CShapeFxPacket.AURA, new Vec3(center.x, center.y - 0.3, center.z), Vec3.ZERO,
        (float) Math.min(48.0, radius), element.color(), element.ordinal(), 18);
    double factor = context.areaFactor();
    if (factor >= 1.5 && context.caster() instanceof Player player) {
      player.displayClientMessage(Component.translatable("hud.arcane.area_effect", String.format(java.util.Locale.ROOT, "%.1f", factor),
          String.format(java.util.Locale.ROOT, "%.1f", radius)), true);
    }
  }
}
