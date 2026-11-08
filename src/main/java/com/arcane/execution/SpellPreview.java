package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionCompiler;
import com.arcane.inscription.InscriptionValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Previa de uma inscricao: descreve em linhas o que ela faz (forma, formato, alvos, efeitos) e AVISA das combinacoes
 * que nao fazem nada, em vez de ignora-las em silencio. Funcao pura sobre o plano compilado; o servidor a calcula e a
 * bancada mostra.
 */
public final class SpellPreview {
  private SpellPreview() {}

  private static String path(ResourceLocation id) { return id.getPath(); }

  private static Component name(ResourceLocation id) { return Component.translatable("glyph.arcane." + id.getPath()); }

  public static Component reachText(int mask) {
    List<Component> parts = new ArrayList<>();
    if ((mask & EffectReach.ENTITY) != 0) parts.add(Component.translatable("gui.arcane.preview.reach.entity"));
    if ((mask & EffectReach.BLOCK) != 0) parts.add(Component.translatable("gui.arcane.preview.reach.block"));
    if ((mask & EffectReach.POINT) != 0) parts.add(Component.translatable("gui.arcane.preview.reach.point"));
    if (parts.isEmpty()) return Component.translatable("gui.arcane.preview.reach.none");
    var out = Component.empty();
    for (int i = 0; i < parts.size(); i++) {
      if (i > 0) out.append(", ");
      out.append(parts.get(i));
    }
    return out;
  }

  public static List<Component> describe(Inscription inscription) {
    List<Component> lines = new ArrayList<>();
    var validation = InscriptionValidator.validate(inscription);
    if (!validation.valid()) {
      lines.add(Component.translatable("gui.arcane.preview.invalid", validation.message()).withStyle(ChatFormatting.RED));
      return lines;
    }
    CompiledExecutionPlan plan = InscriptionCompiler.compile(inscription);
    String form = path(plan.delivery().formId());
    int reach = EffectReach.ofForm(form);
    lines.add(Component.translatable("gui.arcane.preview.form", name(plan.delivery().formId()), reachText(reach)).withStyle(ChatFormatting.AQUA));

    var params = plan.delivery().parameters();
    boolean customRegion = params.containsKey("shape") || params.containsKey("origin");
    if (customRegion && EffectReach.usesRegion(form)) {
      int shape = ShapeGeometry.shapeOf(params.getOrDefault("shape", 0.0));
      String shapeKey = shape == ShapeGeometry.DEFAULT ? "default" : String.valueOf(shape);
      lines.add(Component.translatable("gui.arcane.preview.shape." + shapeKey,
          origin(params.getOrDefault("origin", 0.0))));
    } else if (customRegion) {
      lines.add(Component.translatable("gui.arcane.preview.warn.region_ignored", name(plan.delivery().formId())).withStyle(ChatFormatting.GOLD));
    }

    List<Component> warnings = new ArrayList<>();
    for (CompiledExecutionPlan.ExecutionSegment segment : plan.segments()) {
      int mask = reach;
      for (var filter : segment.filters()) mask = EffectReach.afterFilter(path(filter.filterId()), mask);
      segment.condition().ifPresent(c -> lines.add(Component.translatable("gui.arcane.preview.condition", name(c.conditionId())).withStyle(ChatFormatting.GRAY)));
      for (var effect : segment.effects()) {
        int acts = EffectReach.ofEffect(path(effect.glyphId()));
        int hit = acts & mask;
        if (EffectReach.independent(path(effect.glyphId()))) {
          lines.add(Component.translatable("gui.arcane.preview.effect.free", name(effect.glyphId())));
        } else if (hit == 0) {
          warnings.add(Component.translatable("gui.arcane.preview.warn.noop", name(effect.glyphId()), reachText(mask), reachText(acts)).withStyle(ChatFormatting.GOLD));
        } else {
          lines.add(Component.translatable("gui.arcane.preview.effect", name(effect.glyphId()), reachText(hit)));
        }
      }
    }
    lines.addAll(warnings);
    return lines;
  }

  private static Component origin(double v) {
    String key = v >= 0.5 ? "aim" : v <= -0.5 ? "self" : "default";
    return Component.translatable("gui.arcane.preview.origin." + key);
  }
}
