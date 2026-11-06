package com.arcane.block;

import com.arcane.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Bloco da Barreira: solido, translucido e temporario. A propriedade LIFE conta quantos ciclos de 2 s faltam
 * (1..15 = ate 30 s); ao chegar a zero o bloco some com faiscas.
 */
public class ArcaneBarrierBlock extends Block {
  public static final int MAX_LIFE = 15;
  public static final int CYCLE_TICKS = 40;
  public static final IntegerProperty LIFE = IntegerProperty.create("life", 0, MAX_LIFE);

  public ArcaneBarrierBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(LIFE, MAX_LIFE));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(LIFE);
  }

  @Override
  public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
    if (!level.isClientSide) {
      level.scheduleTick(pos, this, CYCLE_TICKS);
    }
  }

  @Override
  public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    int life = state.getValue(LIFE);
    if (life <= 1) {
      level.sendParticles(ModParticles.ARCANE_SPARK.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
      level.removeBlock(pos, false);
    } else {
      level.setBlock(pos, state.setValue(LIFE, life - 1), 2);
      level.scheduleTick(pos, this, CYCLE_TICKS);
    }
  }

  /** Como o vidro: nao desenha a face entre duas barreiras vizinhas. */
  @Override
  public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
    return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
  }
}
