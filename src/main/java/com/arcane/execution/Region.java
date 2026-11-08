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
        return Targeting.ray(ctx, Math.max(4.0, Math.min(64.0, spec.range())), false).end();
      case ShapeGeometry.CONE:
        return ctx.position().add(0, caster.getBbHeight() * 0.5, 0);
      default:
        return ctx.position();
    }
  }

  public static Region of(ExecutionContext ctx, DeliverySpec spec, int naturalShape, double radius) {
    int shape = resolveShape(spec, naturalShape);
    double r = Math.max(1.0, Math.min(48.0, radius));
    double origin = spec.parameters().getOrDefault("origin", 0.0);
    Vec3 center;
    if (origin >= 0.5) center = Targeting.ray(ctx, Targeting.AIM_RANGE, false).end();
    else if (origin <= -0.5) center = ctx.position();
    else center = defaultCenter(ctx, spec, shape);
    if (shape == ShapeGeometry.WALL) {
      BlockPos b = BlockPos.containing(center);
      center = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }
    double height = shape == ShapeGeometry.WALL ? wallHeight(r) : shape == ShapeGeometry.COLUMN ? COLUMN_HEIGHT : r;
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

  public AABB bounds() {
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
        for (int dy = 0; dy <= (int) height; dy++) {
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
    com.arcane.network.ModNetwork.sendShape(ctx.level(), type, center, dir, (float) radius, element.color(), element.ordinal(),
        type == S2CShapeFxPacket.WAVE ? 14 : type == S2CShapeFxPacket.PILLAR ? 18 : 16);
    ElementSounds.playImpact(ctx.level(), center, element);
  }
}
