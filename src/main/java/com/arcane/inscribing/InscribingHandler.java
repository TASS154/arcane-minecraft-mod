package com.arcane.inscribing;

import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;
import java.util.UUID;

public class InscribingHandler {
  @SubscribeEvent
  public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    if (event.getLevel().isClientSide) return;
    Player player = event.getEntity();
    BlockPos pos = event.getPos();
    WorldInscriptionIndex index = WorldInscriptionIndex.get((ServerLevel) event.getLevel());
    Optional<InscriptionAnchor> anchor = index.get(pos);
    if (anchor.isEmpty()) return;

    Inscription inscription = anchor.get().inscription();
    String trigger = anchor.get().triggerType();
    if ("on_open".equals(trigger)) {
      BlockEntity be = event.getLevel().getBlockEntity(pos);
      if (be instanceof ChestBlockEntity) {
        CastingService.tryCast(player, inscription, PrecisionSnapshot.WORKBENCH);
      }
    }
  }
}
