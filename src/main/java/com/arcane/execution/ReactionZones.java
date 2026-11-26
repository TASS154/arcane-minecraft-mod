package com.arcane.execution;

import com.arcane.ArcaneMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Zonas criadas por reacoes em massa (Fase K). Hoje so a Nuvem de Vapor: reduz a visao (particulas densas e, para quem
 * esta dentro, um veu na tela que respeita "reduzir claroes"), apaga fogo, molha, fere de leve e some em 10 a 20 s.
 * Nuvens perto uma da outra se fundem; ha teto por chunk e por mundo (alem dele, a nova se funde com a mais proxima).
 * Apagar fogo de blocos respeita {@code worldAlteringSpells}.
 *
 * <p>Nao usa a lista de {@link Zones} das magias: essas contam no limite de 2 zonas por conjurador (a nuvem apagaria a
 * zona do jogador) e reentregam uma forma. Segue o mesmo padrao de pulso por intervalo no tick do servidor.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class ReactionZones {
  public static final Set<String> KINDS = Set.of("vapor_cloud");
  public static final double MAX_RADIUS = 8.0;
  /** Distancia de fusao: centros mais perto que isto vezes a soma dos raios se fundem. */
  public static final double MERGE_FACTOR = 0.6;
  public static final int PULSE_TICKS = 20, FX_TICKS = 8;

  public static final class Cloud {
    public final UUID id = UUID.randomUUID();
    public final String defId;
    public final ServerLevel level;
    public Vec3 center;
    public double radius;
    public long endsAt;
    public final UUID owner;
    public final double heartsPerSecond;

    Cloud(String defId, ServerLevel level, Vec3 center, double radius, long endsAt, UUID owner, double heartsPerSecond) {
      this.defId = defId;
      this.level = level;
      this.center = center;
      this.radius = radius;
      this.endsAt = endsAt;
      this.owner = owner;
      this.heartsPerSecond = heartsPerSecond;
    }

    public boolean contains(Vec3 p) { return p.distanceToSqr(center) <= radius * radius; }
  }

  private static final List<Cloud> CLOUDS = new ArrayList<>();

  private ReactionZones() {}

  public static List<Cloud> clouds() { synchronized (CLOUDS) { return List.copyOf(CLOUDS); } }

  public static int count(ServerLevel level) {
    synchronized (CLOUDS) { return (int) CLOUDS.stream().filter(c -> c.level == level).count(); }
  }

  public static void clearAll() { synchronized (CLOUDS) { CLOUDS.clear(); } }

  @SubscribeEvent
  public static void onStop(net.minecraftforge.event.server.ServerStoppedEvent event) { clearAll(); }

  // ------------------------------------------------------------------ fusao (pura)
  /** Centro novo de duas nuvens: media ponderada pela area. */
  public static Vec3 mergedCenter(Vec3 a, double ra, Vec3 b, double rb) {
    double wa = ra * ra, wb = rb * rb;
    return a.scale(wa / (wa + wb)).add(b.scale(wb / (wa + wb)));
  }

  /** Raio novo: a area se soma (raiz da soma dos quadrados), ate {@link #MAX_RADIUS}. */
  public static double mergedRadius(double ra, double rb) {
    return Math.min(MAX_RADIUS, Math.sqrt(ra * ra + rb * rb));
  }

  public static boolean shouldMerge(Vec3 a, double ra, Vec3 b, double rb) {
    return a.distanceTo(b) < (ra + rb) * MERGE_FACTOR;
  }

  /** Quantas particulas por pulso visual: linear no raio, entao sub-linear na area. */
  public static int density(double radius) {
    return com.arcane.fx.Fx.areaCount(radius, 3.0, 6);
  }

  // ------------------------------------------------------------------ criar
  /**
   * Cria a nuvem (ou funde com uma existente). Nunca passa do teto: com o chunk ou o mundo cheios, a nova se funde com
   * a mais proxima. Devolve a nuvem que ficou.
   */
  public static Cloud spawn(ServerLevel level, MassReactions.Def def, Vec3 center, UUID owner) {
    long now = level.getGameTime();
    long ends = now + def.seconds() * 20L;
    synchronized (CLOUDS) {
      Cloud near = null;
      double best = Double.MAX_VALUE;
      int inChunk = 0, inWorld = 0;
      long chunk = new net.minecraft.world.level.ChunkPos(BlockPos.containing(center)).toLong();
      for (Cloud c : CLOUDS) {
        if (c.level != level || !c.defId.equals(def.id())) continue;
        inWorld++;
        if (new net.minecraft.world.level.ChunkPos(BlockPos.containing(c.center)).toLong() == chunk) inChunk++;
        double d = c.center.distanceTo(center);
        if (d < best) { best = d; near = c; }
      }
      boolean full = inChunk >= def.maxPerChunk() || inWorld >= def.maxPerWorld();
      if (near != null && (full || shouldMerge(near.center, near.radius, center, def.radius()))) {
        near.center = mergedCenter(near.center, near.radius, center, def.radius());
        near.radius = mergedRadius(near.radius, def.radius());
        near.endsAt = Math.max(near.endsAt, ends);
        return near;
      }
      Cloud c = new Cloud(def.id(), level, center, def.radius(), ends, owner, def.heartsPerSecond());
      CLOUDS.add(c);
      return c;
    }
  }

  // ------------------------------------------------------------------ pulso
  @SubscribeEvent
  public static void onTick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
    List<Cloud> mine;
    synchronized (CLOUDS) {
      if (CLOUDS.isEmpty()) return;
      mine = CLOUDS.stream().filter(c -> c.level == level).toList();
    }
    if (mine.isEmpty()) return;
    long now = level.getGameTime();
    for (Cloud c : mine) {
      if (now >= c.endsAt) {
        synchronized (CLOUDS) { CLOUDS.remove(c); }
        com.arcane.fx.Fx.vapor(level, c.center, c.radius * 0.6, density(c.radius) / 2);
        continue;
      }
      if (now % FX_TICKS == 0) com.arcane.fx.Fx.vapor(level, c.center, c.radius, density(c.radius));
      if (now % PULSE_TICKS == 0) pulse(level, c);
      if (now % 60 == 0) com.arcane.fx.Fx.sound(level, c.center, new net.minecraft.resources.ResourceLocation("arcane", "reaction.vapor"), 0.45f, 0.85f);
    }
  }

  private static boolean worldAltering() {
    try {
      return com.arcane.config.ArcaneConfig.WORLD_ALTERING_SPELLS.get();
    } catch (IllegalStateException e) {
      return true;
    }
  }

  /** Um segundo de nuvem: apaga fogo, molha, fere de leve (menos o dono e os amigos dele) e avisa quem esta dentro. */
  public static void pulse(ServerLevel level, Cloud c) {
    var owner = c.owner == null ? null : level.getEntity(c.owner);
    var guests = GuestLists.get(level.getServer());
    AABB box = new AABB(c.center, c.center).inflate(c.radius);
    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && c.contains(e.position().add(0, e.getBbHeight() * 0.5, 0)))) {
      e.clearFire();
      Wetness.soak(level, e, 60);
      boolean friendly = c.owner != null && (e.getUUID().equals(c.owner) || guests.isFriendly(c.owner, e)
          || (owner != null && Phase4Service.sameTeam(owner, e)));
      if (!friendly && c.heartsPerSecond > 0) {
        e.invulnerableTime = 0;
        e.hurt(owner != null ? Pierce.spell(level, owner) : level.damageSources().magic(), (float) (c.heartsPerSecond * 2.0));
      }
      if (e instanceof ServerPlayer p) com.arcane.network.ModNetwork.sendVapor(p, PULSE_TICKS + 10);
    }
    if (worldAltering()) {
      int r = (int) Math.ceil(c.radius);
      BlockPos base = BlockPos.containing(c.center);
      for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -2, -r), base.offset(r, 2, r))) {
        if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock && c.contains(Vec3.atCenterOf(pos))) {
          level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
      }
    }
  }
}
