package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.item.ScepterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Acoes da tela do cetro, sobre o cetro da mao principal. O servidor confere tudo: indices fora do lugar sao
 * ignorados e so a magia ativa do proprio grimorio pode ser gravada.
 */
public record C2SScepterPacket(int action, int a, int b) {
  public static final int STYLE = 0, STORE_ACTIVE = 1, SELECT = 2, REMOVE = 3;

  public static void encode(C2SScepterPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.action).writeVarInt(p.a).writeVarInt(p.b);
  }

  public static C2SScepterPacket decode(FriendlyByteBuf buf) {
    return new C2SScepterPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
  }

  public static void handle(C2SScepterPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player != null) apply(player, p);
    });
    ctx.get().setPacketHandled(true);
  }

  /** Aplica a acao (publico para os testes). */
  public static void apply(ServerPlayer player, C2SScepterPacket p) {
    ItemStack stack = player.getMainHandItem();
    if (!(stack.getItem() instanceof ScepterItem)) return;
    switch (p.action) {
      case STYLE -> ScepterItem.setStyle(stack, p.a, p.b);
      case SELECT -> ScepterItem.select(stack, p.a);
      case REMOVE -> ScepterItem.remove(stack, p.a);
      case STORE_ACTIVE -> {
        var active = GrimoireAttachments.get(player).flatMap(g -> g.activeInscriptionId().flatMap(g::getInscription));
        if (active.isEmpty()) {
          player.displayClientMessage(Component.translatable("message.arcane.scepter.no_active").withStyle(ChatFormatting.GRAY), true);
          return;
        }
        int at = ScepterItem.store(stack, active.get());
        String name = active.get().metadata().name();
        player.displayClientMessage(Component.translatable("message.arcane.scepter.stored",
            name == null || name.isEmpty() ? Component.translatable("gui.arcane.unnamed").getString() : name, at + 1, ScepterItem.SLOTS)
            .withStyle(ChatFormatting.AQUA), true);
      }
      default -> { }
    }
  }
}
