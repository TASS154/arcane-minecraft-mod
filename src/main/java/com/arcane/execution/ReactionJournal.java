package com.arcane.execution;

import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Diario de descobertas (Fase K): por jogador e salvo no mundo, guarda a PRIMEIRA vez que ele causou cada reacao
 * (id, data, trecho da inscricao). So conta quem conjurou ({@link CastScope}): uma reacao causada por outro jogador
 * entra no diario dele, nao no seu. Sem recompensa mecanica. Reacoes de datapack aparecem sozinhas (a lista vem das
 * regras carregadas).
 */
public final class ReactionJournal extends SavedData {
  public static final String NAME = "arcane_reaction_journal";
  /** Glifos guardados no trecho da inscricao. */
  public static final int EXCERPT_GLYPHS = 6;

  public record Entry(String id, long epochMillis, long gameDay, String excerpt) {}

  private final Map<UUID, LinkedHashMap<String, Entry>> entries = new HashMap<>();

  public ReactionJournal() {}

  public static ReactionJournal get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(ReactionJournal::load, ReactionJournal::new, NAME);
  }

  /** Registra; devolve true se era a primeira vez deste jogador com esta reacao. */
  public boolean record(UUID player, Entry entry) {
    var mine = entries.computeIfAbsent(player, k -> new LinkedHashMap<>());
    if (mine.containsKey(entry.id())) return false;
    mine.put(entry.id(), entry);
    setDirty();
    return true;
  }

  public Map<String, Entry> of(UUID player) {
    var mine = entries.get(player);
    return mine == null ? Map.of() : java.util.Collections.unmodifiableMap(mine);
  }

  public boolean has(UUID player, String id) { return of(player).containsKey(id); }

  /** Trecho da inscricao: os primeiros glifos, por id, separados por virgula (o cliente traduz). */
  public static String excerpt(Inscription inscription) {
    if (inscription == null) return "";
    List<String> ids = new ArrayList<>();
    for (var g : inscription.glyphs()) {
      if (ids.size() >= EXCERPT_GLYPHS) break;
      ids.add(g.glyphId().getPath());
    }
    return String.join(",", ids);
  }

  /** Primeira vez do jogador com a reacao: grava, avisa discretamente e sincroniza o diario. */
  public static void discover(ServerLevel level, ServerPlayer player, String id, String nameKey, Inscription inscription) {
    var journal = get(level.getServer());
    if (journal.has(player.getUUID(), id)) return;
    journal.record(player.getUUID(), new Entry(id, System.currentTimeMillis(), level.getDayTime() / 24000L, excerpt(inscription)));
    player.displayClientMessage(Component.translatable("message.arcane.discovery.reaction",
        Component.translatable(nameKey)).withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
    // o mesmo som de glifo aprendido, em outro tom, so para ele
    player.playNotifySound(com.arcane.registry.ModSounds.GLYPH_LEARNED.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.6f);
    sync(player);
  }

  /** Manda o catalogo de reacoes (todas as carregadas) e o que este jogador ja descobriu. */
  public static void sync(ServerPlayer player) {
    var server = player.getServer();
    if (server == null) return;
    List<com.arcane.network.packet.S2CReactionJournalPacket.Def> catalog = new ArrayList<>();
    for (var r : InteractionRules.distinct()) catalog.add(new com.arcane.network.packet.S2CReactionJournalPacket.Def(r.id(), r.nameKey(), r.hintKey()));
    for (var m : MassReactions.defs()) catalog.add(new com.arcane.network.packet.S2CReactionJournalPacket.Def(m.id(), m.nameKey(), m.hintKey()));
    List<Entry> found = new ArrayList<>(get(server).of(player.getUUID()).values());
    com.arcane.network.ModNetwork.sendToPlayer(player, new com.arcane.network.packet.S2CReactionJournalPacket(catalog, found));
  }

  // ------------------------------------------------------------------ salvar
  @Override
  public CompoundTag save(CompoundTag tag) {
    ListTag players = new ListTag();
    for (var e : entries.entrySet()) {
      CompoundTag p = new CompoundTag();
      p.putUUID("player", e.getKey());
      ListTag list = new ListTag();
      for (Entry en : e.getValue().values()) {
        CompoundTag t = new CompoundTag();
        t.putString("id", en.id());
        t.putLong("when", en.epochMillis());
        t.putLong("day", en.gameDay());
        t.putString("excerpt", en.excerpt());
        list.add(t);
      }
      p.put("entries", list);
      players.add(p);
    }
    tag.put("players", players);
    return tag;
  }

  public static ReactionJournal load(CompoundTag tag) {
    ReactionJournal j = new ReactionJournal();
    for (Tag pt : tag.getList("players", Tag.TAG_COMPOUND)) {
      CompoundTag p = (CompoundTag) pt;
      if (!p.hasUUID("player")) continue;
      var mine = j.entries.computeIfAbsent(p.getUUID("player"), k -> new LinkedHashMap<>());
      for (Tag et : p.getList("entries", Tag.TAG_COMPOUND)) {
        CompoundTag t = (CompoundTag) et;
        String id = t.getString("id");
        if (!id.isEmpty()) mine.putIfAbsent(id, new Entry(id, t.getLong("when"), t.getLong("day"), t.getString("excerpt")));
      }
    }
    return j;
  }

  /** Sincroniza ao entrar e depois de um /reload (reacoes de datapack aparecem sozinhas). */
  @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.arcane.ArcaneMod.MODID)
  public static final class Events {
    private Events() {}

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onDatapack(net.minecraftforge.event.OnDatapackSyncEvent event) {
      if (event.getPlayer() != null) sync(event.getPlayer());
      else event.getPlayerList().getPlayers().forEach(ReactionJournal::sync);
    }
  }
}
