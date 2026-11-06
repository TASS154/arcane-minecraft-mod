package com.arcane.network;

import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.execution.ManaState;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.glyph.ParameterSchema;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GrimoireState;
import com.arcane.inscription.CompiledPlanCache;
import com.arcane.network.packet.S2CGrimoireSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Envia ao cliente somente o que o HUD, a Bancada e o Grimorio precisam exibir.
 * O servidor continua autoritativo: o cliente nunca decide custo, mana ou conhecimento.
 */
public final class ArcaneSync {
  private static final Map<UUID, Integer> LAST_FULL_HASH = new ConcurrentHashMap<>();
  private static final Map<UUID, EnvironmentalSnapshot> LAST_ENV = new ConcurrentHashMap<>();

  private ArcaneSync() {}

  public static void setEnvironment(ServerPlayer player, EnvironmentalSnapshot env) {
    LAST_ENV.put(player.getUUID(), env);
  }

  public static void forget(ServerPlayer player) {
    LAST_FULL_HASH.remove(player.getUUID());
    LAST_ENV.remove(player.getUUID());
  }

  /** Envia mana sempre; o bloco de conhecimento so quando mudou (ou se forceFull). */
  public static void sync(ServerPlayer player, boolean forceFull) {
    CompoundTag tag = new CompoundTag();
    ManaState mana = GrimoireAttachments.getManaOrCreate(player);
    tag.putFloat("mana", mana.current());
    tag.putFloat("maxMana", mana.max());
    EnvironmentalSnapshot env = LAST_ENV.getOrDefault(player.getUUID(), EnvironmentalSnapshot.NEUTRAL);
    tag.putDouble("potency", env.potencyMultiplier());
    tag.putDouble("poison", env.poisoningExposureRate());

    GrimoireAttachments.get(player).ifPresent(grimoire -> {
      tag.putBoolean("grimoire", grimoire.grimoirePresent());
      CompoundTag full = buildKnowledge(grimoire);
      int hash = full.hashCode();
      Integer last = LAST_FULL_HASH.get(player.getUUID());
      if (forceFull || last == null || last != hash) {
        tag.put("full", full);
        LAST_FULL_HASH.put(player.getUUID(), hash);
      }
    });
    ModNetwork.sendToPlayer(player, new S2CGrimoireSyncPacket(tag));
  }

  static CompoundTag buildKnowledge(GrimoireState grimoire) {
    CompoundTag full = new CompoundTag();
    full.putInt("total", GlyphRegistry.size());
    full.putInt("pages", grimoire.pages());
    full.putInt("stage", grimoire.visualStage());
    full.putInt("maxGlyphs", com.arcane.inscription.SpellLimits.maxGlyphs());
    full.putDouble("costGrowth", com.arcane.inscription.SpellLimits.growth());
    full.putInt("costFree", com.arcane.inscription.SpellLimits.freeGlyphs());

    ListTag glyphs = new ListTag();
    grimoire.knowledge().forEach((id, level) -> {
      CompoundTag g = new CompoundTag();
      g.putString("id", id.toString());
      g.putInt("level", level.ordinal());
      GlyphRegistry.get(id).ifPresent((GlyphDefinition def) -> {
        g.putString("category", def.category().getSerializedName());
        g.putInt("cost", def.cost().mana());
        g.putBoolean("scales", def.category() == com.arcane.glyph.GlyphCategory.EFFECT);
        g.putInt("ritual", def.ritual());
        ListTag params = new ListTag();
        // Parametro principal primeiro: e o que a roda do mouse ajusta na bancada.
        def.primaryParam().ifPresent(primary -> params.add(paramTag(primary, def.parameters().get(primary))));
        def.parameters().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .filter(e -> !e.getKey().equals(def.primaryParam().orElse("")))
            .forEach(e -> params.add(paramTag(e.getKey(), e.getValue())));
        g.put("params", params);
      });
      glyphs.add(g);
    });
    full.put("glyphs", glyphs);

    ListTag spells = new ListTag();
    grimoire.spellbook().forEach((id, inscription) -> {
      CompoundTag s = new CompoundTag();
      s.putUUID("id", id);
      s.putString("name", inscription.metadata().name());
      int cost = -1;
      try {
        cost = CompiledPlanCache.getOrCompile(inscription).baseManaCost();
      } catch (RuntimeException ignored) {
        // glifo removido por datapack: a magia fica listada, sem custo
      }
      s.putInt("cost", cost);
      ListTag entries = new ListTag();
      inscription.glyphs().forEach(gi -> {
        CompoundTag e = new CompoundTag();
        e.putString("id", gi.glyphId().toString());
        CompoundTag p = new CompoundTag();
        gi.parameters().forEach(p::putDouble);
        e.put("p", p);
        entries.add(e);
      });
      s.put("glyphs", entries);
      spells.add(s);
    });
    full.put("spells", spells);
    grimoire.activeInscriptionId().ifPresent(id -> full.putUUID("active", id));

    ListTag log = new ListTag();
    grimoire.discoveryLog().forEach(entry -> log.add(StringTag.valueOf(entry)));
    full.put("log", log);
    return full;
  }

  private static CompoundTag paramTag(String key, ParameterSchema.ParameterDef def) {
    CompoundTag t = new CompoundTag();
    t.putString("k", key);
    t.putDouble("d", def.defaultValue());
    t.putDouble("lo", def.min());
    t.putDouble("hi", def.max());
    t.putBoolean("int", "int".equals(def.type()));
    return t;
  }
}
