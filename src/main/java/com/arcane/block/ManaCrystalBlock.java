package com.arcane.block;

import com.arcane.ambient.AmbientFieldService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ManaCrystalBlock extends Block {
  public ManaCrystalBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
  }

  public static boolean isCrystal(BlockState state) {
    return state.getBlock() instanceof ManaCrystalBlock;
  }
}
