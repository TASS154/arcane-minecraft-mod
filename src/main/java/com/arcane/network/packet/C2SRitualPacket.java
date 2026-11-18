package com.arcane.network.packet;

import com.arcane.block.ArcaneAltarBlock;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.network.ModNetwork;
import com.arcane.ritual.ComponentSource;
import com.arcane.ritual.RitualMessages;
import com.arcane.ritual.RitualService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Tela do altar -> servidor: a magia desenhada ali (so para este ritual, nao e a ativa do grimorio) e o que fazer com ela:
 * PREVIEW (relatorio), PERFORM (realizar) ou STORE (guardar no altar para o glifo Ativar). O servidor revalida tudo
 * (distancia ao altar, glifos, parametros, circulo, oferendas e mana).
 */
public record C2SRitualPacket(BlockPos altar, int action, boolean legendary, List<C2SSaveInscriptionPacket.Entry> glyphs) {
  public static final int PREVIEW = 0, PERFORM = 1, STORE = 2;
  private static final int MAX_GLYPHS = 24, MAX_PARAMS = 8;

  public static void encode(C2SRitualPacket p, FriendlyByteBuf buf) {
    buf.writeBlockPos(p.altar);
    buf.writeVarInt(p.action);
    buf.writeBoolean(p.legendary);
    buf.writeVarInt(p.glyphs.size());
    for (var e : p.glyphs) {
      buf.writeUtf(e.id(), 128);
      buf.writeVarInt(e.params().size());
      e.params().forEach((k, v) -> { buf.writeUtf(k, 32); buf.writeDouble(v); });
    }
  }

  public static C2SRitualPacket decode(FriendlyByteBuf buf) {
    BlockPos altar = buf.readBlockPos();
    int action = buf.readVarInt();
    boolean legendary = buf.readBoolean();
    int count = Math.min(buf.readVarInt(), MAX_GLYPHS + 1);
    List<C2SSaveInscriptionPacket.Entry> glyphs = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String id = buf.readUtf(128);
      int n = Math.min(buf.readVarInt(), MAX_PARAMS);
      Map<String, Double> params = new HashMap<>();
      for (int k = 0; k < n; k++) params.put(buf.readUtf(32), buf.readDouble());
      glyphs.add(new C2SSaveInscriptionPacket.Entry(id, params));
    }
    return new C2SRitualPacket(altar, action, legendary, glyphs);
  }

  public static void handle(C2SRitualPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player != null) apply(player, packet);
    });
    ctx.get().setPacketHandled(true);
  }

  /** Separado do handler para os testes chamarem direto. */
  public static void apply(ServerPlayer player, C2SRitualPacket packet) {
    if (!(player.level() instanceof ServerLevel level) || packet.glyphs.size() > MAX_GLYPHS) return;
    BlockPos pos = packet.altar;
    if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0
        || !(level.getBlockState(pos).getBlock() instanceof ArcaneAltarBlock)) return;
    List<GlyphInstance> instances = new ArrayList<>();
    for (var entry : packet.glyphs) {
      ResourceLocation id = ResourceLocation.tryParse(entry.id());
      if (id == null || com.arcane.glyph.GlyphRegistry.get(id).isEmpty()) return;
      instances.add(new GlyphInstance(id, C2SSaveInscriptionPacket.sanitize(id, entry.params())));
    }
    Inscription draft = new Inscription(List.copyOf(instances));
    if (packet.action == PREVIEW) {
      ModNetwork.sendToPlayer(player, new S2CRitualReportPacket(RitualService.preview(level, pos, player, draft, packet.legendary)));
      return;
    }
    if (instances.isEmpty()) {
      RitualMessages.send(player, new RitualService.Result(RitualService.Outcome.NO_SPELL, null, 0, "", 0, null));
      return;
    }
    ComponentSource source = ComponentSource.of(player);
    RitualService.Result result = packet.action == STORE
        ? RitualService.store(level, pos, player, draft, source, packet.legendary)
        : RitualService.perform(level, pos, player, draft, source, packet.legendary);
    RitualMessages.send(player, result);
    ModNetwork.sendToPlayer(player, new S2CRitualReportPacket(RitualService.preview(level, pos, player, draft, packet.legendary)));
  }
}
