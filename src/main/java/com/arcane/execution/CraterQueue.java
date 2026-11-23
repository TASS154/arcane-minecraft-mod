package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.config.ArcaneConfig;
import com.arcane.restore.BlockJournal;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crateras como onda de choque. Os blocos de uma cratera sao separados em cascas de meio bloco pela distancia ao
 * centro; uma frente sai do centro a craterSpreadSpeed blocos por tick e cada tick remove as cascas que ela ja passou
 * (a cratera abre de dentro para fora, com poeira e estouros na frente). O orcamento craterBlocksPerTick continua
 * valendo para todas as crateras juntas: se a frente passar do orcamento, ela espera.
 *
 * <p>Barreiras: blocos de Barreira nunca saem, e o que esta atras deles (visto do centro) fica (ver BarrierShadow).
 * Os blocos removidos entram no diario do Restaurar.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class CraterQueue {
  private static final Map<ServerLevel, List<Crater>> PENDING = new HashMap<>();

  /** Uma cratera em andamento. */
  private static final class Crater {
    final Vec3 centre;
    /** Cascas de 0,5 bloco: shells[i] tem os blocos entre i/2 e (i+1)/2 de distancia. */
    final LongArrayList[] shells;
    float front;
    int shell, cursor, left;

    Crater(Vec3 centre, LongArrayList[] shells, int total) {
      this.centre = centre;
      this.shells = shells;
      this.left = total;
    }
  }

  private CraterQueue() {}

  /** Entrega a cratera com o centro no meio dos blocos. */
  public static void submit(ServerLevel level, LongArrayList blocks) {
    if (blocks.isEmpty()) return;
    double x = 0, y = 0, z = 0;
    for (int i = 0; i < blocks.size(); i++) {
      BlockPos p = BlockPos.of(blocks.getLong(i));
      x += p.getX() + 0.5;
      y += p.getY() + 0.5;
      z += p.getZ() + 0.5;
    }
    submit(level, blocks, new Vec3(x / blocks.size(), y / blocks.size(), z / blocks.size()));
  }

  /** Entrega a cratera; a onda sai de {@code centre}. A primeira casca ja sai neste tick. */
  public static void submit(ServerLevel level, LongArrayList blocks, Vec3 centre) {
    if (blocks.isEmpty()) return;
    double far = 0;
    for (int i = 0; i < blocks.size(); i++) far = Math.max(far, Vec3.atCenterOf(BlockPos.of(blocks.getLong(i))).distanceToSqr(centre));
    far = Math.sqrt(far);
    BarrierShadow shadow = BarrierShadow.scan(level, centre, far + 1);
    var barrier = com.arcane.registry.ModBlocks.ARCANE_BARRIER.get();
    int n = (int) Math.ceil(far * 2) + 1;
    LongArrayList[] shells = new LongArrayList[n];
    int total = 0;
    for (int i = 0; i < blocks.size(); i++) {
      long l = blocks.getLong(i);
      BlockPos p = BlockPos.of(l);
      if (shadow != null && (level.getBlockState(p).is(barrier) || shadow.shadowed(p))) continue;
      int k = Math.min(n - 1, (int) (Vec3.atCenterOf(p).distanceTo(centre) * 2));
      if (shells[k] == null) shells[k] = new LongArrayList();
      shells[k].add(l);
      total++;
    }
    if (total == 0) return;
    Crater c = new Crater(centre, shells, total);
    c.front = 1.5f;
    PENDING.computeIfAbsent(level, l -> new ArrayList<>()).add(c);
    if (far >= 3) {
      // a onda de choque visivel acompanha a frente
      com.arcane.network.ModNetwork.sendShape(level, com.arcane.network.packet.S2CShapeFxPacket.ORIGINAL, centre,
          new Vec3(speed(), 0, 0), (float) far, 0xFFC080, com.arcane.network.packet.S2CShapeFxPacket.FX_SHOCKWAVE,
          (int) Math.ceil(far / speed()) + 10);
    }
    process(level, ArcaneConfig.CRATER_BLOCKS_PER_TICK.get(), false);
  }

  private static float speed() { return (float) (double) ArcaneConfig.CRATER_SPREAD_SPEED.get(); }

  public static int pending(ServerLevel level) {
    var list = PENDING.get(level);
    if (list == null) return 0;
    int n = 0;
    for (Crater c : list) n += c.left;
    return n;
  }

  /** Remove o que as frentes ja alcancaram, ate o orcamento. {@code advance}: a frente anda (uma vez por tick). */
  private static void process(ServerLevel level, int budget, boolean advance) {
    var list = PENDING.get(level);
    if (list == null) return;
    for (var it = list.iterator(); it.hasNext() && budget > 0; ) {
      Crater c = it.next();
      if (advance) c.front += speed();
      int reach = (int) (c.front * 2);
      int fx = 0;
      while (budget > 0 && c.shell < c.shells.length && c.shell <= reach) {
        LongArrayList shell = c.shells[c.shell];
        if (shell == null || c.cursor >= shell.size()) {
          c.shell++;
          c.cursor = 0;
          continue;
        }
        BlockPos pos = BlockPos.of(shell.getLong(c.cursor++));
        c.left--;
        BlockState before = level.getBlockState(pos);
        if (before.isAir()) continue;
        level.removeBlock(pos, false);
        BlockJournal.record(level, pos, before);
        budget--;
        // poeira do bloco e um estouro, em poucos blocos da frente (leve para a rede)
        if ((c.cursor & 15) == 0 && fx < 6) {
          fx++;
          level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, before), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.15);
          if (fx % 3 == 0) level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
      }
      if (c.shell >= c.shells.length || c.left <= 0) it.remove();
    }
    if (list.isEmpty()) PENDING.remove(level);
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
    for (ServerLevel level : new ArrayList<>(PENDING.keySet())) process(level, ArcaneConfig.CRATER_BLOCKS_PER_TICK.get(), true);
  }

  @SubscribeEvent
  public static void onStopped(ServerStoppedEvent event) {
    PENDING.clear();
  }
}
