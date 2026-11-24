package com.arcane.world;

import com.arcane.config.ArcaneConfig;
import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ruinas Arcanas: masmorra subterranea de 4 a 8 salas numa grade 3x3, ligadas por corredores. Cada sala tem um tema e
 * o seu loot (data/arcane/loot_tables/chests/ruins/):
 * <ul>
 *   <li>Salao do Ritual (o centro): altar com um circulo INCOMPLETO e runas entalhadas nas paredes (cena 7 do design);</li>
 *   <li>Biblioteca: estantes, atril e notas de glifo;</li>
 *   <li>Laboratorio de Alquimia: alambiques, caldeirao, pocoes de mana e tintas;</li>
 *   <li>Arsenal: suportes com armadura, bigorna e armas encantadas;</li>
 *   <li>Cripta: gerador de mortos-vivos, teias e ossos;</li>
 *   <li>Gruta de Cristal: cristais de mana e ametista (sem bau);</li>
 *   <li>Cofre (a sala mais longe do centro): dois baus ricos, um mago guardiao e uma armadilha de runa invisivel na entrada.</li>
 * </ul>
 * Aparecem ao gerar chunks novos no Overworld (chance em ruinsChance) e pelo comando /arcane ruins.
 */
public class ArcaneRuins {
  public static final int CELL = 14, HALF = 4, HEIGHT = 6;
  public enum Room { RITUAL, LIBRARY, ALCHEMY, ARMORY, CRYPT, GROTTO, VAULT }

  /** Lido na hora (os blocos so existem depois do registro; um campo estatico quebraria o carregamento do mod). */
  private static BlockState brick() { return ModBlocks.ARCANE_BRICKS.get().defaultBlockState(); }

  @SubscribeEvent
  public void onChunkLoad(ChunkEvent.Load event) {
    if (!(event.getLevel() instanceof ServerLevel level) || !event.isNewChunk() || level.dimension() != Level.OVERWORLD) return;
    double chance = ArcaneConfig.RUINS_CHANCE.get();
    if (chance <= 0) return;
    var cp = event.getChunk().getPos();
    RandomSource rand = RandomSource.create(level.getSeed() ^ (cp.toLong() * 0x2545F4914F6CDD1DL) ^ 0xA2C4E5);
    if (rand.nextDouble() >= chance) return;
    int y = -20 + rand.nextInt(40);
    BlockPos centre = new BlockPos(cp.getMinBlockX() + 8, y, cp.getMinBlockZ() + 8);
    long seed = rand.nextLong();
    level.getServer().tell(new TickTask(level.getServer().getTickCount() + 40, () -> {
      int reach = CELL + HALF + 2;
      for (BlockPos corner : List.of(centre.offset(-reach, 0, -reach), centre.offset(reach, 0, reach), centre.offset(-reach, 0, reach), centre.offset(reach, 0, -reach))) {
        if (!level.isLoaded(corner)) return;
      }
      if (!level.getBlockState(centre).isSolid() || !level.getBlockState(centre.above(3)).isSolid()) return;
      build(level, centre, RandomSource.create(seed));
    }));
  }

  /** Constroi as ruinas com o piso da sala central em {@code centre}. Devolve o mapa sala -> centro (para testes). */
  public static Map<BlockPos, Room> build(ServerLevel level, BlockPos centre, RandomSource rand) {
    // 1) escolhe as salas: arvore aleatoria a partir do centro da grade
    int target = 4 + rand.nextInt(5);
    List<int[]> cells = new ArrayList<>();
    List<int[][]> links = new ArrayList<>();
    boolean[][] used = new boolean[3][3];
    used[1][1] = true;
    cells.add(new int[] {1, 1});
    int guard = 0;
    while (cells.size() < target && guard++ < 200) {
      int[] from = cells.get(rand.nextInt(cells.size()));
      Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
      int nx = from[0] + d.getStepX(), nz = from[1] + d.getStepZ();
      if (nx < 0 || nz < 0 || nx > 2 || nz > 2 || used[nx][nz]) continue;
      used[nx][nz] = true;
      cells.add(new int[] {nx, nz});
      links.add(new int[][] {from, {nx, nz}});
    }
    // 2) o cofre e a sala mais longe do centro (em passos pela arvore)
    Map<String, Integer> depth = new HashMap<>();
    depth.put("1,1", 0);
    ArrayDeque<int[]> queue = new ArrayDeque<>();
    queue.add(new int[] {1, 1});
    while (!queue.isEmpty()) {
      int[] c = queue.poll();
      for (int[][] l : links) {
        for (int k = 0; k < 2; k++) {
          int[] a = l[k], b = l[1 - k];
          if (a[0] == c[0] && a[1] == c[1] && !depth.containsKey(b[0] + "," + b[1])) {
            depth.put(b[0] + "," + b[1], depth.get(c[0] + "," + c[1]) + 1);
            queue.add(b);
          }
        }
      }
    }
    int[] vault = cells.get(0);
    for (int[] c : cells) if (depth.getOrDefault(c[0] + "," + c[1], 0) > depth.getOrDefault(vault[0] + "," + vault[1], 0)) vault = c;
    Room[] pool = {Room.LIBRARY, Room.ALCHEMY, Room.ARMORY, Room.CRYPT, Room.GROTTO};
    Map<BlockPos, Room> rooms = new HashMap<>();
    // 3) corredores primeiro, salas por cima
    for (int[][] l : links) corridor(level, at(centre, l[0]), at(centre, l[1]), rand);
    for (int[] c : cells) {
      Room type = (c[0] == 1 && c[1] == 1) ? Room.RITUAL : (c == vault && cells.size() > 1) ? Room.VAULT : pool[rand.nextInt(pool.length)];
      BlockPos p = at(centre, c);
      shell(level, p, rand);
      decorate(level, p, type, rand);
      rooms.put(p, type);
    }
    // 4) as portas: abre a parede de cada sala onde o corredor chega
    for (int[][] l : links) corridor(level, at(centre, l[0]), at(centre, l[1]), null);
    return rooms;
  }

  private static BlockPos at(BlockPos centre, int[] cell) {
    return centre.offset((cell[0] - 1) * CELL, 0, (cell[1] - 1) * CELL);
  }

  private static void set(ServerLevel level, BlockPos p, BlockState s) { level.setBlock(p, s, 2); }

  /** Corredor de 3 de largura e 4 de altura entre dois centros alinhados; {@code rand == null} so abre o vao. */
  private static void corridor(ServerLevel level, BlockPos a, BlockPos b, RandomSource rand) {
    int dx = Integer.signum(b.getX() - a.getX()), dz = Integer.signum(b.getZ() - a.getZ());
    int len = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
    for (int i = 0; i <= len; i++) {
      // na segunda passada (rand nulo) so abre o vao das paredes das duas salas, sem mexer no que esta dentro delas
      if (rand == null && i > HALF + 1 && i < len - HALF - 1) continue;
      if (rand == null && (i < HALF || i > len - HALF)) continue;
      BlockPos c = a.offset(dx * i, 0, dz * i);
      for (int s = -2; s <= 2; s++) {
        for (int y = -1; y <= 4; y++) {
          BlockPos p = c.offset(dz != 0 ? s : 0, y, dx != 0 ? s : 0);
          boolean inside = Math.abs(s) <= 1 && y >= 0 && y <= 3;
          if (inside) set(level, p, Blocks.AIR.defaultBlockState());
          else if (rand != null) set(level, p, y == -1 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
              : rand.nextInt(6) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : brick());
        }
      }
      if (rand != null && i % 6 == 3) set(level, c.offset(0, 3, 0), ModBlocks.RUNE_LAMP.get().defaultBlockState());
    }
  }

  private static void shell(ServerLevel level, BlockPos c, RandomSource rand) {
    int s = HALF + 1;
    for (int dx = -s; dx <= s; dx++) for (int dz = -s; dz <= s; dz++) for (int dy = -1; dy <= HEIGHT; dy++) {
      BlockPos p = c.offset(dx, dy, dz);
      boolean wall = Math.abs(dx) == s || Math.abs(dz) == s;
      if (dy == -1) set(level, p, (dx + dz) % 2 == 0 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
      else if (dy == HEIGHT || wall) set(level, p, rand.nextInt(7) == 0 ? ModBlocks.CHISELED_ARCANE_BRICKS.get().defaultBlockState()
          : rand.nextInt(5) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : brick());
      else set(level, p, Blocks.AIR.defaultBlockState());
    }
    for (int sx : new int[] {-HALF, HALF}) for (int sz : new int[] {-HALF, HALF}) {
      for (int dy = 0; dy < HEIGHT; dy++) set(level, c.offset(sx, dy, sz), dy == HEIGHT - 2 ? ModBlocks.RUNE_LAMP.get().defaultBlockState() : Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
    }
  }

  private static void chest(ServerLevel level, BlockPos p, String table, Direction facing, RandomSource rand) {
    level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
    if (level.getBlockEntity(p) instanceof ChestBlockEntity be) be.setLootTable(new ResourceLocation("arcane", "chests/ruins/" + table), rand.nextLong());
  }

  private static void decorate(ServerLevel level, BlockPos c, Room type, RandomSource rand) {
    switch (type) {
      case RITUAL -> {
        level.setBlock(c, ModBlocks.ARCANE_ALTAR.get().defaultBlockState(), 3);
        com.arcane.ritual.RitualService.drawCircle(level, c, 4);
        // o circulo esta incompleto: um terco dos tracos se apagou com o tempo
        var trace = ModBlocks.ARCANE_TRACE.get();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-HALF, 0, -HALF), c.offset(HALF, 0, HALF))) {
          if (level.getBlockState(p).is(trace) && rand.nextInt(3) == 0) set(level, p.immutable(), Blocks.AIR.defaultBlockState());
        }
        for (int i = 0; i < 6; i++) {
          BlockPos w = c.offset(rand.nextBoolean() ? -HALF - 1 : HALF + 1, 1 + rand.nextInt(3), -3 + rand.nextInt(7));
          set(level, w, ModBlocks.CHISELED_ARCANE_BRICKS.get().defaultBlockState());
        }
        chest(level, c.offset(0, 0, HALF - 1), "ritual", Direction.NORTH, rand);
      }
      case LIBRARY -> {
        for (int dx = -HALF + 1; dx <= HALF - 1; dx++) for (int dy = 0; dy < 3; dy++) {
          if (Math.abs(dx) <= 1) continue;
          set(level, c.offset(dx, dy, -HALF), Blocks.BOOKSHELF.defaultBlockState());
          set(level, c.offset(dx, dy, HALF), Blocks.BOOKSHELF.defaultBlockState());
        }
        set(level, c, Blocks.LECTERN.defaultBlockState());
        set(level, c.offset(2, 0, 0), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        chest(level, c.offset(-HALF + 1, 0, 0), "library", Direction.EAST, rand);
        if (rand.nextBoolean()) chest(level, c.offset(HALF - 1, 0, 0), "library", Direction.WEST, rand);
      }
      case ALCHEMY -> {
        set(level, c.offset(-2, 0, -2), Blocks.BREWING_STAND.defaultBlockState());
        set(level, c.offset(2, 0, -2), Blocks.BREWING_STAND.defaultBlockState());
        set(level, c, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        for (int dx = -1; dx <= 1; dx++) set(level, c.offset(dx, 0, HALF - 1), Blocks.BARREL.defaultBlockState());
        chest(level, c.offset(-HALF + 1, 0, 2), "alchemy", Direction.EAST, rand);
      }
      case ARMORY -> {
        set(level, c.offset(0, 0, -HALF + 1), Blocks.ANVIL.defaultBlockState());
        for (int i = -1; i <= 1; i += 2) {
          var stand = EntityType.ARMOR_STAND.create(level);
          if (stand == null) continue;
          stand.moveTo(c.getX() + 0.5 + i * 2.5, c.getY(), c.getZ() + 0.5, 180f, 0);
          stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(rand.nextBoolean() ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET));
          stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(rand.nextBoolean() ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
          level.addFreshEntity(stand);
        }
        chest(level, c.offset(0, 0, HALF - 1), "armory", Direction.NORTH, rand);
      }
      case CRYPT -> {
        level.setBlock(c, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(c) instanceof SpawnerBlockEntity sp) sp.setEntityId(rand.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON, rand);
        for (int i = 0; i < 8; i++) set(level, c.offset(-HALF + rand.nextInt(2 * HALF + 1), rand.nextInt(HEIGHT - 1), -HALF + rand.nextInt(2 * HALF + 1)), Blocks.COBWEB.defaultBlockState());
        set(level, c, Blocks.SPAWNER.defaultBlockState()); // as teias nao cobrem o gerador
        if (level.getBlockEntity(c) instanceof SpawnerBlockEntity sp) sp.setEntityId(rand.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON, rand);
        set(level, c.offset(2, 0, 2), Blocks.SOUL_LANTERN.defaultBlockState());
        chest(level, c.offset(-HALF + 1, 0, -HALF + 1), "crypt", Direction.SOUTH, rand);
      }
      case GROTTO -> {
        for (int i = 0; i < 4; i++) set(level, c.offset(-3 + rand.nextInt(7), 0, -3 + rand.nextInt(7)), ModBlocks.MANA_CRYSTAL.get().defaultBlockState());
        for (int i = 0; i < 10; i++) set(level, c.offset(-HALF + rand.nextInt(2 * HALF + 1), 0, -HALF + rand.nextInt(2 * HALF + 1)), Blocks.AMETHYST_CLUSTER.defaultBlockState());
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
          if (rand.nextInt(3) == 0) set(level, c.offset(dx, -1, dz), Blocks.CALCITE.defaultBlockState());
        }
      }
      case VAULT -> {
        for (int dx = -1; dx <= 1; dx += 2) chest(level, c.offset(dx * 2, 0, HALF - 1), "vault", Direction.NORTH, rand);
        set(level, c.offset(-HALF + 1, 0, HALF - 1), Blocks.GOLD_BLOCK.defaultBlockState());
        set(level, c.offset(HALF - 1, 0, HALF - 1), Blocks.GOLD_BLOCK.defaultBlockState());
        var guard = com.arcane.registry.ModEntities.NPC_MAGE.get().create(level);
        if (guard != null) {
          guard.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, rand.nextFloat() * 360f, 0);
          guard.setPersistenceRequired();
          level.addFreshEntity(guard);
        }
        // armadilha: runa de tinta invisivel no chao do meio da sala (Ao Chegar Perto -> lentidao e dano)
        var trap = new com.arcane.inscription.Inscription(List.of(
            new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", "self")),
            new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", "on_near")),
            new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", "slow")),
            new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", "damage"), Map.of("amount", 4.0))));
        BlockPos floor = c.offset(0, -1, -2);
        com.arcane.inscribing.WorldInscriptionIndex.get(level).put(new com.arcane.inscribing.InscriptionAnchor(floor, trap,
            com.arcane.inscribing.InscribingService.TRIGGER_ON_NEAR, new ResourceLocation("arcane", "invisible_perm"), Direction.UP));
      }
    }
  }
}
