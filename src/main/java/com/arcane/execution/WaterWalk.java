package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Andar na agua. Roda nos dois lados (o movimento do jogador e do cliente): na superficie, sem agachar, a agua sustenta
 * como chao. Agachar deixa afundar; mergulhado, sobe ate a tona.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class WaterWalk {
  private WaterWalk() {}

  @SubscribeEvent
  public static void onTick(LivingEvent.LivingTickEvent event) {
    LivingEntity e = event.getEntity();
    if (!e.hasEffect(ModEffects.WATER_WALK.get()) || e.isShiftKeyDown() || e.isSpectator()) return;
    var level = e.level();
    BlockPos feet = BlockPos.containing(e.getX(), e.getY() - 0.05, e.getZ());
    var fluid = level.getFluidState(feet);
    if (!fluid.is(FluidTags.WATER)) return;
    double surface = feet.getY() + fluid.getHeight(level, feet);
    boolean waterAbove = level.getFluidState(feet.above()).is(FluidTags.WATER);
    Vec3 v = e.getDeltaMovement();
    if (waterAbove) {
      e.setDeltaMovement(v.x, Math.max(v.y, 0.18), v.z); // fundo: boia para cima
    } else if (e.getY() < surface + 0.02) {
      e.setPos(e.getX(), surface, e.getZ());
      e.setDeltaMovement(v.x, Math.max(0.0, v.y), v.z);
      e.setOnGround(true);
      e.fallDistance = 0f;
    }
  }
}
