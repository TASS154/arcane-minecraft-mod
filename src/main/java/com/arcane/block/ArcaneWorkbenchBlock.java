package com.arcane.block;

import com.arcane.menu.ArcaneWorkbenchMenu;
import com.arcane.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

public class ArcaneWorkbenchBlock extends Block {
  public ArcaneWorkbenchBlock(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
      // O menu le a posicao da bancada do pacote de abertura: precisa de NetworkHooks, nao de openMenu simples.
      net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, state.getMenuProvider(level, pos), pos);
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Nullable
  @Override
  public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
    return new SimpleMenuProvider((id, inv, player) -> new ArcaneWorkbenchMenu(id, inv, pos),
        Component.translatable("block.arcane.arcane_workbench"));
  }
}
