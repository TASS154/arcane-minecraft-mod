package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Corte: dano FISICO (a armadura reduz, diferente do Dano, que e magico e ignora armadura).
 * Tosquia ovelhas prontas e, em blocos, corta folhas, teias e plantas na hora.
 */
public class CutEffectHandler implements EffectHandler {
  private static final int MAX_BLOCKS = 6;

  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    float amount = (float) context.amplify(call.parameters().getOrDefault("amount", 3.0), call.magnitudeKeys(), "amount");
    DamageSource source = context.caster() instanceof Player p
        ? context.level().damageSources().playerAttack(p)
        : context.level().damageSources().mobAttack(context.caster());
    for (Entity entity : targets.entities()) {
      if (entity instanceof Shearable shearable && shearable.readyForShearing() && Targeting.mayAlterWorld(context)) {
        shearable.shear(SoundSource.PLAYERS);
        continue;
      }
      entity.hurt(source, amount);
    }
    if (!Targeting.mayBreakBlocks(context)) return;
    int cut = 0;
    for (TargetList.BlockTarget block : targets.blocks()) {
      if (cut >= MAX_BLOCKS) break;
      BlockState state = context.level().getBlockState(block.pos());
      if (state.is(BlockTags.SWORD_EFFICIENT) || state.is(Blocks.COBWEB)) {
        if (context.level().destroyBlock(block.pos(), true, context.caster())) cut++;
      }
    }
  }
}
