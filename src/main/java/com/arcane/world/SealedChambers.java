package com.arcane.world;

import com.arcane.block.ArcaneAltarBlock;
import com.arcane.config.ArcaneConfig;
import com.arcane.registry.ModBlocks;
import com.arcane.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Camaras seladas: de vez em quando, ao gerar um chunk no Overworld, uma camara de tijolos de ardosia aparece fundo
 * debaixo da terra. No centro, um Altar Arcano dentro de um circulo de ritual segura uma criatura SELADA (sem IA,
 * invulneravel e brilhando) e um bau guarda o que sobrou do ritual. A criatura so e solta quando o altar e quebrado
 * (por um jogador ou por uma explosao); ai ela acorda, forte e hostil.
 */
public class SealedChambers {
  public static final String SEALED_TAG = "arcane_sealed_altar";
  /** Metade do lado do interior (9x9) e altura do interior. */
  private static final int HALF = 4;
  private static final int HEIGHT = 5;

  // ------------------------------------------------------------------ geracao
  @SubscribeEvent
  public void onChunkLoad(ChunkEvent.Load event) {
    if (!(event.getLevel() instanceof ServerLevel level) || !event.isNewChunk()) return;
    if (level.dimension() != Level.OVERWORLD) return;
    double chance = ArcaneConfig.SEALED_CHAMBER_CHANCE.get();
    if (chance <= 0) return;
    ChunkAccess chunk = event.getChunk();
    RandomSource rand = RandomSource.create(level.getSeed() ^ (chunk.getPos().toLong() * 0x9E3779B97F4A7C15L) ^ 0x5EA1ED);
    if (rand.nextDouble() >= chance) return;
    int minY = level.getMinBuildHeight() + 8;
    int y = minY + rand.nextInt(Math.max(1, 40 - minY + 10));
    BlockPos center = new BlockPos(chunk.getPos().getMinBlockX() + 8, y, chunk.getPos().getMinBlockZ() + 8);
    final BlockPos spot = center;
    final long seed = rand.nextLong();
    // Nada de ler blocos aqui (o chunk ainda nao esta no mapa e isso trava o servidor): confere e constroi
    // alguns ticks depois, com o chunk completamente carregado.
    level.getServer().tell(new TickTask(level.getServer().getTickCount() + 20, () -> {
      if (level.isLoaded(spot) && mostlySolid(level, spot)) build(level, spot, RandomSource.create(seed));
    }));
  }

  private static boolean mostlySolid(ServerLevel level, BlockPos c) {
    int solid = 0, total = 0;
    for (int dx = -HALF - 1; dx <= HALF + 1; dx += 2) {
      for (int dz = -HALF - 1; dz <= HALF + 1; dz += 2) {
        for (int dy = 0; dy <= HEIGHT; dy += 2) {
          BlockPos p = c.offset(dx, dy, dz);
          if (!level.isLoaded(p)) return false;
          BlockState s = level.getBlockState(p);
          total++;
          if (s.isSolid() && s.getFluidState().isEmpty()) solid++;
        }
      }
    }
    return solid >= total * 0.8;
  }

  /**
   * Constroi a camara com o piso em `floor` (centro). Retorna o altar. Usado pela geracao e pelo comando /arcane chamber.
   */
  public static BlockPos build(ServerLevel level, BlockPos floor, RandomSource rand) {
    BlockState bricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    BlockState cracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    BlockState polished = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    int s = HALF + 1;
    for (int dx = -s; dx <= s; dx++) {
      for (int dz = -s; dz <= s; dz++) {
        for (int dy = -1; dy <= HEIGHT; dy++) {
          BlockPos p = floor.offset(dx, dy, dz);
          boolean shell = Math.abs(dx) == s || Math.abs(dz) == s || dy == -1 || dy == HEIGHT;
          if (shell) {
            level.setBlock(p, dy == -1 ? polished : (rand.nextInt(5) == 0 ? cracked : bricks), 2);
          } else {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
          }
        }
      }
    }
    // pilares nos cantos e luz fraca de almas
    for (int sx : new int[]{-HALF, HALF}) {
      for (int sz : new int[]{-HALF, HALF}) {
        for (int dy = 0; dy < HEIGHT; dy++) level.setBlock(floor.offset(sx, dy, sz), dy == 1 ? Blocks.SOUL_LANTERN.defaultBlockState() : Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), 2);
      }
    }
    BlockPos altar = floor;
    level.setBlock(altar, ModBlocks.ARCANE_ALTAR.get().defaultBlockState(), 3);
    com.arcane.ritual.RitualService.drawCircle(level, altar, 4);

    // bau no canto, com o que sobrou do ritual
    BlockPos chest = floor.offset(HALF, 0, -HALF + 1);
    level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 3);
    if (level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
      be.setItem(0, new ItemStack(ModItems.COMMON_INK.get(), 4 + rand.nextInt(8)));
      be.setItem(1, new ItemStack(ModItems.MANA_CRYSTAL.get(), 1 + rand.nextInt(3)));
      be.setItem(2, new ItemStack(ModItems.ARCANE_PAGE.get(), 1));
      be.setItem(3, new ItemStack(ModItems.GLYPH_NOTE.get(), 1 + rand.nextInt(2)));
      if (rand.nextInt(3) == 0) be.setItem(4, new ItemStack(ModItems.BLOOD_INK.get(), 2));
      if (rand.nextInt(4) == 0) be.setItem(5, new ItemStack(Items.DIAMOND, 1 + rand.nextInt(3)));
    }
    spawnSealed(level, altar, rand);
    return altar;
  }

  private static final List<EntityType<? extends Mob>> POOL = List.of(
      EntityType.WITHER_SKELETON, EntityType.PIGLIN_BRUTE, EntityType.VINDICATOR, EntityType.RAVAGER, EntityType.ZOMBIE);

  /** A criatura selada: de pe sobre o altar, sem IA, invulneravel e brilhando ate o selo (altar) ser quebrado. */
  public static Mob spawnSealed(ServerLevel level, BlockPos altar, RandomSource rand) {
    EntityType<? extends Mob> type = POOL.get(rand.nextInt(POOL.size()));
    Mob mob = type.create(level);
    if (mob == null) return null;
    mob.moveTo(altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, rand.nextFloat() * 360f, 0f);
    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(altar), MobSpawnType.STRUCTURE, null, null);
    var hp = mob.getAttribute(Attributes.MAX_HEALTH);
    if (hp != null) { hp.setBaseValue(hp.getBaseValue() * 2.0); mob.setHealth((float) hp.getBaseValue()); }
    mob.setCustomName(Component.translatable("entity.arcane.sealed_creature", type.getDescription()));
    mob.setCustomNameVisible(true);
    mob.setPersistenceRequired();
    mob.setNoAi(true);
    mob.setInvulnerable(true);
    mob.setGlowingTag(true);
    mob.getPersistentData().putLong(SEALED_TAG, altar.asLong());
    level.addFreshEntity(mob);
    return mob;
  }

  // ------------------------------------------------------------------ quebrar o selo
  public static int free(ServerLevel level, BlockPos altar) {
    int freed = 0;
    for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(altar).inflate(12))) {
      CompoundTag data = mob.getPersistentData();
      if (!data.contains(SEALED_TAG) || data.getLong(SEALED_TAG) != altar.asLong()) continue;
      data.remove(SEALED_TAG);
      mob.setNoAi(false);
      mob.setInvulnerable(false);
      mob.setGlowingTag(false);
      mob.setCustomNameVisible(false);
      level.playSound(null, altar, net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 0.7f, 0.6f);
      level.sendParticles(com.arcane.registry.ModParticles.ARCANE_SHOCK.get(), mob.getX(), mob.getY() + 1, mob.getZ(), 1, 0, 0, 0, 0);
      var player = level.getNearestPlayer(mob, 24);
      if (player != null && mob instanceof Monster m && level.getDifficulty() != Difficulty.PEACEFUL) m.setTarget(player);
      freed++;
    }
    return freed;
  }

  @SubscribeEvent
  public void onBreak(BlockEvent.BreakEvent event) {
    if (event.getLevel() instanceof ServerLevel level && event.getState().getBlock() instanceof ArcaneAltarBlock) {
      free(level, event.getPos());
    }
  }

  @SubscribeEvent
  public void onExplosion(ExplosionEvent.Detonate event) {
    if (!(event.getLevel() instanceof ServerLevel level)) return;
    for (BlockPos p : event.getAffectedBlocks()) {
      if (level.getBlockState(p).getBlock() instanceof ArcaneAltarBlock) free(level, p);
    }
  }
}
