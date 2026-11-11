package com.arcane.network.packet;

import com.arcane.menu.ArcaneWorkbenchMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

/** Grimorio -> servidor: "Escrever". Abre a interface de desenho no campo; a magia entra no grimorio com 30% da potencia. */
public record C2SOpenWritePacket() {
  public static void encode(C2SOpenWritePacket p, FriendlyByteBuf buf) {}

  public static C2SOpenWritePacket decode(FriendlyByteBuf buf) { return new C2SOpenWritePacket(); }

  public static void handle(C2SOpenWritePacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      var pos = player.blockPosition();
      NetworkHooks.openScreen(player,
          new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new ArcaneWorkbenchMenu(id, inv, pos, ArcaneWorkbenchMenu.FIELD),
              Component.translatable("gui.arcane.write_title")),
          buf -> buf.writeBlockPos(pos).writeVarInt(ArcaneWorkbenchMenu.FIELD).writeVarInt(1));
    });
    ctx.get().setPacketHandled(true);
  }
}
