package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Crescer: as plantas em volta do alvo crescem como se recebessem farinha de osso algumas vezes. O raio sobe com o
 * cristal. Teto de plantas por conjuracao para nao pesar.
 */
public class GrowEffectHandler implements EffectHandler {
  private static final int MAX_PLANTS = 256;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    if (!Targeting.mayAlterWorld(context)) return;
    ServerLevel level = context.level();
    double radius = Math.min(16.0, context.amplify(call.parameters().getOrDefault("radius", 3.0), call.magnitudeKeys(), "radius"));
    int doses = (int) Math.max(1, Math.min(8, Math.round(call.parameters().getOrDefault("strength", 2.0))));
    List<Vec3> centres = new ArrayList<>();
    for (var b : targets.blocks()) centres.add(Vec3.atCenterOf(b.pos()));
    for (var p : targets.points()) centres.add(p);
    for (var e : targets.entities()) centres.add(e.position());
    int grown = 0;
    int r = (int) Math.ceil(radius);
    for (Vec3 c : centres) {
      BlockPos base = BlockPos.containing(c);
      for (int dx = -r; dx <= r && grown < MAX_PLANTS; dx++) {
        for (int dz = -r; dz <= r && grown < MAX_PLANTS; dz++) {
          if (dx * dx + dz * dz > radius * radius) continue;
          for (int dy = -2; dy <= 2; dy++) {
            BlockPos pos = base.offset(dx, dy, dz);
            var state = level.getBlockState(pos);
            // So plantas: o bloco de grama tambem e "fertilizavel" (espalha mato) e encheria o chao de capim.
            if (!(state.getBlock() instanceof BonemealableBlock) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.CRIMSON_NYLIUM) || state.is(Blocks.WARPED_NYLIUM)) continue;
            boolean any = false;
            for (int i = 0; i < doses; i++) {
              var now = level.getBlockState(pos);
              if (!(now.getBlock() instanceof BonemealableBlock b) || !b.isValidBonemealTarget(level, pos, now, false)) break;
              if (b.isBonemealSuccess(level, level.random, pos, now)) b.performBonemeal(level, level.random, pos, now);
              any = true;
            }
            if (any) {
              grown++;
              level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
            }
          }
        }
      }
    }
  }
}
