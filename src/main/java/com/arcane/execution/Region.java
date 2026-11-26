package com.arcane.execution;

import com.arcane.network.packet.S2CShapeFxPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Regiao: o volume de uma forma (formato, centro, raio, altura, direcao). TODA forma com area monta uma Regiao a partir
 * do seu formato natural e dos modificadores (formato, origem, raio/Area), e usa a mesma Regiao para escolher criaturas
 * ({@link #entities}), criar blocos ({@link #shell}) e mostrar o efeito ({@link #playFx}). Assim qualquer glifo de
 * formato ou origem vale para qualquer forma.
 *
 * Formatos naturais: Aura = esfera, Onda = cone, Pilar = coluna, Barreira = parede.
 */
public record Region(int shape, Vec3 center, double radius, double height, Vec3 facing) {
  public static final double CONE_COS = Math.cos(Math.toRadians(50));
  public static final double COLUMN_HEIGHT = 10.0;

  // ------------------------------------------------------------------ construcao
  public static int resolveShape(DeliverySpec spec, int natural) {
    int s = ShapeGeometry.shapeOf(spec.parameters().getOrDefault("shape", 0.0));
    return s == ShapeGeometry.DEFAULT ? natural : s;
  }

  /** Meia-largura (em blocos, para cada lado) e altura da parede para um raio. */
  public static int wallHalf(double radius) {
    return (int) Math.max(1, Math.min(8, Math.round(radius * 2.0 / 3.0 - 1)));
  }

  public static int wallHeight(double radius) {
    return (int) Math.max(3, Math.min(12, Math.round(radius)));
  }

  public static boolean wallAlongX(Vec3 look) {
    return Math.abs(look.x) > Math.abs(look.z);
  }

  /** Centro padrao de cada formato quando nenhum glifo de origem foi usado. */
  private static Vec3 defaultCenter(ExecutionContext ctx, DeliverySpec spec, int shape) {
    LivingEntity caster = ctx.caster();
    // Num ritual, as formas de area ficam no centro do circulo (o altar) e cobrem tudo.
    if (ctx.ritualCenter() != null && (shape == ShapeGeometry.SPHERE || shape == ShapeGeometry.DOME || shape == ShapeGeometry.RING || shape == ShapeGeometry.COLUMN)) {
      return ctx.ritualCenter();
    }
    switch (shape) {
      case ShapeGeometry.WALL: {
        Vec3 look = ctx.look();
        boolean alongX = wallAlongX(look);
        int sx = alongX ? (look.x > 0 ? 1 : -1) : 0;
        int sz = alongX ? 0 : (look.z > 0 ? 1 : -1);
        BlockPos b = BlockPos.containing(ctx.position()).offset(sx * 3, 0, sz * 3);
        return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
      }
      case ShapeGeometry.COLUMN:
        return Targeting.ray(ctx, com.arcane.config.ArcaneConfig.RAY_RANGE.get(), false).end();
      case ShapeGeometry.CONE:
        return ctx.position().add(0, caster.getBbHeight() * 0.5, 0);
      default:
        return ctx.position();
    }
  }

  public static Region of(ExecutionContext ctx, DeliverySpec spec, int naturalShape, double radius) {
    int shape = resolveShape(spec, naturalShape);
    double r = Math.max(1.0, Math.min(ctx.ritualRadius() > 0 ? 64.0 : 48.0, radius));
    double origin = spec.parameters().getOrDefault("origin", 0.0);
    Vec3 center;
    if (origin >= 0.5) center = Targeting.ray(ctx, Targeting.AIM_RANGE, false).end();
    else if (origin <= -0.5) center = ctx.position();
    else center = defaultCenter(ctx, spec, shape);
    if (shape == ShapeGeometry.WALL) {
      BlockPos b = BlockPos.containing(center);
      center = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }
    if (shape == ShapeGeometry.COLUMN && ctx.ritualCenter() == null) {
      // A coluna nasce do chao: assenta o centro na celula de ar sobre o chao (se nao ha chao, ver mayManifest).
      var cell = Targeting.groundCell(ctx.level(), center);
      if (cell.isPresent()) center = new Vec3(center.x, cell.get().getY(), center.z);
      else {
        // Mira no alto (criatura no ceu, ou olhar para baixo de uma torre): a coluna desce ate o chao logo abaixo do ponto.
        // Olhar para o ceu vazio continua sem coluna (ver mayManifest): so desce se a mira aponta para baixo ou pega uma criatura.
        boolean onCreature = !ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(1.5), e -> e != ctx.caster() && e.isAlive()).isEmpty();
        if (onCreature || ctx.look().y < -0.05) {
          var dropped = Targeting.dropToGround(ctx.level(), ctx.caster(), center);
          if (dropped.isPresent()) center = dropped.get();
        }
      }
      center = com.arcane.execution.CastChannels.lockAnchor(center); // mantido: nao acompanha a mira
    }
    // A coluna sobe ate o limite de construcao do mundo (o "ceu"); o restante dos formatos mantem as alturas de sempre.
    double height = shape == ShapeGeometry.WALL ? wallHeight(r)
        : shape == ShapeGeometry.COLUMN ? Math.max(COLUMN_HEIGHT, ctx.level().getMaxBuildHeight() - center.y) : r;
    return new Region(shape, center, r, height, ctx.look());
  }

  // ------------------------------------------------------------------ volume
  /** O ponto esta dentro do volume da regiao? */
  public boolean contains(Vec3 p) {
    double dx = p.x - center.x, dy = p.y - center.y, dz = p.z - center.z;
    double d3 = Math.sqrt(dx * dx + dy * dy + dz * dz);
    double d2 = Math.sqrt(dx * dx + dz * dz);
    switch (shape) {
      case ShapeGeometry.DOME:
        return dy >= -0.6 && d3 <= radius;
      case ShapeGeometry.RING:
        return dy >= -0.6 && dy <= 2.6 && Math.abs(d2 - radius) <= 1.5;
      case ShapeGeometry.CONE:
        return d3 < 1.0e-6 || (d3 <= radius && (dx * facing.x + dy * facing.y + dz * facing.z) / d3 >= CONE_COS);
      case ShapeGeometry.COLUMN:
        return d2 <= radius && dy >= -2.0 && dy <= height;
      case ShapeGeometry.WALL: {
        boolean alongX = wallAlongX(facing);
        double fwd = alongX ? dx : dz, lat = alongX ? dz : dx;
        return Math.abs(fwd) <= 1.5 && Math.abs(lat) <= wallHalf(radius) + 1.5 && dy >= -1.5 && dy <= height + 1.5;
      }
      default: // esfera
        return d3 <= radius;
    }
  }

  /** Verdadeiro se uma coluna pode se manifestar aqui: precisa de chao logo abaixo do centro (nunca no ar). Outros formatos: sempre. */
  public static boolean mayManifest(ExecutionContext ctx, DeliverySpec spec, double radius) {
    if (ctx.ritualCenter() != null) return true;
    Region region = Region.of(ctx, spec, ShapeGeometry.COLUMN, radius);
    if (region.shape != ShapeGeometry.COLUMN) return true;
    return Targeting.groundCell(ctx.level(), region.center).isPresent();
  }

  public AABB bounds() {
    if (shape == ShapeGeometry.COLUMN) {
      double h = radius + 2;
      return new AABB(center.x - h, center.y - 4, center.z - h, center.x + h, center.y + height + 2, center.z + h);
    }
    double h = Math.max(radius, height);
    if (shape == ShapeGeometry.WALL) h = Math.max(h, wallHalf(radius) + 2);
    return new AABB(center.x - h - 2, center.y - h - 2, center.z - h - 2, center.x + h + 2, center.y + h + 2, center.z + h + 2);
  }

  /** Criaturas vivas dentro da regiao (o ponto testado e o centro do corpo). */
  public List<Entity> entities(ServerLevel level, LivingEntity caster, boolean includeCaster) {
    List<Entity> out = new ArrayList<>();
    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bounds(),
        x -> x.isAlive() && !x.isSpectator() && (includeCaster || x != caster))) {
      if (contains(e.position().add(0, e.getBbHeight() * 0.5, 0))) out.add(e);
    }
    return out;
  }

  // ------------------------------------------------------------------ blocos
  /** Posicoes da casca da regiao (espessura 1), limitadas a {@link ShapeGeometry#MAX_BLOCKS}. */
  public List<BlockPos> shell() {
    List<BlockPos> out = new ArrayList<>();
    BlockPos base = BlockPos.containing(center);
    double r = ShapeGeometry.clampRadius(shape, radius);
    switch (shape) {
      case ShapeGeometry.DOME, ShapeGeometry.SPHERE, ShapeGeometry.RING -> {
        for (ShapeGeometry.Off o : ShapeGeometry.shell(shape, r)) out.add(base.offset(o.dx(), o.dy(), o.dz()));
      }
      case ShapeGeometry.COLUMN -> {
        int n = (int) Math.ceil(r) + 1;
        for (int dy = 0; dy <= (int) Math.min(height, COLUMN_HEIGHT); dy++) { // so a casca perto do chao vira bloco
          for (int dx = -n; dx <= n; dx++) {
            for (int dz = -n; dz <= n; dz++) {
              double d = Math.sqrt(dx * (double) dx + dz * (double) dz);
              if (d >= r - 0.5 && d < r + 0.5) {
                if (out.size() >= ShapeGeometry.MAX_BLOCKS) return out;
                out.add(base.offset(dx, dy, dz));
              }
            }
          }
        }
      }
      case ShapeGeometry.CONE -> { // calota esferica na frente: a "tampa" do cone
        int n = (int) Math.ceil(r) + 1;
        for (int dy = -n; dy <= n; dy++) {
          for (int dx = -n; dx <= n; dx++) {
            for (int dz = -n; dz <= n; dz++) {
              double d = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
              if (d < r - 0.5 || d >= r + 0.5) continue;
              if ((dx * facing.x + dy * facing.y + dz * facing.z) / d < CONE_COS) continue;
              if (out.size() >= ShapeGeometry.MAX_BLOCKS) return out;
              out.add(base.offset(dx, dy, dz));
            }
          }
        }
      }
      case ShapeGeometry.WALL -> {
        boolean alongX = wallAlongX(facing);
        int half = wallHalf(radius), h = wallHeight(radius);
        for (int w = -half; w <= half; w++) {
          for (int y = 0; y < h; y++) out.add(base.offset(alongX ? 0 : w, y, alongX ? w : 0));
        }
      }
      default -> { }
    }
    return out;
  }

  /**
   * Terreno do fuste de uma coluna: blocos expostos dentro do circulo da coluna, do chao (2 abaixo da marca) ate
   * {@code up} blocos acima, dos mais baixos e proximos do eixo para os demais. E o que a Coluna entrega como blocos.
   */
  public List<TargetList.BlockTarget> columnBlocks(ServerLevel level, int up, int max) {
    BlockPos base = BlockPos.containing(center);
    int n = (int) Math.ceil(radius);
    int top = (int) Math.min(height, up);
    List<BlockPos> found = new ArrayList<>();
    for (int dy = -2; dy <= top; dy++) {
      for (int dx = -n; dx <= n; dx++) {
        for (int dz = -n; dz <= n; dz++) {
          if (dx * dx + dz * dz > radius * radius) continue;
          BlockPos pos = base.offset(dx, dy, dz);
          if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir()) continue;
          boolean exposed = false;
          for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(d)).isAir()) { exposed = true; break; }
          }
          if (exposed) found.add(pos);
        }
      }
    }
    found.sort(java.util.Comparator.comparingDouble((BlockPos p) -> p.getY()).thenComparingDouble(p -> {
      double dx = p.getX() + 0.5 - center.x, dz = p.getZ() + 0.5 - center.z;
      return dx * dx + dz * dz;
    }));
    List<TargetList.BlockTarget> out = new ArrayList<>();
    for (BlockPos p : found) {
      if (out.size() >= max) break;
      out.add(new TargetList.BlockTarget(p, level.getBlockState(p)));
    }
    return out;
  }

  /**
   * Blocos expostos (com ao menos uma face para o ar) dentro da regiao, dos mais proximos do centro para os mais
   * distantes. Alcance de busca limitado a 20 blocos para nao varrer volumes enormes.
   */
  public List<TargetList.BlockTarget> exposedBlocks(ServerLevel level, int max) {
    double scan = Math.min(radius, 20.0);
    BlockPos base = BlockPos.containing(center);
    int n = (int) Math.ceil(scan);
    List<BlockPos> found = new ArrayList<>();
    for (int dx = -n; dx <= n; dx++) {
      for (int dy = -n; dy <= n; dy++) {
        for (int dz = -n; dz <= n; dz++) {
          if (dx * dx + dy * dy + dz * dz > scan * scan) continue;
          BlockPos pos = base.offset(dx, dy, dz);
          if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir()) continue;
          if (!contains(Vec3.atCenterOf(pos))) continue;
          boolean exposed = false;
          for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            if (level.getBlockState(pos.relative(d)).isAir()) { exposed = true; break; }
          }
          if (exposed) found.add(pos);
        }
      }
    }
    found.sort(java.util.Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(center)));
    List<TargetList.BlockTarget> out = new ArrayList<>();
    for (BlockPos p : found) {
      if (out.size() >= max) break;
      out.add(new TargetList.BlockTarget(p, level.getBlockState(p)));
    }
    return out;
  }

  // ------------------------------------------------------------------ efeito visual
  /** Mostra o efeito do formato (anel da Aura, onda do cone, coluna do Pilar). A parede nao precisa: os blocos aparecem. */
  public void playFx(ExecutionContext ctx) {
    SpellElement element = SpellElement.of(ctx.plan());
    int type;
    Vec3 dir = Vec3.ZERO;
    switch (shape) {
      case ShapeGeometry.CONE -> {
        type = S2CShapeFxPacket.WAVE;
        dir = new Vec3(facing.x, 0, facing.z).normalize();
      }
      case ShapeGeometry.COLUMN -> type = S2CShapeFxPacket.PILLAR;
      case ShapeGeometry.WALL -> {
        return;
      }
      default -> type = S2CShapeFxPacket.AURA;
    }
    if (CastChannels.holding()) {
      // Magia mantida: o desenho fica de pe (o cliente reaproveita o mesmo efeito em vez de recomecar a animacao).
      com.arcane.network.ModNetwork.sendShape(ctx.level(), type, center, dir, (float) radius, element.color(), element.ordinal(),
          S2CShapeFxPacket.SUSTAIN + CastChannels.HOLD_INTERVAL_TICKS + 6);
      if (!CastChannels.holdPulse()) com.arcane.fx.Fx.sound(ctx.level(), center, element, com.arcane.fx.Fx.SoundRole.IMPACT, (float) Math.max(1.0, Math.sqrt(radius / 4.0)));
      return;
    }
    com.arcane.network.ModNetwork.sendShape(ctx.level(), type, center, dir, (float) radius, element.color(), element.ordinal(),
        type == S2CShapeFxPacket.WAVE ? 14 : type == S2CShapeFxPacket.PILLAR ? 18 : 16);
    com.arcane.fx.Fx.sound(ctx.level(), center, element, com.arcane.fx.Fx.SoundRole.IMPACT, (float) Math.max(1.0, Math.sqrt(radius / 4.0)));
  }
}
