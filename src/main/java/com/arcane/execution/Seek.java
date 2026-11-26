package com.arcane.execution;

import com.arcane.glyph.GlyphCategory;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.GlyphInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Regra de ordem do Mais Proximo (prompt de 26 nov, A1).
 *
 * <ul>
 *   <li>{@code Forma -> Mais Proximo} (o PRIMEIRO filtro logo depois da forma; modificadores de entrega no meio, como
 *       Area, Alcance ou Domo, nao contam): o Mais Proximo muda a ENTREGA. A forma mira sozinha a criatura viva mais
 *       proxima de quem conjura, no alcance e com as regras da forma (Raio, Toque e Foco nao atravessam parede; o Pilar
 *       assenta no chao debaixo dela; o Projetil sai na direcao dela; Aura e Onda ficam so com ela).</li>
 *   <li>{@code Forma -> outro filtro -> Mais Proximo}, ou depois de um efeito ou condicao: continua o filtro de sempre
 *       ({@link com.arcane.execution.handlers.NearestFilter}): entre os alvos ja coletados, fica o mais perto.</li>
 * </ul>
 *
 * O compilador grava {@link #KEY} nos parametros da forma e tira o Mais Proximo da lista de filtros (ele virou a
 * entrega). Formas que nao miram (Eu, Barreira, Invocar) mantem o filtro e a previa avisa.
 */
public final class Seek {
  public static final String KEY = "seek_nearest";
  public static final String NEAREST = "nearest";
  /** Formas em que o Mais Proximo logo depois muda a entrega. */
  public static final Set<String> FORMS = Set.of("ray", "touch", "focus", "pillar", "projectile", "aura", "wave");
  /** Alcance da busca do Projetil (o orbe nao tem alcance proprio). */
  public static final double PROJECTILE_RANGE = 32.0;
  /** Teto da busca: o Raio "sem limite" (rayRange, 512) e o Pilar nao varrem o mundo inteiro atras de criaturas. */
  public static final double MAX_RANGE = 48.0;

  private Seek() {}

  public static boolean on(Map<String, Double> formParams) {
    return formParams.getOrDefault(KEY, 0.0) > 0.0;
  }

  public static boolean on(DeliverySpec spec) {
    return on(spec.parameters());
  }

  /**
   * Indice do glifo Mais Proximo que vem como primeiro filtro logo depois da forma (so modificadores no meio), ou -1.
   * Puro: o compilador e a previa usam a mesma regra.
   */
  public static int firstAfterForm(List<GlyphInstance> glyphs) {
    boolean afterForm = false;
    for (int i = 0; i < glyphs.size(); i++) {
      var def = GlyphRegistry.get(glyphs.get(i).glyphId()).orElse(null);
      if (def == null) return -1;
      GlyphCategory cat = def.category();
      if (cat == GlyphCategory.FORM) { afterForm = true; continue; }
      if (!afterForm) continue;
      if (cat == GlyphCategory.MODIFIER) continue;
      if (cat == GlyphCategory.FILTER && NEAREST.equals(glyphs.get(i).glyphId().getPath())) return i;
      return -1; // outro filtro, efeito, condicao ou ativador: o Mais Proximo depois disso e so filtro
    }
    return -1;
  }

  /** O que a previa diz sobre o Mais Proximo desta magia. */
  public enum Mode { NONE, DELIVERY, FILTER, IGNORED_BY_FORM }

  public static Mode mode(List<GlyphInstance> glyphs, String formPath) {
    boolean any = glyphs.stream().anyMatch(g -> NEAREST.equals(g.glyphId().getPath()));
    if (!any) return Mode.NONE;
    if (firstAfterForm(glyphs) < 0) return Mode.FILTER;
    return FORMS.contains(formPath) ? Mode.DELIVERY : Mode.IGNORED_BY_FORM;
  }

  /**
   * A criatura viva mais proxima de quem conjura, a ate {@code range} blocos. Com {@code lineOfSight}, so conta quem
   * a vista alcanca a partir dos olhos (as formas que nao atravessam parede). Null se nao ha ninguem.
   */
  public static LivingEntity nearest(ExecutionContext ctx, double wanted, boolean lineOfSight) {
    double range = Math.min(wanted, MAX_RANGE);
    LivingEntity caster = ctx.caster();
    Vec3 from = ctx.origin();
    Vec3 me = ctx.position();
    List<LivingEntity> found = ctx.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range),
        e -> e != caster && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && e.distanceToSqr(me) <= range * range);
    found.sort(Comparator.comparingDouble((Entity e) -> e.distanceToSqr(me)));
    for (LivingEntity e : found) {
      if (!lineOfSight || visible(ctx, from, e)) return e;
    }
    return null;
  }

  /** Mira dos olhos ao centro da criatura sem bloco no meio. */
  public static boolean visible(ExecutionContext ctx, Vec3 from, Entity target) {
    Vec3 to = aimPoint(target);
    var hit = ctx.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster()));
    return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(from) >= to.distanceToSqr(from) - 1.0e-3;
  }

  public static Vec3 aimPoint(Entity target) {
    return target.getBoundingBox().getCenter();
  }

  /** So a criatura mais perto de quem conjura entre as que o volume pegou (Aura, Onda). */
  public static TargetList closestOf(ExecutionContext ctx, List<Entity> found) {
    Vec3 me = ctx.position();
    return found.stream().min(Comparator.comparingDouble((Entity e) -> e.distanceToSqr(me)))
        .map(TargetList::ofEntities).orElse(TargetList.empty());
  }

  /** Roda a entrega da forma com o olhar virado para o alvo e devolve o olhar original depois. */
  public static <T> T aimedAt(ExecutionContext ctx, Entity target, Supplier<T> body) {
    Vec3 look = ctx.look();
    Vec3 dir = aimPoint(target).subtract(ctx.origin());
    if (dir.lengthSqr() < 1.0e-6) return body.get();
    ctx.setLook(dir.normalize());
    try {
      return body.get();
    } finally {
      ctx.setLook(look);
    }
  }
}
