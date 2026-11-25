package com.arcane.world;

import com.arcane.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Masmorra procedural em volta de um covil de Hidra: 5 a 10 salas ligadas por corredores tortos, tudo sorteado pela
 * posicao do covil (o mesmo mundo gera sempre a mesma masmorra). As salas ficam longe da camara (a mais de 52 blocos
 * do centro) para a cupula, que cresce quando a Hidra e revelada, nao engolir nada; a rede comeca no pe do poco de
 * entrada e um ramo desce ate a camara.
 *
 * <p>Salas: guarita (gerador de esqueletos), tesouro (baus da faixa baixa da Hidra), salao de armadilhas, santuario
 * (altar com circulo quebrado e notas de glifo), salao alagado, fosso de ossos e cofre de cristais. Pelos corredores,
 * runas invisiveis no chao (Ao Chegar Perto): fogo, lancamento para cima, cegueira e lentidao, dano ou empurrao.
 */
public final class LairDungeon {
  public enum Room { GUARD, TREASURY, TRAPS, SHRINE, FLOODED, BONES, CRYSTALS }

  private static final int HALF = 4, HEIGHT = 5;

  private LairDungeon() {}

  private static BlockState wall(RandomSource r) {
    int k = r.nextInt(10);
    return k == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
        : k == 1 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
        : k < 4 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
  }

  private static void set(ServerLevel level, BlockPos p, BlockState s) { level.setBlock(p, s, 2); }

  /**
   * Gera a masmorra. {@code chamber} = centro do piso da camara; {@code entrance} = pe do poco de entrada.
   * Devolve o tipo de cada sala (para testes).
   */
  public static Map<BlockPos, Room> build(ServerLevel level, BlockPos chamber, BlockPos entrance) {
    // dois sorteios: a planta (posicoes e tipos) so depende do covil; a decoracao usa outro, para a planta ser sempre a mesma
    RandomSource deco = RandomSource.create(chamber.asLong() ^ 0x5DEECE66DL);
    Map<BlockPos, Room> types = new java.util.LinkedHashMap<>();
    for (Step step : plan(chamber, entrance)) {
      corridor(level, step.from(), step.at(), step.bend(), deco);
      room(level, step.at(), step.type(), deco);
      types.put(step.at(), step.type());
    }
    // ramo ate a camara: um corredor que desce da entrada ate a parede da cupula (a camara abre por ele tambem)
    corridor(level, entrance, chamber.offset(HydraLairs.CHAMBER_R + 1, 0, 0), true, deco);
    return types;
  }

  /** Uma sala da planta: de onde sai o corredor, onde fica a sala, o tipo e o lado da dobra do corredor. */
  public record Step(BlockPos from, BlockPos at, Room type, boolean bend) {}

  /** Salas sorteadas (5 a 10) e tentativas por sala antes de desistir de um rumo. */
  static final int MIN_ROOMS = 5, MAX_ROOMS = 10, TRIES_PER_ROOM = 20, ATTEMPTS_PER_ROOM = 8;

  /**
   * A planta da masmorra, sem tocar no mundo (so depende da posicao do covil). Cada sala sai de uma sala ja posta (ou da
   * entrada), num rumo sorteado, a 14-22 blocos, a mais de 52 blocos da camara e sem dividir a celula de 12x12 com outra.
   *
   * <p>Antes, uma sala que nao achava lugar em 20 tentativas era simplesmente pulada; perto da entrada (a 38 blocos da
   * camara) a maioria dos rumos cai dentro do raio proibido, e alguns covis saiam com 1 sala so. Agora a sala pulada e
   * sorteada de novo a partir de outra base, ate {@code ATTEMPTS_PER_ROOM} vezes por sala.
   */
  public static List<Step> plan(BlockPos chamber, BlockPos entrance) {
    RandomSource r = RandomSource.create(chamber.asLong() * 0x9E3779B97F4A7C15L ^ 0x4C41495244L);
    int rooms = MIN_ROOMS + r.nextInt(MAX_ROOMS - MIN_ROOMS + 1);
    List<Step> steps = new ArrayList<>();
    List<BlockPos> placed = new ArrayList<>();
    Set<Long> used = new HashSet<>();
    Room[] pool = Room.values();
    for (int attempt = 0; placed.size() < rooms && attempt < rooms * ATTEMPTS_PER_ROOM; attempt++) {
      BlockPos base = placed.isEmpty() || r.nextInt(3) == 0 ? entrance : placed.get(r.nextInt(placed.size()));
      BlockPos next = null;
      for (int tries = 0; tries < TRIES_PER_ROOM && next == null; tries++) {
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int dist = 14 + r.nextInt(9);
        BlockPos cand = base.relative(d, dist).offset(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
        double fromChamber = Math.hypot(cand.getX() - chamber.getX(), cand.getZ() - chamber.getZ());
        long key = (((long) Math.floorDiv(cand.getX(), 12)) << 32) ^ (Math.floorDiv(cand.getZ(), 12) & 0xffffffffL);
        if (fromChamber < 52 || used.contains(key)) continue;
        used.add(key);
        next = cand;
      }
      if (next == null) continue;
      int i = placed.size();
      Room type = i == 0 ? Room.GUARD : i == rooms - 1 ? Room.TREASURY : pool[r.nextInt(pool.length)];
      steps.add(new Step(base, next, type, r.nextBoolean()));
      placed.add(next);
    }
    return steps;
  }

  /** Corredor de 3x4 em L com uma dobra no meio; armadilhas de runa sorteadas no chao. */
  private static void corridor(ServerLevel level, BlockPos a, BlockPos b, boolean xFirst, RandomSource r) {
    BlockPos bend = xFirst ? new BlockPos(b.getX(), a.getY(), a.getZ()) : new BlockPos(a.getX(), a.getY(), b.getZ());
    straight(level, a, bend, r);
    straight(level, bend, b, r);
  }

  private static void straight(ServerLevel level, BlockPos a, BlockPos b, RandomSource r) {
    int dx = Integer.signum(b.getX() - a.getX()), dz = Integer.signum(b.getZ() - a.getZ());
    int len = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
    for (int i = 0; i <= len; i++) {
      BlockPos c = a.offset(dx * i, 0, dz * i);
      for (int s = -2; s <= 2; s++) for (int y = -1; y <= 4; y++) {
        BlockPos p = c.offset(dz != 0 ? s : 0, y, dx != 0 ? s : 0);
        boolean inside = Math.abs(s) <= 1 && y >= 0 && y <= 3;
        if (inside) set(level, p, Blocks.CAVE_AIR.defaultBlockState());
        else if (!level.getBlockState(p).isAir()) set(level, p, y == -1 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : wall(r));
      }
      if (i % 7 == 3) set(level, c.offset(0, 3, 0), Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
      if (i > 4 && i < len - 4 && r.nextInt(14) == 0) trap(level, c.below(), r);
    }
  }

  /** Runa invisivel permanente no chao: dispara em quem chega perto (uma vez). */
  public static void trap(ServerLevel level, BlockPos floor, RandomSource r) {
    String[][] kinds = {
        {"self", "on_near", "fire", "damage"},
        {"self", "on_near", "levitate"},
        {"self", "on_near", "blind", "slow"},
        {"self", "on_near", "damage"},
        {"self", "on_near", "push"},
        {"self", "on_near", "poison", "slow"}};
    String[] k = kinds[r.nextInt(kinds.length)];
    List<com.arcane.inscription.GlyphInstance> glyphs = new ArrayList<>();
    for (String g : k) {
      glyphs.add(g.equals("damage") ? new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", g), Map.of("amount", 5.0))
          : new com.arcane.inscription.GlyphInstance(new ResourceLocation("arcane", g)));
    }
    com.arcane.inscribing.WorldInscriptionIndex.get(level).put(new com.arcane.inscribing.InscriptionAnchor(floor,
        new com.arcane.inscription.Inscription(glyphs), com.arcane.inscribing.InscribingService.TRIGGER_ON_NEAR,
        new ResourceLocation("arcane", "invisible_perm"), Direction.UP));
  }

  private static void chest(ServerLevel level, BlockPos p, String table, RandomSource r) {
    set(level, p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(r)));
    if (level.getBlockEntity(p) instanceof ChestBlockEntity be) be.setLootTable(new ResourceLocation("arcane", table), r.nextLong());
  }

  private static void room(ServerLevel level, BlockPos c, Room type, RandomSource r) {
    int hx = HALF + r.nextInt(3), hz = HALF + r.nextInt(3), h = HEIGHT + r.nextInt(3);
    for (int dx = -hx - 1; dx <= hx + 1; dx++) for (int dz = -hz - 1; dz <= hz + 1; dz++) for (int dy = -1; dy <= h; dy++) {
      BlockPos p = c.offset(dx, dy, dz);
      boolean shell = Math.abs(dx) == hx + 1 || Math.abs(dz) == hz + 1 || dy == -1 || dy == h;
      set(level, p, shell ? (dy == -1 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : wall(r)) : Blocks.CAVE_AIR.defaultBlockState());
    }
    for (int sx : new int[] {-hx, hx}) for (int sz : new int[] {-hz, hz}) {
      for (int dy = 0; dy < h; dy++) set(level, c.offset(sx, dy, sz), dy == h - 2 ? Blocks.SOUL_LANTERN.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
    }
    switch (type) {
      case GUARD -> {
        set(level, c, Blocks.SPAWNER.defaultBlockState());
        if (level.getBlockEntity(c) instanceof SpawnerBlockEntity sp) sp.setEntityId(r.nextBoolean() ? EntityType.SKELETON : EntityType.WITHER_SKELETON, r);
        chest(level, c.offset(hx - 1, 0, hz - 1), "chests/ruins/armory", r);
      }
      case TREASURY -> {
        int n = 2 + r.nextInt(3);
        for (int k = 0; k < n; k++) chest(level, c.offset(-hx + 1 + k * 2, 0, hz - 1), "chests/hydra/low", r);
        set(level, c, Blocks.RAW_GOLD_BLOCK.defaultBlockState());
        for (int k = 0; k < 3; k++) trap(level, c.offset(r.nextInt(2 * hx - 1) - hx + 1, -1, r.nextInt(2 * hz - 1) - hz + 1), r);
      }
      case TRAPS -> {
        for (int dx = -hx + 1; dx <= hx - 1; dx += 2) for (int dz = -hz + 1; dz <= hz - 1; dz += 2) if (r.nextInt(3) == 0) trap(level, c.offset(dx, -1, dz), r);
        chest(level, c.offset(0, 0, hz - 1), "chests/ruins/vault", r);
      }
      case SHRINE -> {
        level.setBlock(c, ModBlocks.ARCANE_ALTAR.get().defaultBlockState(), 3);
        com.arcane.ritual.RitualService.drawCircle(level, c, 4);
        var trace = ModBlocks.ARCANE_TRACE.get();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-HALF, 0, -HALF), c.offset(HALF, 0, HALF))) {
          if (level.getBlockState(p).is(trace) && r.nextInt(3) == 0) set(level, p.immutable(), Blocks.CAVE_AIR.defaultBlockState());
        }
        chest(level, c.offset(-hx + 1, 0, 0), "chests/ruins/ritual", r);
      }
      case FLOODED -> {
        for (int dx = -hx; dx <= hx; dx++) for (int dz = -hz; dz <= hz; dz++) {
          set(level, c.offset(dx, 0, dz), Blocks.WATER.defaultBlockState());
          if (r.nextInt(9) == 0) set(level, c.offset(dx, h - 1, dz), Blocks.POINTED_DRIPSTONE.defaultBlockState()
              .setValue(net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN));
        }
        chest(level, c.offset(0, -1, 0), "chests/ruins/alchemy", r); // no fundo d'agua
      }
      case BONES -> {
        for (int k = 0; k < 14; k++) set(level, c.offset(r.nextInt(2 * hx + 1) - hx, 0, r.nextInt(2 * hz + 1) - hz), Blocks.BONE_BLOCK.defaultBlockState());
        for (int k = 0; k < 10; k++) set(level, c.offset(r.nextInt(2 * hx + 1) - hx, 1 + r.nextInt(h - 2), r.nextInt(2 * hz + 1) - hz), Blocks.COBWEB.defaultBlockState());
        chest(level, c, "chests/ruins/crypt", r);
      }
      case CRYSTALS -> {
        for (int k = 0; k < 3; k++) set(level, c.offset(r.nextInt(2 * hx - 1) - hx + 1, 0, r.nextInt(2 * hz - 1) - hz + 1), ModBlocks.MANA_CRYSTAL.get().defaultBlockState());
        for (int k = 0; k < 12; k++) set(level, c.offset(r.nextInt(2 * hx + 1) - hx, 0, r.nextInt(2 * hz + 1) - hz), Blocks.AMETHYST_CLUSTER.defaultBlockState());
      }
    }
  }
}
