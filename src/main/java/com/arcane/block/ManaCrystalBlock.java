package com.arcane.block;

import com.arcane.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ManaCrystalBlock extends Block {
  private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

  public ManaCrystalBlock(Properties properties) {
    super(properties);
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  /** Particulas ambiente: a zona de mana fica visivel sem precisar de HUD. */
  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    if (random.nextInt(3) != 0) return;
    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.6;
    double y = pos.getY() + 0.3 + random.nextDouble() * 1.2;
    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.6;
    level.addParticle(ModParticles.ARCANE_SPARK.get(), x, y, z, 0.0, 0.015 + random.nextDouble() * 0.02, 0.0);
  }

  public static boolean isCrystal(BlockState state) {
    return state.getBlock() instanceof ManaCrystalBlock;
  }
}
