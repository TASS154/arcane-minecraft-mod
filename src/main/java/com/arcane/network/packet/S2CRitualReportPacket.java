package com.arcane.network.packet;

import com.arcane.ritual.RitualService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> tela do altar: o relatorio do que o ritual exige agora (circulo, mana, oferendas). So informativo. */
public record S2CRitualReportPacket(RitualService.Report report) {
  public static void encode(S2CRitualReportPacket p, FriendlyByteBuf buf) {
    var r = p.report;
    buf.writeBoolean(r.valid());
    buf.writeComponent(r.message());
    buf.writeVarInt(r.cost());
    buf.writeVarInt(r.mana());
    buf.writeVarInt(r.circleLevel());
    buf.writeVarInt(r.completenessPct());
    buf.writeVarInt(r.needLevel());
    buf.writeVarInt(r.needs().size());
    for (var n : r.needs()) {
      buf.writeResourceLocation(n.item());
      buf.writeVarInt(n.need());
      buf.writeVarInt(n.have());
    }
    buf.writeBoolean(r.legendary());
    buf.writeVarInt(r.stars());
  }

  public static S2CRitualReportPacket decode(FriendlyByteBuf buf) {
    boolean valid = buf.readBoolean();
    Component message = buf.readComponent();
    int cost = buf.readVarInt(), mana = buf.readVarInt(), circle = buf.readVarInt(), pct = buf.readVarInt(), level = buf.readVarInt();
    int n = Math.min(buf.readVarInt(), 64);
    List<RitualService.Need> needs = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      ResourceLocation item = buf.readResourceLocation();
      needs.add(new RitualService.Need(item, buf.readVarInt(), buf.readVarInt()));
    }
    boolean legendary = buf.readBoolean();
    int stars = buf.readVarInt();
    return new S2CRitualReportPacket(new RitualService.Report(valid, message, cost, mana, circle, pct, level, needs, legendary, stars));
  }

  public static void handle(S2CRitualReportPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CRitualReportPacket p) {
    com.arcane.client.screen.RitualAltarScreen.onReport(p.report);
  }
}
