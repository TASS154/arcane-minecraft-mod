package com.arcane.execution.handlers;

import com.arcane.api.form.FormHandler;
import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Region;
import com.arcane.execution.Seek;
import com.arcane.execution.ShapeGeometry;
import com.arcane.execution.TargetList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Coluna / Pilar ("eu afirmo um eixo Y no mundo"). Nao e um Raio vertical nem uma Aura estreita: marca um lugar no CHAO
 * (o ponto mirado assentado no chao, ou o chao debaixo da criatura mirada) e o efeito percorre o fuste inteiro.
 * <ul>
 *   <li>Criaturas: TODAS dentro da coluna, em qualquer altura (do chao ate o limite do mundo).</li>
 *   <li>Blocos: o terreno do fuste (blocos expostos dentro da coluna, do chao para cima), nao o bloco da mira.</li>
 *   <li>Ponto: a marca no chao (efeitos de ponto agem ali).</li>
 *   <li>Sem chao: nao se manifesta. Quem conjura direto falha antes de cobrar mana, como o Foco sem alvo.</li>
 * </ul>
 * Segurar = pilar fixo (a ancora nao segue a mira); Zona = tempestade no eixo. Com Mais Proximo logo depois da forma,
 * a coluna assenta no chao debaixo da criatura mais proxima.
 */
public class PillarFormHandler implements FormHandler {
  /** Ate onde a busca de blocos do fuste sobe (o fuste visual vai ate o ceu; os blocos param aqui). */
  public static final int SHAFT_BLOCK_HEIGHT = 32;

  public static double radiusFor(ExecutionContext context, DeliverySpec spec) {
    return Math.max(1.0, Math.min(context.ritualRadius() > 0 || context.areaFactor() > 1.0 ? ExecutionContext.cap(64.0) : ExecutionContext.cap(24.0), context.coverRadius(spec.parameters().getOrDefault("radius", 2.0) * context.areaFactor())));
  }

  /** A criatura sob a qual a coluna assenta quando o Mais Proximo vira entrega (null = ninguem: falha). */
  public static LivingEntity seekTarget(ExecutionContext context) {
    return Seek.nearest(context, com.arcane.config.ArcaneConfig.RAY_RANGE.get(), true);
  }

  @Override
  public DeliveryResult deliver(ExecutionContext context, DeliverySpec spec) {
    if (Seek.on(spec)) {
      LivingEntity target = seekTarget(context);
      if (target == null) return DeliveryResult.stop(TargetList.empty());
      return Seek.aimedAt(context, target, () -> manifest(context, spec));
    }
    return manifest(context, spec);
  }

  private static DeliveryResult manifest(ExecutionContext context, DeliverySpec spec) {
    double radius = radiusFor(context, spec);
    if (!Region.mayManifest(context, spec, radius)) {
      return DeliveryResult.stop(TargetList.empty()); // a coluna nao se manifesta no ar
    }
    Region region = Region.of(context, spec, ShapeGeometry.COLUMN, radius);
    List<Entity> entities = new ArrayList<>(region.entities(context.level(), context.caster(), false));
    region.playFx(context);
    List<TargetList.BlockTarget> blocks = region.shape() == ShapeGeometry.COLUMN
        ? region.columnBlocks(context.level(), SHAFT_BLOCK_HEIGHT, 1500)
        : region.exposedBlocks(context.level(), 1500);
    return DeliveryResult.of(new TargetList(entities, blocks, List.of(region.center())));
  }
}
