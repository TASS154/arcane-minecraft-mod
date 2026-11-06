package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.discovery.AnalysisService;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.grimoire.GrimoireAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cicatrizes do mundo + assinatura arcana. Magias fortes deixam um residuo temporario onde atingem: brilha na cor do
 * elemento e guarda a "assinatura" de quem conjurou e os glifos usados. Quem analisa o residuo (Projetor de Glifos
 * agachado) descobre o elemento, de quem e a assinatura e avanca o conhecimento de um dos glifos. Ficam so em memoria
 * (somem ao fechar o servidor); o tempo de vida e a ativacao estao na configuracao.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class WorldScars {
  /** Custo base de mana a partir do qual a magia deixa cicatriz. */
  public static final int MIN_MANA_COST = 5;
  public static final int MAX_PER_LEVEL = 48;
  public static final double ANALYZE_RANGE = 8.0;

  public record Scar(Vec3 pos, SpellElement element, List<ResourceLocation> glyphs, UUID caster, long expiresAt) {}

  private static final Map<ResourceKey<Level>, List<Scar>> SCARS = new ConcurrentHashMap<>();

  private WorldScars() {}

  /** Assinatura estavel e curta de um conjurador (dois pares hexadecimais). */
  public static String signature(UUID caster) {
    int h = caster.hashCode();
    return String.format("%02X-%02X", (h >>> 8) & 0xFF, h & 0xFF);
  }

  public static void leave(ExecutionContext ctx, Vec3 pos) {
    if (!ArcaneConfig.SCARS_ENABLED.get() || ctx.plan().baseManaCost() < MIN_MANA_COST) return;
    ServerLevel level = ctx.level();
    List<Scar> list = SCARS.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
    synchronized (list) {
      while (list.size() >= MAX_PER_LEVEL) list.remove(0);
      list.add(new Scar(pos, SpellElement.of(ctx.plan()), ctx.inscription().glyphs().stream().map(g -> g.glyphId()).toList(),
          ctx.caster().getUUID(), level.getGameTime() + ArcaneConfig.SCAR_LIFETIME_TICKS.get()));
    }
  }

  public static List<Scar> all(ServerLevel level) {
    List<Scar> list = SCARS.get(level.dimension());
    if (list == null) return List.of();
    synchronized (list) {
      return new ArrayList<>(list);
    }
  }

  public static void clearAll() {
    SCARS.clear();
  }

  /** Analisa o residuo mais proximo; retorna false se nao ha nenhum ao alcance. */
  public static boolean analyze(ServerPlayer player) {
    Scar best = null;
    double bestD = ANALYZE_RANGE * ANALYZE_RANGE;
    for (Scar s : all(player.serverLevel())) {
      double d = s.pos().distanceToSqr(player.position());
      if (d <= bestD) {
        bestD = d;
        best = s;
      }
    }
    if (best == null) {
      player.displayClientMessage(Component.translatable("message.arcane.scar.none").withStyle(ChatFormatting.GRAY), true);
      return false;
    }
    boolean mine = best.caster().equals(player.getUUID());
    player.displayClientMessage(Component.translatable(mine ? "message.arcane.scar.mine" : "message.arcane.scar.other",
        Component.translatable("element.arcane." + best.element().name().toLowerCase(Locale.ROOT)), signature(best.caster()))
        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    // Estudar o residuo ensina: avanca o conhecimento do primeiro glifo ainda nao dominado.
    var grimoire = GrimoireAttachments.get(player).orElse(null);
    if (grimoire != null) {
      for (ResourceLocation id : best.glyphs()) {
        var known = grimoire.knowledge().getOrDefault(id, GlyphKnowledge.UNKNOWN);
        if (!known.atLeast(GlyphKnowledge.MASTERED)) {
          AnalysisService.advanceGlyph(player, id);
          break;
        }
      }
    }
    return true;
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 10 != 0) return;
    for (ServerLevel level : event.getServer().getAllLevels()) {
      List<Scar> list = SCARS.get(level.dimension());
      if (list == null) continue;
      long now = level.getGameTime();
      synchronized (list) {
        list.removeIf(s -> s.expiresAt() < now);
        for (Scar s : list) {
          level.sendParticles(s.element().particle(), s.pos().x, s.pos().y + 0.15, s.pos().z, 1, 0.35, 0.05, 0.35, 0.0);
        }
      }
    }
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    clearAll();
  }
}
