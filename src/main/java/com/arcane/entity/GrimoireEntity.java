package com.arcane.entity;

import com.arcane.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class GrimoireEntity extends Entity {
  private UUID ownerId;
  private static final EntityDataAccessor<Integer> STAGE =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.INT);

  /** Dono sincronizado: o cliente calcula sozinho a posicao suavizada do livro em volta dele. */
  private static final EntityDataAccessor<java.util.Optional<UUID>> OWNER =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  /** Tick de jogo ate o qual o livro esta folheando (so folheia quando o dono escolhe entre as magias). */
  private static final EntityDataAccessor<Long> FLIP_UNTIL =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.LONG);
  public static final int FLIP_TICKS = 16;

  /** Bancada onde o livro esta pousado (o cliente o desenha parado, aberto sobre ela). */
  private static final EntityDataAccessor<java.util.Optional<net.minecraft.core.BlockPos>> ANCHOR =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
  /** Ate quando a pena escreve (o cliente desenha a pena e a tinta ate esse tick). */
  private static final EntityDataAccessor<Long> WRITING_UNTIL =
      SynchedEntityData.defineId(GrimoireEntity.class, EntityDataSerializers.LONG);
  /** Livro de estudo: pousado sem dono presente (outro mago estudando um grimorio alheio). */
  private boolean study;

  public java.util.Optional<net.minecraft.core.BlockPos> anchor() { return this.entityData.get(ANCHOR); }
  public long writingUntil() { return this.entityData.get(WRITING_UNTIL); }
  public void setAnchor(net.minecraft.core.BlockPos pos) {
    this.entityData.set(ANCHOR, java.util.Optional.ofNullable(pos));
    if (pos != null) setPos(pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5);
  }
  public void setWritingUntil(long tick) { this.entityData.set(WRITING_UNTIL, tick); }
  public void markStudy(net.minecraft.core.BlockPos pos, int stage) {
    this.study = true;
    this.entityData.set(STAGE, Math.max(1, stage));
    setAnchor(pos);
  }
  public boolean isStudy() { return study; }

  public long flipUntil() { return this.entityData.get(FLIP_UNTIL); }
  public void flip() { this.entityData.set(FLIP_UNTIL, level().getGameTime() + FLIP_TICKS); }

  public GrimoireEntity(EntityType<? extends GrimoireEntity> type, Level level) {
    super(type, level);
    this.noPhysics = true;
    this.noCulling = true;
  }

  public java.util.Optional<UUID> ownerUuid() { return this.entityData.get(OWNER); }

  public void bind(Player player, int stage) {
    this.ownerId = player.getUUID();
    this.entityData.set(OWNER, java.util.Optional.of(ownerId));
    this.entityData.set(STAGE, Math.max(1, stage));
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) {
      int stage = visualStage();
      if (stage >= 3 && tickCount % (stage >= 6 ? 2 : 6) == 0) {
        double a = tickCount * 0.2;
        level().addParticle(com.arcane.registry.ModParticles.ARCANE_SPARK.get(),
            getX() + Math.cos(a) * 0.35, getY() + 0.3 + random.nextDouble() * 0.3, getZ() + Math.sin(a) * 0.35, 0.0, 0.01, 0.0);
      }
    }
    if (!level().isClientSide && study) {
      if (anchor().isEmpty() || tickCount % 20 == 0 && !com.arcane.grimoire.GrimoireDesk.studyAt(level(), anchor().get())) discard();
      return;
    }
    if (!level().isClientSide && ownerId == null && tickCount > 40) {
      discard(); // grimorio sem dono (sobra de teste ou dado antigo)
      return;
    }
    if (!level().isClientSide && ownerId != null) {
      Player owner = level().getPlayerByUUID(ownerId);
      if (anchor().isPresent()) {
        if (owner == null) discard(); // dono saiu do jogo: volta a aparecer na bancada quando ele entrar
        else setAnchor(anchor().get());
        return;
      }
      if (owner == null || !owner.isAlive()) {
        discard();
        return;
      }
      setPos(owner.getX() + 1.2, owner.getY() + 1.0, owner.getZ() + 0.6);
    }
  }

  public int visualStage() { return this.entityData.get(STAGE); }

  @Override
  protected void defineSynchedData() {
    this.entityData.define(STAGE, 1);
    this.entityData.define(OWNER, java.util.Optional.empty());
    this.entityData.define(FLIP_UNTIL, 0L);
    this.entityData.define(ANCHOR, java.util.Optional.empty());
    this.entityData.define(WRITING_UNTIL, 0L);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (tag.hasUUID("owner")) { ownerId = tag.getUUID("owner"); this.entityData.set(OWNER, java.util.Optional.of(ownerId)); }
    this.entityData.set(STAGE, Math.max(1, tag.getInt("stage")));
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    if (ownerId != null) tag.putUUID("owner", ownerId);
    tag.putInt("stage", visualStage());
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return NetworkHooks.getEntitySpawningPacket(this);
  }
}
