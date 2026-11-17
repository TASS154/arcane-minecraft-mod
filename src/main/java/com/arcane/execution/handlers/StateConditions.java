package com.arcane.execution.handlers;

import com.arcane.api.condition.ConditionHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.inscription.GlyphInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Condicoes de estado da Fase 3 (todas lidas na hora de conjurar, sem custo por tick): Vida do Alvo Abaixo de X,
 * Bioma ou Dimensao, Segurando Item X e Chuva ou Tempestade. Os parametros numericos escolhem a variante.
 */
public final class StateConditions {
  private StateConditions() {}

  /** Nomes das variantes de Bioma/Dimensao (indice = parametro "kind"). */
  public static final String[] REGIONS = {"", "overworld", "nether", "end", "hot", "cold", "forest", "ocean", "swamp"};
  /** Nomes das variantes de Segurando (indice = parametro "kind"). */
  public static final String[] ITEMS = {"empty", "weapon", "tool", "ranged", "food", "block", "potion", "any"};

  /** Alguma criatura alvo esta com vida abaixo de X% (percent). Sem alvo criatura, nao passa. */
  public static class TargetHealth implements ConditionHandler {
    @Override
    public boolean test(ExecutionContext context, GlyphInstance instance) {
      double percent = instance.parameters().getOrDefault("percent", 30.0);
      for (Entity e : context.resolvedTargets().entities()) {
        if (e instanceof LivingEntity living && living.isAlive() && living.getHealth() < living.getMaxHealth() * percent / 100.0) return true;
      }
      return false;
    }
  }

  public static class Region implements ConditionHandler {
    @Override
    public boolean test(ExecutionContext context, GlyphInstance instance) {
      return inRegion(context.level(), context.caster().blockPosition(), (int) Math.round(instance.parameters().getOrDefault("kind", 1.0)));
    }
  }

  public static boolean inRegion(net.minecraft.server.level.ServerLevel level, BlockPos pos, int kind) {
    var biome = level.getBiome(pos);
    return switch (kind) {
      case 1 -> level.dimension() == Level.OVERWORLD;
      case 2 -> level.dimension() == Level.NETHER;
      case 3 -> level.dimension() == Level.END;
      case 4 -> biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_BADLANDS) || isDesert(biome) || biome.value().getBaseTemperature() >= 1.5f;
      case 5 -> biome.value().getBaseTemperature() <= 0.15f && !biome.is(BiomeTags.IS_NETHER);
      case 6 -> biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA);
      case 7 -> biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
      case 8 -> biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
      default -> false;
    };
  }

  private static boolean isDesert(net.minecraft.core.Holder<Biome> biome) {
    return biome.is(Biomes.DESERT);
  }

  public static class Holding implements ConditionHandler {
    @Override
    public boolean test(ExecutionContext context, GlyphInstance instance) {
      return holds(context.caster().getMainHandItem(), (int) Math.round(instance.parameters().getOrDefault("kind", 1.0)));
    }
  }

  public static boolean holds(ItemStack stack, int kind) {
    return switch (kind) {
      case 0 -> stack.isEmpty();
      case 1 -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
      case 2 -> stack.getItem() instanceof DiggerItem;
      case 3 -> stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem;
      case 4 -> stack.isEdible();
      case 5 -> stack.getItem() instanceof BlockItem;
      case 6 -> stack.getItem() instanceof PotionItem;
      case 7 -> !stack.isEmpty();
      default -> false;
    };
  }

  /** Chuva ou tempestade no mundo onde se conjura (storm=1: so tempestade). */
  public static class Raining implements ConditionHandler {
    @Override
    public boolean test(ExecutionContext context, GlyphInstance instance) {
      boolean stormOnly = instance.parameters().getOrDefault("storm", 0.0) >= 0.5;
      var level = context.level();
      if (stormOnly) return level.isThundering();
      return level.isRaining() || level.isThundering();
    }
  }
}
