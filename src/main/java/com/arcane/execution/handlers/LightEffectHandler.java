package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.LightService;
import com.arcane.execution.TargetList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Luz: acende uma luz no ponto mirado (sobre o bloco, ou acima da cabeca da criatura). Duracao em ticks; 0 = para
 * sempre. No maximo 4 luzes por conjuracao.
 */
public class LightEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    var level = context.level();
    int brightness = (int) Math.round(call.parameters().getOrDefault("brightness", 15.0));
    double dur = call.parameters().getOrDefault("duration", 2400.0);
    int ticks = dur <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE / 2.0, context.amplify(dur, call.magnitudeKeys(), "duration"));
    List<BlockPos> spots = new ArrayList<>();
    for (var b : targets.blocks()) spots.add(b.pos().above());
    for (var p : targets.points()) spots.add(BlockPos.containing(p));
    for (var e : targets.entities()) spots.add(BlockPos.containing(e.getEyePosition().add(0, 0.8, 0)));
    int lit = 0;
    for (BlockPos pos : spots) {
      if (lit >= 4) break;
      BlockPos at = pos;
      for (int up = 0; up < 3 && !level.getBlockState(at).isAir() && !level.getBlockState(at).is(Blocks.LIGHT); up++) at = at.above();
      if (LightService.place(level, at, brightness, ticks)) {
        lit++;
        Vec3 c = Vec3.atCenterOf(at);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 12, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 6, 0.25, 0.25, 0.25, 0.0);
      }
    }
  }
}
