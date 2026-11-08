package com.arcane.inscribing;

import com.arcane.execution.CastingService;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import com.arcane.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Inscricoes escritas no mundo com a Pena Arcana.
 * Gatilhos do MVP (DOCUMENTACAO_COMPLETA, "Aplicacao em Objetos"):
 *   on_open  - bau: dispara quando alguem abre (InscribingHandler);
 *   on_touch - qualquer outro bloco: dispara quando uma criatura pisa nele.
 */
public final class InscribingService {
  public static final String TRIGGER_ON_OPEN = "on_open";
  public static final String TRIGGER_ON_TOUCH = "on_touch";
  /** Intervalo minimo entre dois disparos do mesmo bloco por toque (provisorio). */
  public static final int TOUCH_COOLDOWN_TICKS = 40;

  private static final Map<ServerLevel, Map<BlockPos, Long>> LAST_TRIGGER = new WeakHashMap<>();

  private InscribingService() {}

  public static boolean inscribe(ServerLevel level, Player player, BlockPos pos, List<GlyphInstance> glyphs,
                                 ResourceLocation ink, String trigger) {
    return inscribe(level, player, pos, glyphs, ink, trigger, "");
  }

  public static boolean inscribe(ServerLevel level, Player player, BlockPos pos, List<GlyphInstance> glyphs,
                                 ResourceLocation ink, String trigger, String name) {
    if (!PenRegistry.canWriteOnBlock(player, pos, level)) return false;
    Inscription inscription = new Inscription(new ArrayList<>(glyphs), new InscriptionMetadata(
        name, ink, new ResourceLocation("arcane", "arcane_pen"), com.arcane.execution.PrecisionSnapshot.FIELD_POWER,
        Optional.of(player.getUUID()), level.getGameTime(), InscriptionSourceKind.WORLD));
    WorldInscriptionIndex.get(level).put(new InscriptionAnchor(pos, inscription, trigger, ink));
    if (InkRegistry.isBloodInk(ink)) {
      player.hurt(player.damageSources().magic(), 2f);
    }
    level.playSound(null, pos, com.arcane.registry.ModSounds.PEN_STROKE.get(),
        net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
    level.sendParticles(ModParticles.ARCANE_SPARK.get(),
        pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 10, 0.3, 0.02, 0.3, 0.0);
    return true;
  }

  public static boolean erase(ServerLevel level, BlockPos pos) {
    boolean removed = WorldInscriptionIndex.get(level).remove(pos).isPresent();
    if (removed) {
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
          pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 8, 0.3, 0.02, 0.3, 0.0);
    }
    Map<BlockPos, Long> cooldowns = LAST_TRIGGER.get(level);
    if (cooldowns != null) cooldowns.remove(pos);
    return removed;
  }

  /** Chamado a cada 10 ticks: dispara gatilhos de toque e mostra as inscricoes visiveis. */
  public static void tickWorld(ServerLevel level, boolean showParticles) {
    WorldInscriptionIndex index = WorldInscriptionIndex.get(level);
    if (index.isEmpty()) return;
    long now = level.getGameTime();
    Map<BlockPos, Long> cooldowns = LAST_TRIGGER.computeIfAbsent(level, l -> new HashMap<>());

    for (InscriptionAnchor anchor : List.copyOf(index.all())) {
      BlockPos pos = anchor.pos();
      if (!level.isLoaded(pos)) continue;

      if (showParticles && !"invisible".equals(anchor.ink().getPath())) {
        level.sendParticles(ModParticles.ARCANE_SPARK.get(),
            pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.3, 0.0, 0.3, 0.0);
      }

      if (!TRIGGER_ON_TOUCH.equals(anchor.triggerType())) continue;
      Long last = cooldowns.get(pos);
      if (last != null && now - last < TOUCH_COOLDOWN_TICKS) continue;
      AABB top = new AABB(pos.getX(), pos.getY() + 0.9, pos.getZ(), pos.getX() + 1, pos.getY() + 1.6, pos.getZ() + 1);
      List<LivingEntity> touching = level.getEntitiesOfClass(LivingEntity.class, top,
          e -> e.isAlive() && !e.isSpectator() && e.onGround());
      if (touching.isEmpty()) continue;
      cooldowns.put(pos, now);
      // A criatura que pisa e o alvo/origem: Eu + Dano numa pedra vira armadilha.
      CastingService.tryCast(touching.get(0), anchor.inscription(), level);
    }
  }
}
