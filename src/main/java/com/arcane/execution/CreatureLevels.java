package com.arcane.execution;

import com.arcane.config.ArcaneConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nivel das criaturas contra as magias de controle (Petrificar, Congelar, Sono, Medo, Silencio, Levitar, Maldicao,
 * Selamento). Cada criatura tem um nivel (vida maxima + 2 x armadura) e so e controlada se a magia tiver mana
 * suficiente: a forca da magia (o custo base ampliado por carga, ritual, cristal e precisao) dividida pelo nivel x
 * {@code controlManaPerLevel}.
 * <ul>
 *   <li>forca &gt;= 100% do necessario: efeito inteiro;</li>
 *   <li>de 50% a 100%: o efeito entra mais fraco (a duracao cai na mesma proporcao);</li>
 *   <li>abaixo de 50%: a criatura resiste (faisca cinza, nada acontece).</li>
 * </ul>
 * Excecoes: jogadores nao tem nivel (sempre afetados, como nas cenas de referencia); chefes (Dragao, Wither, Hidra e
 * tudo marcado como chefe) resistem sempre ao controle.
 */
public final class CreatureLevels {
  public static final Set<String> CONTROL = Set.of("petrify", "freeze", "sleep", "fear", "silence", "levitate", "curse", "seal_creature");
  public static final double WEAK_FLOOR = 0.5;

  private CreatureLevels() {}

  public static boolean isControl(String glyphPath) { return CONTROL.contains(glyphPath); }

  /** Nivel da criatura: vida maxima + 2 x armadura. */
  public static double level(LivingEntity e) {
    double armor = e.getAttribute(Attributes.ARMOR) == null ? 0 : e.getAttributeValue(Attributes.ARMOR);
    return e.getMaxHealth() + 2.0 * armor;
  }

  public static boolean isBoss(LivingEntity e) {
    return e instanceof com.arcane.entity.HydraEntity || e.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES)
        || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss;
  }

  /** Mana que a magia precisa ter para controlar esta criatura por inteiro. */
  public static double required(LivingEntity e) { return level(e) * ArcaneConfig.CONTROL_MANA_PER_LEVEL.get(); }

  /** Forca da magia em "mana": o custo base com todos os amplificadores da conjuracao. */
  public static double spellMana(ExecutionContext context) {
    return context.amplify(Math.max(1, context.plan().baseManaCost()), List.of(), "power");
  }

  /** Fracao (0..1) do efeito que a criatura recebe: 1 inteiro, 0 resiste. */
  public static double fraction(ExecutionContext context, LivingEntity e) {
    if (e instanceof Player) return 1.0;
    if (isBoss(e)) return 0.0;
    double need = required(e);
    if (need <= 0) return 1.0;
    double f = spellMana(context) / need;
    if (f >= 1.0) return 1.0;
    return f >= WEAK_FLOOR ? f : 0.0;
  }

  /**
   * Aplica um efeito de controle respeitando o nivel de cada alvo: quem tem forca inteira recebe a chamada original;
   * quem fica entre 50% e 100% recebe a mesma chamada com a duracao reduzida; quem resiste fica de fora.
   */
  public static void apply(ExecutionContext context, EffectCall call, TargetList targets, com.arcane.api.effect.EffectHandler handler) {
    List<Entity> full = new ArrayList<>();
    Map<Long, List<Entity>> weak = new HashMap<>();
    for (Entity e : targets.entities()) {
      if (!(e instanceof LivingEntity living) || e == context.caster()) { full.add(e); continue; }
      double f = fraction(context, living);
      if (f >= 1.0) full.add(e);
      else if (f <= 0.0) resisted(context, living);
      else weak.computeIfAbsent(Math.round(f * 20), k -> new ArrayList<>()).add(e); // grupos de 5%
    }
    handler.apply(context, call, new TargetList(full, targets.blocks(), targets.points()));
    for (var group : weak.entrySet()) {
      double f = group.getKey() / 20.0;
      Map<String, Double> params = new HashMap<>(call.parameters());
      params.replaceAll((k, v) -> k.equals("duration") || k.equals("seconds") ? v * f : v);
      if (!params.containsKey("duration")) params.put("duration", 100.0 * f);
      handler.apply(context, new EffectCall(call.glyphId(), call.handler(), params, call.magnitudeKeys()),
          new TargetList(group.getValue(), List.of(), List.of()));
    }
  }

  private static void resisted(ExecutionContext context, LivingEntity e) {
    context.level().sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 8, e.getBbWidth() * 0.4, 0.3, e.getBbWidth() * 0.4, 0.02);
    context.level().sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 6, e.getBbWidth() * 0.4, 0.3, e.getBbWidth() * 0.4, 0.2);
    if (context.caster() instanceof Player p) {
      p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.control.resisted", e.getDisplayName())
          .withStyle(net.minecraft.ChatFormatting.GRAY), true);
    }
  }
}
