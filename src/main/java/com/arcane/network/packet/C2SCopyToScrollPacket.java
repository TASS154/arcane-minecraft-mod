package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscribing.ItemInscriptionHelper;
import com.arcane.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Grimorio -> servidor: escreve uma magia salva num pergaminho (1 papel + 1 tinta comum; gratis no criativo).
 * E assim que os magos compartilham magias: o pergaminho pode ser lancado ou estudado por outra pessoa.
 */
public record C2SCopyToScrollPacket(UUID inscriptionId) {
  public static void encode(C2SCopyToScrollPacket packet, FriendlyByteBuf buf) {
    buf.writeUUID(packet.inscriptionId);
  }

  public static C2SCopyToScrollPacket decode(FriendlyByteBuf buf) {
    return new C2SCopyToScrollPacket(buf.readUUID());
  }

  public static void handle(C2SCopyToScrollPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      GrimoireAttachments.get(player).flatMap(g -> g.getInscription(packet.inscriptionId)).ifPresent(spell -> {
        Inventory inv = player.getInventory();
        int paper = findPlainPaper(inv);
        int ink = findInk(inv);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && (paper < 0 || ink < 0)) {
          player.displayClientMessage(Component.translatable("message.arcane.scroll.need_materials").withStyle(ChatFormatting.RED), true);
          return;
        }
        if (!creative) {
          inv.getItem(paper).shrink(1);
          inv.getItem(ink).shrink(1);
        }
        ItemStack scroll = new ItemStack(Items.PAPER);
        ItemInscriptionHelper.write(scroll, spell);
        scroll.setHoverName(spell.metadata().name().isEmpty() ? Component.translatable("gui.arcane.unnamed") : Component.literal(spell.metadata().name()));
        if (!inv.add(scroll)) player.drop(scroll, false);
        player.displayClientMessage(Component.translatable("message.arcane.scroll.copied").withStyle(ChatFormatting.LIGHT_PURPLE), true);
      });
    });
    ctx.get().setPacketHandled(true);
  }

  private static int findPlainPaper(Inventory inv) {
    for (int i = 0; i < inv.getContainerSize(); i++) {
      ItemStack s = inv.getItem(i);
      if (s.is(Items.PAPER) && !ItemInscriptionHelper.isScroll(s)) return i;
    }
    return -1;
  }

  private static int findInk(Inventory inv) {
    for (int i = 0; i < inv.getContainerSize(); i++) {
      if (inv.getItem(i).is(ModItems.COMMON_INK.get())) return i;
    }
    return -1;
  }
}
