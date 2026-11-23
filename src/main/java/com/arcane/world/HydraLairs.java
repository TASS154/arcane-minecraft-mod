package com.arcane.world;

import com.arcane.ArcaneMod;
import com.arcane.entity.HydraEntity;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GlyphKnowledge;
import com.arcane.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Os 7 covis das Hidras. Cada covil e uma masmorra subterranea (entrada na superficie, poco, corredor com salas e
 * baus, e a camara da Hidra no fim, uma cupula selada). O estagio da Hidra NAO e definido quando o covil e gerado:
 * ele so e sorteado quando alguem chega a 1 bloco da parede da camara (ou a ve de dentro, como um espectador).
 * O primeiro covil encontrado no mundo tem sempre a recem-nascida (1), o setimo sempre a anciã (14); os do meio
 * sorteiam estagios entre 2 e 13 sem repetir. Cada covil guarda um glifo original, ensinado a quem derrota a Hidra.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class HydraLairs extends SavedData {
  public static final int COUNT = 7;
  /** Raio interno da camara (a cupula). A parede fica em CHAMBER_R e CHAMBER_R + 1. */
  public static final int CHAMBER_R = 14;
  /** Glifo original guardado por cada covil (pela ordem dos covis). */
  public static final String[] ORIGINALS = {"time_stop", "rewind", "space_collapse", "annihilate", "eclipse", "genesis", "gravity_inversion"};
  private static final String NAME = "arcane_hydra_lairs";

  public static final class Lair {
    public final int index;
    public final BlockPos centre;
    public boolean built;
    /** 0 = ainda nao revelado. */
    public int stage;
    public int revealOrder;
    public boolean defeated;

    Lair(int index, BlockPos centre) {
      this.index = index;
      this.centre = centre;
    }

    public String original() { return ORIGINALS[index]; }
  }

  private final List<Lair> lairs = new ArrayList<>();
  private boolean placed;

  public static HydraLairs get(ServerLevel overworld) {
    return overworld.getDataStorage().computeIfAbsent(HydraLairs::load, HydraLairs::new, NAME);
  }

  private static HydraLairs load(CompoundTag tag) {
    HydraLairs h = new HydraLairs();
    h.placed = tag.getBoolean("placed");
    for (Tag t : tag.getList("lairs", Tag.TAG_COMPOUND)) {
      CompoundTag c = (CompoundTag) t;
      Lair l = new Lair(c.getInt("i"), BlockPos.of(c.getLong("c")));
      l.built = c.getBoolean("b");
      l.stage = c.getInt("s");
      l.revealOrder = c.getInt("o");
      l.defeated = c.getBoolean("d");
      h.lairs.add(l);
    }
    return h;
  }

  @Override
  public CompoundTag save(CompoundTag tag) {
    tag.putBoolean("placed", placed);
    ListTag list = new ListTag();
    for (Lair l : lairs) {
      CompoundTag c = new CompoundTag();
      c.putInt("i", l.index);
      c.putLong("c", l.centre.asLong());
      c.putBoolean("b", l.built);
      c.putInt("s", l.stage);
      c.putInt("o", l.revealOrder);
      c.putBoolean("d", l.defeated);
      list.add(c);
    }
    tag.put("lairs", list);
    return tag;
  }

  public List<Lair> lairs() { return lairs; }

  /** Onde ficam os 7 covis: em volta da origem, a cada 360/7 graus, de 700 a 2800 blocos (fixo pela semente). */
  public void placeIfNeeded(ServerLevel level) {
    if (placed) return;
    RandomSource r = RandomSource.create(level.getSeed() ^ 0x4859445241L);
    double turn = r.nextDouble() * Math.PI * 2;
    List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6));
    for (int k = 0; k < COUNT; k++) {
      double a = turn + k * Math.PI * 2 / COUNT;
      int dist = 700 + order.get(k) * 350;
      int x = (int) Math.round(Math.cos(a) * dist), z = (int) Math.round(Math.sin(a) * dist);
      lairs.add(new Lair(k, new BlockPos(x, 0, z)));
    }
    placed = true;
    setDirty();
  }

  /** Para testes e comandos: poe um covil num lugar exato. */
  public Lair addAt(BlockPos centre) {
    placed = true;
    Lair l = new Lair(lairs.size() % COUNT, centre);
    lairs.add(l);
    setDirty();
    return l;
  }

  public int revealedCount() {
    return (int) lairs.stream().filter(l -> l.stage > 0).count();
  }

  /**
   * Estagio do proximo covil revelado: o 1o e 1, o 7o e 14, os outros sorteiam entre 2 e 13 os que ainda nao sairam.
   */
  public static int nextStage(List<Integer> used, int revealedSoFar, RandomSource random) {
    if (revealedSoFar <= 0) return 1;
    if (revealedSoFar >= COUNT - 1) return HydraEntity.MAX_STAGE;
    List<Integer> pool = new ArrayList<>();
    for (int s = 2; s <= 13; s++) if (!used.contains(s)) pool.add(s);
    if (pool.isEmpty()) return 2 + random.nextInt(12);
    return pool.get(random.nextInt(pool.size()));
  }

  public void reveal(ServerLevel level, Lair lair) {
    if (lair.stage > 0) return;
    List<Integer> used = lairs.stream().filter(l -> l.stage > 0).map(l -> l.stage).toList();
    int n = revealedCount();
    lair.stage = nextStage(used, n, level.random);
    lair.revealOrder = n + 1;
    setDirty();
    HydraEntity hydra = ModEntities.HYDRA.get().create(level);
    if (hydra == null) return;
    BlockPos at = lair.centre;
    hydra.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
    hydra.setStage(lair.stage);
    hydra.setLairIndex(lair.index);
    hydra.homeTo(lair.centre, CHAMBER_R - 2);
    hydra.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
    hydra.setStage(lair.stage);
    // o ritual: um selo no alto de cada pilar escolhido; a Hidra dorme ate o ultimo quebrar
    int mask = HydraEntity.sealMaskFor(lair.stage);
    for (int i = 0; i < HydraEntity.PILLARS; i++) {
      BlockPos top = lair.centre.offset(HydraEntity.pillarTop(i));
      if ((mask & (1 << i)) != 0) level.setBlock(top, com.arcane.registry.ModBlocks.HYDRA_SEAL.get().defaultBlockState(), 3);
    }
    hydra.setSealMask(sealMaskAt(level, lair.centre));
    level.addFreshEntity(hydra);
    level.playSound(null, at, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0f, 1.8f - lair.stage * 0.09f);
    for (ServerPlayer p : level.players()) {
      if (p.distanceToSqr(Vec3.atCenterOf(at)) < 96 * 96) {
        p.displayClientMessage(Component.translatable("message.arcane.hydra.awake", Component.translatable("hydra.arcane.stage." + lair.stage), lair.stage,
            Integer.bitCount(hydra.sealMask())).withStyle(ChatFormatting.DARK_PURPLE), false);
      }
    }
  }

  /** Quais pilares da camara ainda tem selo (um bit por pilar). */
  public static int sealMaskAt(ServerLevel level, BlockPos centre) {
    int mask = 0;
    var seal = com.arcane.registry.ModBlocks.HYDRA_SEAL.get();
    for (int i = 0; i < HydraEntity.PILLARS; i++) if (level.getBlockState(centre.offset(HydraEntity.pillarTop(i))).is(seal)) mask |= 1 << i;
    return mask;
  }

  /** Um selo foi quebrado: atualiza a Hidra da camara (zerar a desperta). */
  public static void onSealBroken(ServerLevel level, BlockPos pos) {
    for (HydraEntity h : level.getEntitiesOfClass(HydraEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(CHAMBER_R * 2.5))) {
      if (!h.sealed()) continue;
      BlockPos home = h.home();
      if (home.distSqr(pos) > (CHAMBER_R + 2) * (CHAMBER_R + 2)) continue;
      int mask = sealMaskAt(level, home);
      com.arcane.network.ModNetwork.sendShape(level, com.arcane.network.packet.S2CShapeFxPacket.ORIGINAL, Vec3.atCenterOf(pos),
          h.position().add(0, h.getBbHeight() * 0.5, 0), 1f, 0xFF3040, com.arcane.network.packet.S2CShapeFxPacket.FX_SEAL_BREAK, 24);
      level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 3f, 0.5f);
      level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 3f, 0.6f);
      if (mask != 0) {
        for (ServerPlayer p : level.players()) {
          if (p.distanceToSqr(Vec3.atCenterOf(pos)) < 48 * 48) {
            p.displayClientMessage(Component.translatable("message.arcane.hydra.seal_broken", Integer.bitCount(mask)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
          }
        }
      }
      h.setSealMask(mask);
    }
  }

  /** Alguem chegou a 1 bloco da parede da camara, ou um espectador a ve. */
  static boolean seesChamber(ServerPlayer p, Lair lair) {
    Vec3 c = Vec3.atCenterOf(lair.centre).add(0, 3, 0);
    double d = p.position().distanceTo(c);
    if (d <= CHAMBER_R + 2.5) return true;
    if (p.isSpectator() && d <= 64) {
      Vec3 look = p.getLookAngle();
      return c.subtract(p.getEyePosition()).normalize().dot(look) > 0.8;
    }
    if (d <= 48) {
      // Vista por uma abertura (a porta da camara aberta, por exemplo): linha de visao livre ate o centro.
      var hit = p.level().clip(new ClipContext(p.getEyePosition(), c, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
      return hit.getType() == HitResult.Type.MISS;
    }
    return false;
  }

  // ------------------------------------------------------------------ eventos
  @SubscribeEvent
  public static void onTick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
    if (level.getGameTime() % 10 != 0 || level.players().isEmpty()) return;
    HydraLairs data = get(level);
    data.placeIfNeeded(level);
    for (Lair lair : data.lairs) {
      if (!lair.built || lair.stage > 0) continue;
      for (ServerPlayer p : level.players()) {
        if (seesChamber(p, lair)) {
          data.reveal(level, lair);
          break;
        }
      }
    }
  }

  @SubscribeEvent
  public static void onChunkLoad(ChunkEvent.Load event) {
    if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
    HydraLairs data = get(level);
    data.placeIfNeeded(level);
    var cp = event.getChunk().getPos();
    for (Lair lair : data.lairs) {
      if (lair.built || (lair.centre.getX() >> 4) != cp.x || (lair.centre.getZ() >> 4) != cp.z) continue;
      lair.built = true; // marca ja: evita construir duas vezes se o chunk recarregar antes da tarefa rodar
      data.setDirty();
      level.getServer().tell(new TickTask(level.getServer().getTickCount() + 2, () -> build(level, data, lair)));
    }
  }

  public static void onHydraDeath(ServerLevel level, HydraEntity hydra) {
    ServerLevel overworld = level.getServer().overworld();
    HydraLairs data = get(overworld);
    String glyph = ORIGINALS[Math.floorMod(hydra.lairIndex() < 0 ? hydra.stage() : hydra.lairIndex(), COUNT)];
    if (hydra.lairIndex() >= 0) {
      for (Lair l : data.lairs) if (l.index == hydra.lairIndex() && l.stage == hydra.stage()) { l.defeated = true; data.setDirty(); }
    }
    ResourceLocation id = new ResourceLocation("arcane", glyph);
    for (ServerPlayer p : level.players()) {
      if (!hydra.fighters().contains(p.getUUID()) && p.distanceToSqr(hydra) > 64 * 64) continue;
      GrimoireAttachments.getOrCreate(p).learnGlyph(id, GlyphKnowledge.MASTERED);
      com.arcane.network.ArcaneSync.sync(p, true);
      p.displayClientMessage(Component.translatable("message.arcane.hydra.original", Component.translatable("glyph.arcane." + glyph)).withStyle(ChatFormatting.GOLD), false);
      level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
    }
  }

  // ------------------------------------------------------------------ construcao
  private static void set(ServerLevel level, BlockPos pos, BlockState state) {
    if (level.getBlockState(pos).getDestroySpeed(level, pos) < 0 && !level.getBlockState(pos).isAir()) return; // nao mexe em rocha-mae
    level.setBlock(pos, state, 2);
  }

  /** Constroi o covil inteiro (chamado uma vez, quando o chunk do centro carrega). */
  public static void build(ServerLevel level, HydraLairs data, Lair lair) {
    int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lair.centre.getX(), lair.centre.getZ());
    int floorY = Mth.clamp(surface - 34, level.getMinBuildHeight() + 6, 40);
    BlockPos c = new BlockPos(lair.centre.getX(), floorY, lair.centre.getZ());
    // o centro real (com a altura certa) fica guardado
    Lair fixed = new Lair(lair.index, c);
    fixed.built = true;
    data.lairs.set(data.lairs.indexOf(lair), fixed);
    data.setDirty();
    buildAt(level, c, surface);
  }

  /** Cupula + corredor + poco ate a superficie. {@code surface} = altura do chao la em cima. */
  public static void buildAt(ServerLevel level, BlockPos c, int surface) {
    BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    BlockState accent = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
    BlockState air = Blocks.CAVE_AIR.defaultBlockState();
    int R = CHAMBER_R;
    // camara: meia esfera (cupula) sobre um piso redondo
    for (int dx = -R - 1; dx <= R + 1; dx++) {
      for (int dz = -R - 1; dz <= R + 1; dz++) {
        for (int dy = -1; dy <= R + 1; dy++) {
          double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.0);
          BlockPos p = c.offset(dx, dy, dz);
          if (dy == -1 && Math.sqrt(dx * dx + dz * dz) <= R + 1) set(level, p, (dx + dz) % 4 == 0 ? accent : floor);
          else if (dy >= 0 && d <= R - 0.5) set(level, p, air);
          else if (dy >= 0 && d <= R + 1.0) set(level, p, (Math.abs(dx) + dy * 3 + Math.abs(dz)) % 11 == 0 ? accent : wall);
        }
      }
    }
    // anel de luz fraca e pilares
    // (os selos do ritual entram no alto dos pilares quando a Hidra e revelada; os pilares sem selo ficam com lanterna)
    for (int k = 0; k < HydraEntity.PILLARS; k++) {
      BlockPos top = HydraEntity.pillarTop(k);
      for (int y = 0; y < top.getY(); y++) set(level, c.offset(top.getX(), y, top.getZ()), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
      set(level, c.offset(top), Blocks.SOUL_LANTERN.defaultBlockState());
    }
    // corredor para leste: da parede ate 22 blocos, 3 de largura e 4 de altura, com 2 salas laterais
    int start = R + 2, end = R + 24;
    for (int x = R - 1; x <= end; x++) {
      for (int z = -2; z <= 2; z++) {
        for (int y = -1; y <= 4; y++) {
          BlockPos p = c.offset(x, y, z);
          boolean inner = Math.abs(z) <= 1 && y >= 0 && y <= 3;
          if (x < start && inner) set(level, p, air);
          else if (x >= start) set(level, p, inner ? air : (y == -1 ? floor : Blocks.DEEPSLATE_BRICKS.defaultBlockState()));
        }
      }
      if (x >= start && x % 4 == 0) set(level, c.offset(x, 3, 0), Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }
    for (int side : new int[] {-1, 1}) {
      int sx = R + 10;
      for (int x = sx - 2; x <= sx + 2; x++) for (int z = 2; z <= 6; z++) for (int y = -1; y <= 4; y++) {
        BlockPos p = c.offset(x, y, z * side);
        boolean inner = Math.abs(x - sx) <= 1 && z >= 2 && z <= 5 && y >= 0 && y <= 3;
        set(level, p, inner ? air : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
      }
      BlockPos chest = c.offset(sx, 0, 5 * side);
      set(level, chest, Blocks.CHEST.defaultBlockState());
      if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
        be.setLootTable(new ResourceLocation("arcane", "chests/glyphs/ancient_city"), level.random.nextLong());
      }
    }
    // porta da camara: grade de ferro (a camara fica fechada ate alguem chegar perto)
    for (int z = -1; z <= 1; z++) for (int y = 0; y <= 3; y++) set(level, c.offset(R, y, z), Blocks.IRON_BARS.defaultBlockState());
    // poco ate a superficie com escada, e um marco de pedra la em cima
    BlockPos shaft = c.offset(end, 0, 0);
    for (int y = 0; y <= surface - c.getY() + 1; y++) {
      for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
        BlockPos p = shaft.offset(dx, y, dz);
        if (dx == 0 && dz == 0) set(level, p, air);
        else if (dx == -1 && dz == 0) set(level, p, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        else set(level, p, y + c.getY() >= surface ? air : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
      }
      set(level, shaft.offset(0, y, 0), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.EAST));
    }
    BlockPos top = new BlockPos(shaft.getX(), surface, shaft.getZ());
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
      if (Math.abs(dx) == 2 || Math.abs(dz) == 2) set(level, top.offset(dx, 0, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
    }
    for (int y = 1; y <= 3; y++) {
      set(level, top.offset(2, y, 2), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
      set(level, top.offset(-2, y, -2), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
      set(level, top.offset(2, y, -2), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
      set(level, top.offset(-2, y, 2), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
    }
    set(level, top.offset(2, 4, 2), Blocks.SOUL_LANTERN.defaultBlockState());
    set(level, top.offset(-2, 4, -2), Blocks.SOUL_LANTERN.defaultBlockState());
  }
}
