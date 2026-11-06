package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.VowFormula;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Juramentos: glifos que trocam uma restricao por poder. O bonus (formula em {@link VowFormula}) vem do compilador;
 * aqui fica a parte que so o servidor decide: a restricao da "promessa" escolhida e o preco. Quanto maior a promessa
 * (1 a 5), maior o bonus e mais dura a restricao:
 * <ul>
 *   <li>Quietude: ficar parado no chao por (promessa-1) segundos.</li>
 *   <li>Sangue: custa 2 x promessa de vida; recusa se sobrarem menos de 2 pontos depois do preco.</li>
 *   <li>Luz: 1 dia; 2 ceu aberto; 3 sem chuva; 4 entre 1000 e 10000 ticks do dia; 5 entre 4000 e 8000 (perto do meio-dia).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class Vows {
  /** Ultimo tick em que cada jogador se mexeu (para a Quietude). */
  private static final Map<UUID, Long> LAST_MOVE = new ConcurrentHashMap<>();

  private Vows() {}

  private static GlyphInstance find(Inscription inscription, net.minecraft.resources.ResourceLocation id) {
    return inscription.glyphs().stream().filter(g -> id.equals(g.glyphId())).findFirst().orElse(null);
  }

  public static float bloodCost(int pledge) {
    return 2.0f * pledge;
  }

  public static int stillnessTicks(int pledge) {
    return (pledge - 1) * 20;
  }

  @SubscribeEvent
  public static void onTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
    var p = event.player;
    double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
    if (!p.onGround() || dx * dx + dz * dz >= 1.0e-4) LAST_MOVE.put(p.getUUID(), p.level().getGameTime());
  }

  /** Chave de mensagem da restricao quebrada, ou null se todos os juramentos da magia foram cumpridos. */
  public static String violation(LivingEntity caster, Inscription inscription) {
    if (caster instanceof ServerPlayer silenced && OathPenalties.active(silenced, OathPenalties.SILENCE)) {
      return "message.arcane.oath.silenced";
    }
    GlyphInstance still = find(inscription, VowFormula.STILLNESS);
    if (still != null) {
      double dx = caster.getX() - caster.xo, dz = caster.getZ() - caster.zo;
      if (!caster.onGround() || dx * dx + dz * dz >= 1.0e-4) return "message.arcane.vow.stillness";
      if (caster instanceof ServerPlayer sp) {
        long since = sp.level().getGameTime() - LAST_MOVE.getOrDefault(sp.getUUID(), 0L);
        if (since < stillnessTicks(VowFormula.pledge(still))) return "message.arcane.vow.stillness";
      }
    }
    GlyphInstance day = find(inscription, VowFormula.DAYLIGHT);
    if (day != null) {
      int n = VowFormula.pledge(day);
      var level = caster.level();
      long t = level.getDayTime() % 24000L;
      if (!level.isDay()) return "message.arcane.vow.daylight";
      if (n >= 2 && !level.canSeeSky(caster.blockPosition())) return "message.arcane.vow.daylight";
      if (n >= 3 && (level.isRaining() || level.isThundering())) return "message.arcane.vow.daylight";
      if (n == 4 && (t < 1000 || t > 10000)) return "message.arcane.vow.daylight";
      if (n >= 5 && (t < 4000 || t > 8000)) return "message.arcane.vow.daylight";
    }
    GlyphInstance blood = find(inscription, VowFormula.BLOOD);
    if (blood != null && caster.getHealth() <= bloodCost(VowFormula.pledge(blood)) + 2.0f) return "message.arcane.vow.blood";
    return null;
  }

  /** Cobra o preco dos juramentos depois de a magia ter sido conjurada com sucesso. */
  public static void pay(LivingEntity caster, Inscription inscription) {
    GlyphInstance blood = find(inscription, VowFormula.BLOOD);
    GlyphInstance oath = find(inscription, VowFormula.OATH);
    if (oath != null && caster instanceof ServerPlayer sp) {
      OathPenalties.apply(sp, VowFormula.oathKind(oath), VowFormula.oathSeconds(oath));
    }
    if (blood != null) caster.hurt(caster.damageSources().magic(), bloodCost(VowFormula.pledge(blood)));
  }
}
