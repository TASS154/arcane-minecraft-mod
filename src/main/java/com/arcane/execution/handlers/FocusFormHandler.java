package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Foco: forma que entrega SO a criatura viva sob a mira (nao blocos, nem pontos vazios, nem efeitos proprios). Existe para
 * efeitos que so fazem sentido com uma criatura (Atrair, Marcar, Elo...). Sem criatura na mira a conjuracao falha ANTES de
 * cobrar mana (ver {@link #hasFocus} e {@link #find}, usados por CastingService).
 */
public class FocusFormHandler implements FormHandler {
  public static final ResourceLocation ID = new ResourceLocation("arcane", "focus");

  public static boolean hasFocus(Inscription inscription) {
    for (GlyphInstance g : inscription.glyphs()) if (ID.equals(g.glyphId())) return true;
    return false;
  }

  public static double rangeOf(Inscription inscription) {
    for (GlyphInstance g : inscription.glyphs()) if (ID.equals(g.glyphId())) return Math.max(4.0, Math.min(48.0, g.param("range", 24.0)));
    return 24.0;
  }

  /** A criatura viva sob a mira de quem conjura (nao atravessa paredes), ou null. */
  public static LivingEntity find(LivingEntity caster, Vec3 start, Vec3 look, double range) {
    if (!(caster.level() instanceof ServerLevel level)) return null;
    Vec3 end = start.add(look.scale(range));
    var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
    Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, caster, start, limit, new AABB(start, limit).inflate(1.0),
        e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator() && e.isAlive() && e != caster);
    return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
  }

  /**
   * A criatura que o Foco trava nesta conjuracao: a da mira ou, com Mais Proximo logo depois da forma, a criatura viva
   * mais proxima de quem conjura (sem atravessar parede). Null = nenhuma (a conjuracao falha antes de cobrar mana).
   */
  public static LivingEntity lock(ExecutionContext context, DeliverySpec spec) {
    double range = Math.max(4.0, Math.min(48.0, spec.range() > 0 ? spec.range() : spec.parameters().getOrDefault("range", 24.0)));
    if (com.arcane.execution.Seek.on(spec)) return com.arcane.execution.Seek.nearest(context, range, true);
    return find(context.caster(), context.origin(), context.look(), range);
  }

  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    LivingEntity target = lock(context, spec);
    return DeliveryResult.of(target == null ? new TargetList(java.util.List.of(), java.util.List.of()) : TargetList.ofEntities(target));
  }
}
