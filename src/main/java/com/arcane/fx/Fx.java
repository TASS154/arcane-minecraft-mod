package com.arcane.fx;

import com.arcane.execution.SpellElement;
import com.arcane.registry.ModParticles;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Fachada de efeitos visuais e sonoros dos elementos (servidor). Todo efeito de elemento passa por aqui: escolhe a
 * familia de sprites e os sons pelo json do elemento ({@link com.arcane.execution.ElementVisuals}) e aplica o orcamento.
 *
 * <p><b>Composicao:</b> o elemento 1 da magia e o corpo do efeito (conjuracao, projetil); o elemento 2 (o segundo
 * elemento diferente; sem ele, o proprio 1) e o rastro e o impacto.
 *
 * <p><b>Orcamento:</b> cada pedido passa por {@link #budget}: escala pela qualidade (config), e o total por fonte e por
 * tick tem teto ({@code fxMaxParticlesPerCast}). Efeitos de area usam {@link #areaCount}, que cresce no maximo
 * linearmente com o raio (um anel de raio 128 nao manda 128 x 128 particulas).
 */
public final class Fx {
  /** Papel da particula: muda tempo de vida, tamanho e movimento no cliente. */
  public enum Role { BODY, TRAIL, IMPACT, DECAL, REACTION }

  /** Papel do som: conjuracao ou impacto (o volume e o tom variam com a potencia). */
  public enum SoundRole { CAST, IMPACT }

  /** Reacoes elementais com visual proprio (a mecanica continua em InteractionRules). */
  public enum Reaction { STEAM, SHATTER, HISS }

  private Fx() {}

  // ------------------------------------------------------------------ orcamento
  private static final Map<Object, Integer> SPENT = new HashMap<>();
  private static long spentTick = Long.MIN_VALUE;
  private static long sentTotal;

  /** Quantas particulas um pedido de {@code requested} pode mandar agora, pela fonte {@code source} (null = global). */
  public static int budget(ServerLevel level, Object source, int requested) {
    if (requested <= 0) return 0;
    long now = level.getGameTime();
    if (now != spentTick) {
      SPENT.clear();
      spentTick = now;
    }
    int scaled = scaled(requested, FxSettings.quality());
    Object key = source == null ? "global" : source;
    int used = SPENT.getOrDefault(key, 0);
    int allowed = Math.max(0, Math.min(scaled, FxSettings.maxPerCast() - used));
    if (allowed > 0) SPENT.put(key, used + allowed);
    sentTotal += allowed;
    return allowed;
  }

  /** Pedido escalado pela qualidade (0,25 a 2); nunca zera um pedido positivo. */
  public static int scaled(int requested, double quality) {
    if (requested <= 0) return 0;
    return Math.max(1, (int) Math.round(requested * Mth.clamp(quality, 0.0, 2.0)));
  }

  /**
   * Particulas para uma area de raio {@code radius}: {@code perBlock} por bloco de raio (perimetro, nao area), no minimo
   * {@code min}. Linear no raio por construcao.
   */
  public static int areaCount(double radius, double perBlock, int min) {
    return Math.max(min, (int) Math.ceil(Math.max(0.0, radius) * perBlock));
  }

  /** Total mandado desde o inicio (testes medem a diferenca). */
  public static long sentTotal() { return sentTotal; }

  // ------------------------------------------------------------------ particulas
  public static ElementFxOptions options(SpellElement e, Role role, float scale, int color) {
    var visual = e.visual();
    boolean decal = role == Role.DECAL;
    String family = decal ? visual.decal() : visual.sprite();
    return new ElementFxOptions(ModParticles.fx(family, decal), e.ordinal(), role.ordinal(), scale, color, Direction.UP.get3DDataValue());
  }

  /** Nuvem de particulas do elemento; devolve quantas foram mandadas. */
  public static int burst(ServerLevel level, Entity source, Vec3 at, SpellElement e, Role role, int count, double spread, double speed) {
    return burst(level, source, at, e, role, count, spread, speed, 1f, -1);
  }

  public static int burst(ServerLevel level, Entity source, Vec3 at, SpellElement e, Role role, int count, double spread,
                          double speed, float scale, int color) {
    int n = budget(level, source == null ? null : source.getUUID(), count);
    if (n > 0) level.sendParticles(options(e, role, scale, color), at.x, at.y, at.z, n, spread, spread, spread, speed);
    return n;
  }

  /** Particulas espalhadas num anel de raio {@code radius} (linear no raio). */
  public static int ring(ServerLevel level, Entity source, Vec3 center, double radius, SpellElement e, Role role, double perBlock) {
    int want = areaCount(radius, perBlock, 6);
    int n = budget(level, source == null ? null : source.getUUID(), want);
    var opts = options(e, role, 1f, -1);
    for (int i = 0; i < n; i++) {
      double a = (Math.PI * 2 * i) / n + level.random.nextDouble() * 0.2;
      level.sendParticles(opts, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius, 1, 0.05, 0.05, 0.05, 0.01);
    }
    return n;
  }

  /**
   * Faiscas numa casca de blocos (domo, barreira) que se desfaz. A casca tem ~raio^2 blocos; as faiscas saem de uma
   * amostra proporcional ao raio (sqrt do numero de blocos), nunca de todos.
   */
  public static int shellSparks(ServerLevel level, Entity source, java.util.List<net.minecraft.core.BlockPos> shell, SpellElement e) {
    if (shell.isEmpty()) return 0;
    double radius = Math.sqrt(shell.size() / (4 * Math.PI)) + 1;
    int n = budget(level, source == null ? null : source.getUUID(), areaCount(radius, 3.0, 8));
    var opts = options(e, Role.IMPACT, 1f, -1);
    for (int i = 0; i < n; i++) {
      var pos = shell.get(level.random.nextInt(shell.size()));
      level.sendParticles(opts, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
    }
    return n;
  }

  /** Marca no bloco atingido (no rosto {@code face}), da cor do elemento. */
  public static void decal(ServerLevel level, Entity source, Vec3 hit, Direction face, SpellElement e, float scale) {
    if (budget(level, source == null ? null : source.getUUID(), 1) == 0) return;
    var base = options(e, Role.DECAL, scale, -1);
    var opts = new ElementFxOptions(base.type(), base.element(), base.role(), scale, -1, face.get3DDataValue());
    Vec3 at = hit.add(face.getStepX() * 0.02, face.getStepY() * 0.02, face.getStepZ() * 0.02);
    level.sendParticles(opts, at.x, at.y, at.z, 1, 0, 0, 0, 0);
  }

  /** Reacao elemental com visual proprio: vapor (fogo em molhado), estilhaco (gelo quebrado), chiado (fogo apagado). */
  public static void reaction(ServerLevel level, Entity source, Vec3 at, Reaction kind) {
    switch (kind) {
      case STEAM -> {
        burst(level, source, at, SpellElement.WATER, Role.REACTION, 14, 0.4, 0.02, 1.6f, 0xE8F4FF);
        sound(level, at, new ResourceLocation("arcane", "reaction.steam"), 0.7f, 1f);
      }
      case SHATTER -> {
        burst(level, source, at, SpellElement.FROST, Role.IMPACT, 18, 0.3, 0.25, 1.2f, 0xDFF6FF);
        sound(level, at, new ResourceLocation("arcane", "reaction.shatter"), 0.8f, 1f);
      }
      case HISS -> {
        burst(level, source, at, SpellElement.ARCANE, Role.REACTION, 8, 0.25, 0.015, 1.3f, 0x8A8A8A);
        sound(level, at, new ResourceLocation("arcane", "reaction.steam"), 0.5f, 1.3f);
      }
    }
  }

  // ------------------------------------------------------------------ composicao (conjuracao e impacto)
  /** Conjuracao: espiral e faiscas do elemento 1 em volta de quem conjura, e o som de conjuracao. */
  public static void cast(ServerLevel level, Entity caster, SpellElement body, float power) {
    cast(level, caster, body, power, -1);
  }

  /** Como {@link #cast(ServerLevel, Entity, SpellElement, float)}, com a cor sobrescrita ({@code color} -1 = a do elemento). */
  public static void cast(ServerLevel level, Entity caster, SpellElement body, float power, int color) {
    Vec3 base = caster.position();
    double h = caster.getBbHeight();
    int want = 10 + Math.min(14, (int) (power * 4));
    int n = budget(level, caster.getUUID(), want);
    var opts = options(body, Role.BODY, 1f, color);
    for (int i = 0; i < n; i++) {
      double f = i / (double) Math.max(1, n);
      double a = f * Math.PI * 4;
      double r = 0.7 - f * 0.25;
      level.sendParticles(opts, base.x + Math.cos(a) * r, base.y + f * (h + 0.4), base.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0.01);
    }
    sound(level, base, body, SoundRole.CAST, power);
  }

  /**
   * Impacto: estouro do elemento 2 (o rastro e o impacto sao dele), anel curto do elemento 1 e marca no bloco, se houver.
   * {@code power} e a forca relativa (1 = normal) e mexe em quantidade, volume e tom.
   */
  public static void impact(ServerLevel level, Entity source, Vec3 at, Direction face, SpellElement body, SpellElement trail, float power) {
    float p = Mth.clamp(power, 0.25f, 4f);
    burst(level, source, at, trail, Role.IMPACT, (int) (16 + 10 * p), 0.25, 0.12 + 0.04 * p);
    if (body != trail) burst(level, source, at, body, Role.BODY, (int) (6 + 4 * p), 0.35, 0.05);
    if (face != null) decal(level, source, at, face, trail, 0.8f + 0.3f * Math.min(2f, p));
    sound(level, at, trail, SoundRole.IMPACT, p);
  }

  // ------------------------------------------------------------------ sons
  /** Som do elemento. Potencia maior: mais alto e mais grave. */
  public static void sound(ServerLevel level, Vec3 at, SpellElement e, SoundRole role, float power) {
    var v = e.visual();
    String id = role == SoundRole.CAST ? v.castSound() : v.impactSound();
    ResourceLocation rl = ResourceLocation.tryParse(id);
    if (rl == null) return;
    float p = Mth.clamp(power, 0.25f, 4f);
    float volume = (role == SoundRole.CAST ? 0.5f : 0.65f) * (0.75f + 0.25f * (float) Math.sqrt(p));
    float pitch = Mth.clamp(1.0f / (0.85f + 0.15f * p), 0.6f, 1.4f) * (0.95f + level.random.nextFloat() * 0.1f);
    sound(level, at, rl, volume, pitch);
  }

  public static void sound(ServerLevel level, Vec3 at, ResourceLocation id, float volume, float pitch) {
    SoundEvent event = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(id);
    if (event == null) event = SoundEvent.createVariableRangeEvent(id);
    level.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch);
  }
}
