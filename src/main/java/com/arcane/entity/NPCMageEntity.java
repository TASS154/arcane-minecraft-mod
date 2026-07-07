package com.arcane.entity;

import com.arcane.execution.CastingService;
import com.arcane.inscription.Inscription;
import com.arcane.npc.NPCArcaneComponent;
import com.arcane.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NPCMageEntity extends Monster {
  private final NPCArcaneComponent arcane = new NPCArcaneComponent();

  public NPCMageEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Zombie.createAttributes();
  }

  @Override
  public void tick() {
    super.tick();
    if (!level().isClientSide && tickCount % 80 == 0) {
      Player target = level().getNearestPlayer(this, 12.0);
      if (target != null) {
        Inscription inscription = arcane.selectSpell();
        CastingService.tryCast(this, inscription, (ServerLevel) level());
      }
    }
  }

  @Override
  protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
    super.dropCustomDeathLoot(source, looting, recentlyHit);
    // Grimório drops as glyph note with encoded knowledge
    if (!level().isClientSide) {
      spawnAtLocation(com.arcane.registry.ModItems.GLYPH_NOTE.get());
    }
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0, false));
    this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.8));
    this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, true));
  }
}
