package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Seek;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.world.entity.LivingEntity;

/**
 * Raio ("eu corto agora"): hit-scan na mira, so criatura ou bloco. Mira no ar vazio NAO entrega ponto (o Raio nao detona
 * no ar: isso e do Projetil e da Coluna); errar tudo e falha (para quem conjura direto, antes de cobrar mana: ver
 * {@code CastingService.formFailure}). Tambem serve ao Toque (alcance curto, sem rastro).
 *
 * <p>Com Mais Proximo logo depois da forma ({@link Seek}): o feixe vira sozinho para a criatura viva mais proxima no
 * alcance, sem atravessar parede.
 */
public class RayFormHandler implements FormHandler {
  private final boolean trail;

  public RayFormHandler(boolean trail) {
    this.trail = trail;
  }

  /** O alcance maximo do glifo Raio (48) vale "sem limite": usa rayRange da config. Abaixo disso, o escolhido. O Toque e curto. */
  public static double rangeFor(DeliverySpec spec, boolean trail) {
    double chosen = Math.max(1.0, Math.min(64.0, spec.range()));
    return trail && spec.range() >= 48.0 ? com.arcane.config.ArcaneConfig.RAY_RANGE.get() : chosen;
  }

  /** O que o Raio ou o Toque pegaria agora (sem desenhar nada). Vazio = errou. */
  public static TargetList probe(ExecutionContext context, DeliverySpec spec, boolean trail) {
    double range = rangeFor(spec, trail);
    if (Seek.on(spec)) {
      LivingEntity target = Seek.nearest(context, range, true);
      if (target == null) return TargetList.empty();
      return hits(Seek.aimedAt(context, target, () -> Targeting.ray(context, range, false).targets()));
    }
    return hits(Targeting.ray(context, range, false).targets());
  }

  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    double range = rangeFor(spec, trail);
    if (Seek.on(spec)) {
      LivingEntity target = Seek.nearest(context, range, true);
      if (target == null) return DeliveryResult.of(TargetList.empty());
      return DeliveryResult.of(hits(Seek.aimedAt(context, target, () -> Targeting.ray(context, range, trail).targets())));
    }
    TargetList got = hits(Targeting.ray(context, range, trail).targets());
    return DeliveryResult.of(trail ? got : withTouchedBlock(context, range, got));
  }

  /**
   * Privilegio do Toque: ao encostar numa criatura, a mao tambem chega ao bloco logo atras dela na mesma linha (dentro do
   * alcance curto). Toque + Fogo queima o lobo e acende a parede; Toque + Cavar fere e cava. O Raio para no primeiro.
   */
  static TargetList withTouchedBlock(ExecutionContext context, double range, TargetList got) {
    if (got.entities().isEmpty() || !got.blocks().isEmpty()) return got;
    var from = context.origin();
    var to = from.add(context.look().scale(range));
    var hit = context.level().clip(new net.minecraft.world.level.ClipContext(from, to,
        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, context.caster()));
    if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return got;
    var pos = hit.getBlockPos();
    return new TargetList(got.entities(), java.util.List.of(new TargetList.BlockTarget(pos, context.level().getBlockState(pos))));
  }

  /** So criatura ou bloco: o ponto vazio do fim da mira nao e alvo do Raio nem do Toque. */
  private static TargetList hits(TargetList t) {
    return new TargetList(t.entities(), t.blocks());
  }
}
