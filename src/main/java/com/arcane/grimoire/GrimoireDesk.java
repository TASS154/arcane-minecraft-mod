package com.arcane.grimoire;

import com.arcane.ArcaneMod;
import com.arcane.entity.GrimoireEntity;
import com.arcane.inscription.Inscription;
import com.arcane.item.GrimoireItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O grimorio fisico na bancada (decisoes de Everton):
 * <ul>
 *   <li><b>Escrever</b>: ao gravar uma magia na bancada o livro vai para a mesa e a pena escreve 1 s por glifo. Nesse tempo
 *   o mago nao conjura. Depois ele o pega estando a ate ~4 blocos; se tiver se afastado, o livro fica na mesa.</li>
 *   <li><b>Posse x dono</b>: com o livro na mesa, outro mago o pega com botao direito na bancada. Ele vira portador, nunca
 *   dono: conjura as magias do livro com 60% do poder (glifos que nao conhece aparecem como ???) e nao modifica nada.
 *   O dono fica sem o grimorio (penalidade de perda) ate recupera-lo.</li>
 *   <li><b>Estudo</b>: o portador pousa o livro alheio numa bancada e estuda um glifo desconhecido por dia de jogo
 *   (24.000 ticks; dormir adianta). Enquanto estuda o livro nao serve para conjurar; quem o tira da mesa pausa o
 *   estudo, que fica salvo no proprio livro (por estudante).</li>
 * </ul>
 * Morrer com o livro na mesa nao o derruba nem aplica a penalidade.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class GrimoireDesk {
  public static final double REACH = 4.5;
  public static final int TICKS_PER_GLYPH = 20;
  public static final double FOREIGN_POWER = 0.6;
  public static final long STUDY_TICKS = 24000L;
  private static final String STUDY_TAG = "arcane_study", BORROW_IDX = "arcane_borrow_idx";

  private GrimoireDesk() {}

  // ------------------------------------------------------------------ escrever

  /** A magia foi gravada: a pena escreve por 1 s por glifo; na bancada o livro fica pousado nela. */
  public static void startWriting(ServerPlayer player, BlockPos desk, int glyphs) {
    var state = GrimoireAttachments.get(player).orElse(null);
    if (state == null) return;
    long until = player.level().getGameTime() + (long) TICKS_PER_GLYPH * Math.max(1, glyphs);
    state.setWritingUntil(until);
    if (desk != null) state.setDesk(desk, player.level().dimension().location().toString());
    for (GrimoireEntity e : entitiesOf(player)) {
      if (desk != null) e.setAnchor(desk);
      e.setWritingUntil(until);
    }
  }

  /** Motivo para nao conjurar agora (chave de traducao) ou nulo. */
  public static String blockedReason(ServerPlayer player) {
    var state = GrimoireAttachments.get(player).orElse(null);
    if (state == null) return null;
    if (state.writingUntil() > player.level().getGameTime()) return "message.arcane.grimoire.writing";
    if (state.deskPos().isPresent()) return "message.arcane.grimoire.on_desk";
    return null;
  }

  private static List<GrimoireEntity> entitiesOf(ServerPlayer player) {
    List<GrimoireEntity> out = new ArrayList<>();
    for (ServerLevel level : player.server.getAllLevels()) {
      for (var e : level.getAllEntities()) {
        if (e instanceof GrimoireEntity g && !g.isStudy() && g.ownerUuid().map(player.getUUID()::equals).orElse(false)) out.add(g);
      }
    }
    return out;
  }

  private static boolean sameDim(ServerPlayer player, GrimoireState state) {
    return player.level().dimension().location().toString().equals(state.deskDim());
  }

  /** Pega o livro de volta da mesa (o dono, estando perto e a pena tendo terminado). */
  public static void retrieve(ServerPlayer player, GrimoireState state) {
    state.setDesk(null, null);
    for (GrimoireEntity e : entitiesOf(player)) e.setAnchor(null);
    player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.1f);
    player.displayClientMessage(Component.translatable("message.arcane.grimoire.retrieved").withStyle(ChatFormatting.LIGHT_PURPLE), true);
  }

  /** Por jogador: tinta e som enquanto escreve; o dono pega o livro ao chegar perto. */
  public static void tickPlayer(ServerPlayer player) {
    var state = GrimoireAttachments.get(player).orElse(null);
    if (state == null) return;
    long now = player.level().getGameTime();
    if (state.writingUntil() > now) {
      var where = state.deskPos().filter(p -> sameDim(player, state)).map(p -> p.getCenter().add(0, 0.7, 0))
          .orElse(player.position().add(0, 1.2, 0));
      if (now % 4 == 0) {
        player.serverLevel().sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.05f, 0.05f, 0.12f), 0.5f),
            where.x, where.y - 0.25, where.z, 2, 0.12, 0.02, 0.12, 0.0);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, where.x, where.y + 0.2, where.z, 3, 0.25, 0.1, 0.25, 0.4);
      }
      if (now % 10 == 0) player.level().playSound(null, BlockPos.containing(where), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.5f, 1.4f);
      return;
    }
    if (state.writingUntil() > 0 && state.writingUntil() <= now && now - state.writingUntil() < 2) {
      player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 0.7f, 1.0f);
    }
    var desk = state.deskPos().orElse(null);
    if (desk == null || !sameDim(player, state)) return;
    if (player.isAlive() && player.position().distanceTo(desk.getCenter()) <= REACH) retrieve(player, state);
  }

  /** Livro do dono pousado nesta bancada (dono online), se houver. */
  private static ServerPlayer deskOwnerAt(ServerLevel level, BlockPos pos) {
    String dim = level.dimension().location().toString();
    for (ServerPlayer p : level.players()) {
      var st = GrimoireAttachments.get(p).orElse(null);
      if (st != null && st.deskPos().map(pos::equals).orElse(false) && dim.equals(st.deskDim())) return p;
    }
    return null;
  }

  // ------------------------------------------------------------------ bancada: pegar, estudar, devolver

  /** Botao direito na bancada. Devolve true se tratou o clique (o menu nao abre). */
  public static boolean onWorkbenchUse(ServerPlayer user, ServerLevel level, BlockPos pos) {
    Key key = new Key(level.dimension(), pos.asLong());
    // 1) um livro em estudo nesta mesa: quem clica o pega (o estudo fica salvo nele)
    Study study = STUDIES.get(key);
    if (study != null) {
      STUDIES.remove(key);
      dirty(level.getServer());
      ItemStack book = study.saveInto();
      if (GrimoireItem.isOwnedBy(book, user.getUUID())) restoreOwner(user, book);
      else {
        if (!user.getInventory().add(book)) user.drop(book, false);
        user.displayClientMessage(Component.translatable("message.arcane.grimoire.study_taken").withStyle(ChatFormatting.GRAY), true);
      }
      level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 0.9f);
      return true;
    }
    // 2) livro de outro dono, pousado escrevendo/esquecido: quem nao e o dono o pega
    ServerPlayer owner = deskOwnerAt(level, pos);
    if (owner != null && owner != user) {
      steal(user, owner);
      return true;
    }
    // 3) com um grimorio alheio na mao: pousa para estudar
    ItemStack held = user.getMainHandItem();
    if (GrimoireItem.contents(held) != null && !GrimoireItem.isOwnedBy(held, user.getUUID())) {
      startStudy(user, level, pos, held);
      return true;
    }
    return false;
  }

  /** Outro mago pega o livro da mesa: vira portador; o dono fica sem grimorio (penalidade) ate recupera-lo. */
  public static void steal(ServerPlayer thief, ServerPlayer owner) {
    var state = GrimoireAttachments.get(owner).orElse(null);
    if (state == null) return;
    state.setDesk(null, null);
    state.setWritingUntil(0L);
    CompoundTag snapshot = new CompoundTag();
    state.save(snapshot);
    ItemStack book = GrimoireItem.createFor(owner, snapshot);
    for (GrimoireEntity e : entitiesOf(owner)) e.discard();
    CompoundTag blank = new CompoundTag();
    GrimoireState.createDefault().save(blank);
    state.load(blank);
    GrimoireLifecycleHandler.markGrimoireDestroyed(owner, owner.level().getGameTime());
    if (!thief.getInventory().add(book)) thief.drop(book, false);
    thief.level().playSound(null, thief.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.7f);
    thief.displayClientMessage(Component.translatable("message.arcane.grimoire.took_foreign", owner.getGameProfile().getName())
        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    owner.displayClientMessage(Component.translatable("message.arcane.grimoire.stolen_by", thief.getGameProfile().getName())
        .withStyle(ChatFormatting.RED), false);
    com.arcane.network.ArcaneSync.sync(owner, true);
  }

  /** O dono recupera o proprio livro (o mesmo que pega-lo do chao). */
  public static void restoreOwner(ServerPlayer owner, ItemStack book) {
    CompoundTag contents = GrimoireItem.contents(book);
    if (contents == null) return;
    GrimoireAttachments.get(owner).ifPresent(g -> {
      GrimoireTransfer.restore(g, contents);
      g.setGrimoirePresent(true);
      g.setGrimoireDestroyedUntilTick(0L);
      g.setDesk(null, null);
      g.setWritingUntil(0L);
      g.setVisualStage(GrimoireState.stageFor(g.pages()));
    });
    owner.displayClientMessage(Component.translatable("message.arcane.grimoire.restored").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    GrimoireAttachments.updateManaMultiplier(owner);
    GrimoireLifecycleHandler.spawnGrimoire(owner);
    com.arcane.network.ArcaneSync.sync(owner, true);
  }

  // ------------------------------------------------------------------ portador: conjurar com 60%

  private static List<Inscription> spellsOf(ItemStack book) {
    CompoundTag contents = GrimoireItem.contents(book);
    if (contents == null) return List.of();
    GrimoireState st = new GrimoireState();
    st.load(contents);
    return new ArrayList<>(st.spellbook().values());
  }

  /** Nome da magia do livro alheio com os glifos que o portador nao conhece como ???. */
  public static Component describe(ServerPlayer holder, Inscription spell) {
    var mine = GrimoireAttachments.get(holder).orElse(null);
    List<String> parts = new ArrayList<>();
    for (var g : spell.glyphs()) {
      boolean known = mine != null && mine.knowsGlyph(g.glyphId(), GlyphKnowledge.IDENTIFIED);
      parts.add(known ? Component.translatable(com.arcane.discovery.DiscoveryService.glyphKey(g.glyphId())).getString() : "???");
    }
    String name = spell.metadata().name();
    return Component.literal((name == null || name.isBlank() ? Component.translatable("gui.arcane.unnamed").getString() : name)
        + ": " + String.join(" > ", parts));
  }

  /** Botao direito com o grimorio alheio na mao: conjura a magia escolhida (agachado troca de magia). */
  public static void useForeign(ServerPlayer holder, ItemStack book, boolean next) {
    List<Inscription> spells = spellsOf(book);
    if (spells.isEmpty()) {
      holder.displayClientMessage(Component.translatable("message.arcane.grimoire.foreign_empty").withStyle(ChatFormatting.GRAY), true);
      return;
    }
    int idx = Math.floorMod(book.getOrCreateTag().getInt(BORROW_IDX), spells.size());
    if (next) {
      idx = (idx + 1) % spells.size();
      book.getOrCreateTag().putInt(BORROW_IDX, idx);
      holder.displayClientMessage(Component.translatable("message.arcane.grimoire.foreign_spell", describe(holder, spells.get(idx)), idx + 1, spells.size())
          .withStyle(ChatFormatting.LIGHT_PURPLE), true);
      return;
    }
    Inscription spell = spells.get(idx);
    var result = com.arcane.execution.CastingService.castForeign(holder, spell, FOREIGN_POWER);
    if (!result.success()) {
      holder.displayClientMessage(Component.translatable(result.message()).withStyle(ChatFormatting.RED), true);
    }
  }

  // ------------------------------------------------------------------ estudo

  private record Key(ResourceKey<Level> dim, long pos) {}

  private static final class Study {
    ItemStack book;
    UUID studier;
    ResourceLocation glyph;
    long ticks;
    long lastDay = -1;

    /** O livro com o progresso deste estudante gravado nele. */
    ItemStack saveInto() {
      ItemStack out = book.copy();
      CompoundTag all = out.getOrCreateTag().getCompound(STUDY_TAG);
      CompoundTag mine = new CompoundTag();
      if (glyph != null) mine.putString("glyph", glyph.toString());
      mine.putLong("ticks", ticks);
      all.put(studier.toString(), mine);
      out.getOrCreateTag().put(STUDY_TAG, all);
      return out;
    }
  }

  private static final Map<Key, Study> STUDIES = new HashMap<>();
  private static Store store;

  /** Bancada (na dimensao dada) onde o grimorio deste dono esta sendo estudado, ou nulo. */
  public static net.minecraft.world.phys.Vec3 studyOf(Level level, UUID owner) {
    for (var e : STUDIES.entrySet()) {
      if (e.getKey().dim() == level.dimension() && GrimoireItem.isOwnedBy(e.getValue().book, owner)) return BlockPos.of(e.getKey().pos()).getCenter();
    }
    return null;
  }

  public static boolean studyAt(Level level, BlockPos pos) { return STUDIES.containsKey(new Key(level.dimension(), pos.asLong())); }

  /** Progresso salvo no livro para este estudante (ticks), para os testes e a dica. */
  public static long savedProgress(ItemStack book, UUID studier) {
    if (!book.hasTag()) return 0;
    return book.getTag().getCompound(STUDY_TAG).getCompound(studier.toString()).getLong("ticks");
  }

  /** Glifos do livro que o estudante ainda nao identificou, na ordem em que aparecem. */
  public static List<ResourceLocation> unknownGlyphs(ServerPlayer studier, ItemStack book) {
    var mine = GrimoireAttachments.get(studier).orElse(null);
    Set<ResourceLocation> out = new LinkedHashSet<>();
    for (Inscription s : spellsOf(book)) for (var g : s.glyphs()) {
      if (mine == null || !mine.knowsGlyph(g.glyphId(), GlyphKnowledge.IDENTIFIED)) out.add(g.glyphId());
    }
    return new ArrayList<>(out);
  }

  public static void startStudy(ServerPlayer user, ServerLevel level, BlockPos pos, ItemStack held) {
    Key key = new Key(level.dimension(), pos.asLong());
    if (STUDIES.containsKey(key) || deskOwnerAt(level, pos) != null) {
      user.displayClientMessage(Component.translatable("message.arcane.grimoire.study_busy").withStyle(ChatFormatting.RED), true);
      return;
    }
    List<ResourceLocation> unknown = unknownGlyphs(user, held);
    if (unknown.isEmpty()) {
      user.displayClientMessage(Component.translatable("message.arcane.grimoire.study_none").withStyle(ChatFormatting.GRAY), true);
      return;
    }
    Study s = new Study();
    s.book = held.copyWithCount(1);
    s.studier = user.getUUID();
    CompoundTag saved = s.book.getOrCreateTag().getCompound(STUDY_TAG).getCompound(user.getUUID().toString());
    s.glyph = saved.contains("glyph") ? com.arcane.glyph.GlyphAliases.resolve(new ResourceLocation(saved.getString("glyph"))) : unknown.get(0);
    s.ticks = saved.getLong("ticks");
    if (!unknown.contains(s.glyph)) { s.glyph = unknown.get(0); s.ticks = 0; }
    held.shrink(1);
    STUDIES.put(key, s);
    dirty(level.getServer());
    ensureBook(level, pos, s);
    level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.8f, 1.0f);
    user.displayClientMessage(Component.translatable("message.arcane.grimoire.study_start", unknown.size()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
  }

  private static void ensureBook(ServerLevel level, BlockPos pos, Study s) {
    if (!level.isLoaded(pos)) return;
    var near = level.getEntitiesOfClass(GrimoireEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5), GrimoireEntity::isStudy);
    if (!near.isEmpty()) return;
    GrimoireEntity e = new GrimoireEntity(com.arcane.registry.ModEntities.GRIMOIRE.get(), level);
    CompoundTag contents = GrimoireItem.contents(s.book);
    int pages = contents == null ? GrimoireState.DEFAULT_PAGES : Math.max(1, contents.getInt("pages"));
    e.markStudy(pos, GrimoireState.stageFor(pages));
    level.addFreshEntity(e);
  }

  /** Avanca os estudos: 1 por tick, ou o salto do relogio do dia (dormir). Estudante fora do jogo pausa. */
  public static void tickStudies(MinecraftServer server) {
    if (STUDIES.isEmpty()) return;
    for (var entry : new ArrayList<>(STUDIES.entrySet())) {
      ServerLevel level = server.getLevel(entry.getKey().dim());
      Study s = entry.getValue();
      if (level == null) continue;
      long day = level.getDayTime();
      long delta = 1;
      if (s.lastDay >= 0 && level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
        long d = day - s.lastDay;
        if (d > 1 && d <= STUDY_TICKS) delta = d;
      }
      s.lastDay = day;
      ServerPlayer studier = findPlayer(server, s.studier);
      if (studier == null) continue;
      BlockPos pos = BlockPos.of(entry.getKey().pos());
      if (server.getTickCount() % 20 == 0) ensureBook(level, pos, s);
      s.ticks += delta;
      if (server.getTickCount() % 100 == 0) {
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.5);
        dirty(server);
      }
      if (s.ticks < STUDY_TICKS) continue;
      var mine = GrimoireAttachments.get(studier).orElse(null);
      if (mine != null && s.glyph != null) {
        mine.learnGlyph(s.glyph, GlyphKnowledge.IDENTIFIED);
        mine.logDiscovery("study:" + s.glyph);
        studier.displayClientMessage(Component.translatable("message.arcane.grimoire.study_learned",
            Component.translatable(com.arcane.discovery.DiscoveryService.glyphKey(s.glyph))).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        com.arcane.network.ArcaneSync.sync(studier, true);
      }
      List<ResourceLocation> left = unknownGlyphs(studier, s.book);
      s.ticks = 0;
      s.glyph = left.isEmpty() ? null : left.get(0);
      if (left.isEmpty()) {
        studier.displayClientMessage(Component.translatable("message.arcane.grimoire.study_done").withStyle(ChatFormatting.GRAY), false);
        s.ticks = Long.MIN_VALUE / 4; // nada mais a aprender: para de contar
      }
      dirty(server);
    }
  }

  private static ServerPlayer findPlayer(MinecraftServer server, UUID id) {
    ServerPlayer p = server.getPlayerList().getPlayer(id);
    if (p != null) return p;
    for (ServerLevel l : server.getAllLevels()) for (ServerPlayer q : l.players()) if (q.getUUID().equals(id)) return q;
    return null;
  }

  /** Para os testes: avanca o estudo desta bancada em {@code ticks}. */
  public static void advanceStudy(ServerLevel level, BlockPos pos, long ticks) {
    Study s = STUDIES.get(new Key(level.dimension(), pos.asLong()));
    if (s != null) s.ticks += ticks;
  }

  // ------------------------------------------------------------------ eventos e persistencia

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer p) tickPlayer(p);
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    if (server == null) return;
    if (store == null) load(server);
    tickStudies(server);
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    STUDIES.clear();
    store = null;
  }

  private static void load(MinecraftServer server) {
    store = server.overworld().getDataStorage().computeIfAbsent(Store::load, Store::new, "arcane_grimoire_studies");
  }

  private static void dirty(MinecraftServer server) {
    if (store == null) load(server);
    store.setDirty();
  }

  /** Estudos salvos no mundo (o livro pousado nao some ao reiniciar). */
  public static final class Store extends SavedData {
    public static Store load(CompoundTag tag) {
      STUDIES.clear();
      for (Tag t : tag.getList("studies", Tag.TAG_COMPOUND)) {
        CompoundTag c = (CompoundTag) t;
        Study s = new Study();
        s.book = ItemStack.of(c.getCompound("book"));
        s.studier = c.getUUID("studier");
        s.glyph = c.contains("glyph") ? com.arcane.glyph.GlyphAliases.resolve(new ResourceLocation(c.getString("glyph"))) : null;
        s.ticks = c.getLong("ticks");
        ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(c.getString("dim")));
        STUDIES.put(new Key(dim, c.getLong("pos")), s);
      }
      return new Store();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      for (var e : STUDIES.entrySet()) {
        CompoundTag c = new CompoundTag();
        c.put("book", e.getValue().book.save(new CompoundTag()));
        c.putUUID("studier", e.getValue().studier);
        if (e.getValue().glyph != null) c.putString("glyph", e.getValue().glyph.toString());
        c.putLong("ticks", e.getValue().ticks);
        c.putString("dim", e.getKey().dim().location().toString());
        c.putLong("pos", e.getKey().pos());
        list.add(c);
      }
      tag.put("studies", list);
      return tag;
    }
  }

}
