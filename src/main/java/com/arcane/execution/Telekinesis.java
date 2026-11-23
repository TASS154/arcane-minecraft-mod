package com.arcane.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Telecinese: o conjurador segura criaturas e/ou um grupo de blocos e os leva consigo, pairando a uma distancia da mira.
 * O grupo de blocos e decidido na hora de conjurar: um bloco, uma esfera em volta do bloco mirado, ou os blocos
 * conectados do mesmo tipo (opcionalmente limitados pela esfera). Os blocos viram entidades de bloco sem gravidade;
 * ao soltar (fim da duracao, nova conjuracao, morte do conjurador) voltam a cair e pousam como blocos, com o embalo.
 */
public final class Telekinesis {
  public static final int MAX_BLOCKS = 120;
  public static final int MAX_ENTITIES = 6;

  private static final class Held {
    final Entity entity;
    final Vec3 offset;
    final boolean block;
    final boolean hadNoGravity;

    Held(Entity entity, Vec3 offset, boolean block) {
      this.entity = entity;
      this.offset = offset;
      this.block = block;
      this.hadNoGravity = entity.isNoGravity();
    }
  }

  private static final class Hold {
    final UUID caster;
    final ServerLevel level;
    final long endTick;
    final double distance;
    final List<Held> held = new ArrayList<>();

    Hold(UUID caster, ServerLevel level, long endTick, double distance) {
      this.caster = caster;
      this.level = level;
      this.endTick = endTick;
      this.distance = distance;
    }
  }

  private static final Map<UUID, Hold> HOLDS = new ConcurrentHashMap<>();

  private Telekinesis() {}

  public static int heldCount(UUID caster) {
    Hold h = HOLDS.get(caster);
    return h == null ? 0 : (int) h.held.stream().filter(x -> x.entity.isAlive()).count();
  }

  /** Blocos do grupo escolhido a partir do bloco mirado. */
  public static List<BlockPos> collect(ServerLevel level, BlockPos seed, int sphere, boolean sameType, int limit) {
    BlockState seedState = level.getBlockState(seed);
    List<BlockPos> out = new ArrayList<>();
    if (!movable(level, seed, seedState)) return out;
    if (!sameType && sphere <= 0) {
      out.add(seed);
      return out;
    }
    if (!sameType) {
      int n = sphere;
      for (int dx = -n; dx <= n; dx++) {
        for (int dy = -n; dy <= n; dy++) {
          for (int dz = -n; dz <= n; dz++) {
            if (dx * dx + dy * dy + dz * dz > sphere * sphere) continue;
            BlockPos p = seed.offset(dx, dy, dz);
            if (movable(level, p, level.getBlockState(p))) out.add(p);
          }
        }
      }
      out.sort(Comparator.comparingDouble(p -> p.distSqr(seed)));
      return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }
    // Mesmo tipo: conectados ao bloco mirado (pelas faces), ate o limite, e dentro da esfera quando ela foi pedida.
    java.util.Set<BlockPos> seen = new java.util.HashSet<>();
    ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    queue.add(seed);
    seen.add(seed);
    while (!queue.isEmpty() && out.size() < limit) {
      BlockPos p = queue.poll();
      out.add(p);
      for (Direction d : Direction.values()) {
        BlockPos n = p.relative(d);
        if (seen.contains(n)) continue;
        if (sphere > 0 && n.distSqr(seed) > sphere * sphere) continue;
        BlockState st = level.getBlockState(n);
        if (st.getBlock() == seedState.getBlock() && movable(level, n, st)) {
          seen.add(n);
          queue.add(n);
        }
      }
    }
    return out;
  }

  private static boolean movable(ServerLevel level, BlockPos pos, BlockState state) {
    return !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0
        && !state.hasBlockEntity() && level.hasChunkAt(pos);
  }

  /** Comeca a segurar: entidades e blocos (o ponto de ancora e o centro do grupo). Substitui a seguranca anterior. */
  public static int grab(ExecutionContext context, List<Entity> entities, List<BlockPos> blocks, double distance, int ticks) {
    LivingEntity caster = context.caster();
    release(caster.getUUID());
    ServerLevel level = context.level();
    Hold hold = new Hold(caster.getUUID(), level, level.getGameTime() + ticks, distance);
    Vec3 anchor = null;
    for (Entity e : entities) {
      if (e == caster || hold.held.size() >= MAX_ENTITIES || !e.isAlive()) continue;
      if (anchor == null) anchor = e.position().add(0, e.getBbHeight() * 0.5, 0);
      hold.held.add(new Held(e, e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(anchor), false));
    }
    if (!blocks.isEmpty()) {
      Vec3 centroid = Vec3.ZERO;
      for (BlockPos p : blocks) centroid = centroid.add(Vec3.atCenterOf(p));
      centroid = centroid.scale(1.0 / blocks.size());
      if (anchor == null) anchor = centroid;
      Vec3 a = anchor;
      for (BlockPos p : blocks) {
        BlockState st = level.getBlockState(p);
        if (!movable(level, p, st)) continue;
        FallingBlockEntity fb = FallingBlockEntity.fall(level, p, st);
        fb.setNoGravity(true);
        fb.dropItem = true;
        hold.held.add(new Held(fb, Vec3.atCenterOf(p).subtract(a), true));
      }
    }
    if (hold.held.isEmpty()) return 0;
    for (Held h : hold.held) h.entity.setNoGravity(true);
    // Pegar: cada objeto solta um estouro de forca e um anel; o conjurador ouve o "zumbido" da telecinese.
    for (int i = 0; i < Math.min(hold.held.size(), 16); i++) {
      Entity e = hold.held.get(i).entity;
      Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
      level.sendParticles(com.arcane.registry.ModParticles.ARCANE_FORCE.get(), c.x, c.y, c.z, 10, 0.35, 0.35, 0.35, 0.08);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.05);
    }
    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.6f);
    HOLDS.put(caster.getUUID(), hold);
    return hold.held.size();
  }

  /** Soltar: cada objeto (ate 16) solta um estouro; som de energia se desfazendo. */
  private static void releaseFx(Hold hold) {
    int n = 0;
    for (Held h : hold.held) {
      if (!h.entity.isAlive() || n++ >= 16) continue;
      Vec3 c = h.entity.position().add(0, h.entity.getBbHeight() * 0.5, 0);
      hold.level.sendParticles(com.arcane.registry.ModParticles.ARCANE_FORCE.get(), c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.12);
    }
    if (!hold.held.isEmpty()) {
      Vec3 c = hold.held.get(0).entity.position();
      hold.level.playSound(null, c.x, c.y, c.z, net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);
    }
  }

  public static void release(UUID caster) {
    Hold hold = HOLDS.remove(caster);
    if (hold == null) return;
    releaseFx(hold);
    for (Held h : hold.held) {
      if (!h.entity.isAlive()) continue;
      h.entity.setNoGravity(h.hadNoGravity && !h.block);
      h.entity.hurtMarked = true;
    }
  }

  private static final net.minecraft.core.particles.DustParticleOptions TETHER =
      new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.72f, 0.45f, 1.0f), 0.9f);

  /**
   * Enquanto segura: um fio de energia ondulante sai da mao do conjurador ate o ponto de ancora, e cada objeto preso
   * (amostra de ate 10 por tick, alternando) tem uma aura girando em volta. Leve: poucas particulas, so a cada 2 ticks.
   */
  private static void heldFx(Hold hold, Entity caster, Vec3 anchor) {
    long t = hold.level.getGameTime();
    if ((t & 1) != 0 || hold.held.isEmpty()) return;
    Vec3 look = caster.getLookAngle();
    Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
    Vec3 hand = (caster instanceof LivingEntity le ? le.getEyePosition() : caster.position()).add(right.scale(0.35)).add(0, -0.35, 0).add(look.scale(0.4));
    Vec3 up = look.cross(right).normalize();
    int steps = 12;
    for (int i = 1; i < steps; i++) {
      double f = i / (double) steps;
      double wave = Math.sin(f * Math.PI) * Math.sin(t * 0.35 + f * 9.0) * 0.25;
      Vec3 p = hand.lerp(anchor, f).add(up.scale(wave)).add(right.scale(Math.cos(t * 0.3 + f * 7.0) * 0.12 * Math.sin(f * Math.PI)));
      hold.level.sendParticles(TETHER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }
    int n = hold.held.size();
    int start = (int) ((t / 2) % Math.max(1, n));
    for (int k = 0; k < Math.min(10, n); k++) {
      Entity e = hold.held.get((start + k) % n).entity;
      double r = Math.max(0.55, e.getBbWidth() * 0.8);
      double a = t * 0.25 + k * 1.7;
      Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
      hold.level.sendParticles(com.arcane.registry.ModParticles.ARCANE_FORCE.get(), c.x + Math.cos(a) * r, c.y + Math.sin(a * 0.7) * 0.3, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
      hold.level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, c.x - Math.cos(a) * r, c.y, c.z - Math.sin(a) * r, 1, 0, 0, 0, 0);
    }
  }

  public static void tick(MinecraftServer server) {
    if (HOLDS.isEmpty()) return;
    for (Iterator<Hold> it = HOLDS.values().iterator(); it.hasNext(); ) {
      Hold hold = it.next();
      Entity caster = hold.level.getEntity(hold.caster);
      if (caster == null || !caster.isAlive() || caster.level() != hold.level || hold.level.getGameTime() >= hold.endTick) {
        it.remove();
        releaseFx(hold);
        for (Held h : hold.held) {
          if (!h.entity.isAlive()) continue;
          h.entity.setNoGravity(h.hadNoGravity && !h.block);
          h.entity.hurtMarked = true;
        }
        continue;
      }
      Vec3 eye = caster instanceof LivingEntity le ? le.getEyePosition() : caster.position();
      Vec3 anchor = eye.add(caster.getLookAngle().scale(hold.distance));
      hold.held.removeIf(h -> !h.entity.isAlive());
      heldFx(hold, caster, anchor);
      for (Held h : hold.held) {
        Vec3 wanted = anchor.add(h.offset);
        Entity e = h.entity;
        Vec3 pos = h.block ? e.position().add(0, 0.49, 0) : e.position().add(0, e.getBbHeight() * 0.5, 0);
        Vec3 delta = wanted.subtract(pos).scale(0.28);
        e.fallDistance = 0.0f;
        if (e instanceof FallingBlockEntity fb) {
          fb.time = 1;
          Vec3 next = e.position().add(delta);
          e.setPos(next.x, next.y, next.z);
          e.setDeltaMovement(delta);
        } else if (e instanceof net.minecraft.server.level.ServerPlayer) {
          e.setDeltaMovement(delta);
          e.hurtMarked = true;
        } else {
          Vec3 next = e.position().add(delta);
          e.setPos(next.x, next.y, next.z);
          e.setDeltaMovement(delta);
          e.hurtMarked = true;
        }
      }
      if (hold.held.isEmpty()) it.remove();
    }
  }
}
