package com.arcane.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selo do ritual que prende a Hidra. Fica no alto dos pilares da camara; enquanto sobrar um, a Hidra dorme e nao
 * pode ser ferida. Quebrar o ultimo selo (picareta, magia, explosao de jogador) a desperta. Nao solta nada.
 */
public class HydraSealBlock extends Block {
  public HydraSealBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
    super.onRemove(state, level, pos, newState, moving);
    if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) com.arcane.world.HydraLairs.onSealBroken(sl, pos);
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    // fumaca de almas subindo e faiscas caindo para dentro da camara
    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.9,
        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.03, 0);
    if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
  }
}
