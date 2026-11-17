package com.arcane.network.packet;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.SpellPrefs;
import com.arcane.network.ArcaneSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Organizacao do grimorio: modo de conjuracao, favorito, pasta, conjuntos e troca de conjunto. Tudo revalidado no servidor. */
public record C2SGrimoireOrgPacket(int op, UUID spell, int value, String text) {
  public static final int MODE = 0, FAVORITE = 1, FOLDER = 2, LOADOUT_TOGGLE = 3, LOADOUT_SWITCH = 4;

  public static void encode(C2SGrimoireOrgPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.op);
    buf.writeUUID(p.spell);
    buf.writeVarInt(p.value);
    buf.writeUtf(p.text, SpellPrefs.MAX_FOLDER * 4);
  }

  public static C2SGrimoireOrgPacket decode(FriendlyByteBuf buf) {
    return new C2SGrimoireOrgPacket(buf.readVarInt(), buf.readUUID(), buf.readVarInt(), buf.readUtf(SpellPrefs.MAX_FOLDER * 4));
  }

  public static void handle(C2SGrimoireOrgPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null) return;
      apply(player, p);
    });
    ctx.get().setPacketHandled(true);
  }

  /** Aplica o pedido (publico para os testes). */
  public static void apply(ServerPlayer player, C2SGrimoireOrgPacket p) {
    GrimoireAttachments.get(player).ifPresent(g -> {
      var prefs = g.prefs();
      if (p.op == LOADOUT_SWITCH) {
        prefs.setActiveLoadout(p.value);
        ArcaneSync.sync(player, true);
        return;
      }
      var spell = g.getInscription(p.spell);
      if (spell.isEmpty()) return;
      switch (p.op) {
        case MODE -> {
          var mode = SpellPrefs.CastMode.of(p.value);
          if (mode == SpellPrefs.CastMode.TOGGLE && !com.arcane.execution.CastChannels.toggleable(spell.get())) {
            player.displayClientMessage(Component.translatable("message.arcane.toggle_not_allowed"), true);
            return;
          }
          prefs.setMode(p.spell, mode);
        }
        case FAVORITE -> prefs.setFavorite(p.spell, p.value != 0);
        case FOLDER -> prefs.setFolder(p.spell, p.text);
        case LOADOUT_TOGGLE -> {
          if (!prefs.toggleInLoadout(p.value, p.spell) && !prefs.loadout(p.value).contains(p.spell)) {
            player.displayClientMessage(Component.translatable("message.arcane.loadout_full", SpellPrefs.SLOTS), true);
          }
        }
        default -> { return; }
      }
      ArcaneSync.sync(player, true);
    });
  }
}
