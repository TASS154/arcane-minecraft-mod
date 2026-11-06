package com.arcane.block;

import com.arcane.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Traco de giz arcano no chao: uma celula do circulo ritual. Fino, atravessavel, so existe sobre chao firme.
 * Acende (LIT) durante um ritual e apaga sozinho.
 */
public class ArcaneTraceBlock extends Block {
  public static final BooleanProperty LIT = BlockStateProperties.LIT;
  private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
  public static final int GLOW_TICKS = 120;

  public ArcaneTraceBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(LIT, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(LIT);
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  @Override
  public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    BlockPos below = pos.below();
    return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
  }

  @Override
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                BlockPos pos, BlockPos neighborPos) {
    return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
        : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    return new ItemStack(ModItems.ARCANE_CHALK.get());
  }

  /** Acende o traco por GLOW_TICKS. */
  public static void glow(ServerLevel level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    if (state.getBlock() instanceof ArcaneTraceBlock) {
      level.setBlock(pos, state.setValue(LIT, true), 3);
      level.scheduleTick(pos, state.getBlock(), GLOW_TICKS);
    }
  }

  @Override
  public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, false), 3);
  }
}
