package com.arcane.client.particle;

import com.arcane.execution.ElementVisuals;
import com.arcane.execution.SpellElement;
import com.arcane.fx.ElementFxOptions;
import com.arcane.fx.Fx;
import com.arcane.fx.FxFamilies;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Particula de elemento ({@link Fx}). O movimento (gravidade, atrito, vida, tamanho, espiral, tremeluzir) e a luz vem do
 * json do elemento; o papel ajusta: rastro dura menos e e menor, impacto e maior, decal fica deitado no bloco atingido
 * e esmaece, reacao sobe devagar. Sprites em tons de cinza levam a cor do elemento (ou a cor pedida); os antigos ja sao
 * coloridos.
 */
public class ElementFxParticle extends TextureSheetParticle {
  /** Brilho somado: luz, raio e auras. Sem escrever profundidade, como as particulas translucidas. */
  public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
    @Override
    public void begin(BufferBuilder builder, TextureManager textures) {
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
      builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

    @Override
    public void end(Tesselator tesselator) {
      tesselator.end();
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(true);
    }

    @Override
    public String toString() { return "ARCANE_ADDITIVE"; }
  };

  private final SpriteSet sprites;
  private final Fx.Role role;
  private final ElementVisuals.Motion motion;
  private final boolean additive;
  private final int light;
  private final float baseAlpha;
  private final Direction face;

  protected ElementFxParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                              SpriteSet sprites, String family, ElementFxOptions o) {
    super(level, x, y, z, dx, dy, dz);
    this.sprites = sprites;
    SpellElement element = SpellElement.byOrdinal(o.element());
    ElementVisuals.Visual visual = element.visual();
    Fx.Role[] roles = Fx.Role.values();
    this.role = roles[Math.floorMod(o.role(), roles.length)];
    this.motion = visual.motion();
    this.additive = visual.additive() && role != Fx.Role.DECAL;
    this.light = visual.light();
    this.face = Direction.from3DDataValue(o.face());

    float jitter = motion.jitter();
    this.xd = dx + (random.nextDouble() - 0.5) * jitter;
    this.yd = dy + (random.nextDouble() - 0.5) * jitter;
    this.zd = dz + (random.nextDouble() - 0.5) * jitter;
    int life = motion.minLife() + random.nextInt(Math.max(1, motion.maxLife() - motion.minLife() + 1));
    float size = motion.size() * o.scale() * (jitter == 0f ? 1f : 0.7f + random.nextFloat() * 0.6f);
    float gravity = motion.gravity();
    switch (role) {
      case TRAIL -> { life = Math.max(3, (int) (life * 0.6f)); size *= 0.8f; gravity *= 0.5f; xd *= 0.3; yd *= 0.3; zd *= 0.3; }
      case IMPACT -> { life = Math.max(4, (int) (life * 0.8f)); size *= 1.25f; }
      case DECAL -> { life = 80 + random.nextInt(30); size = 0.45f * o.scale(); gravity = 0f; xd = yd = zd = 0; }
      case REACTION -> { life = (int) (life * 1.4f) + 8; size *= 1.4f; gravity = -0.01f; }
      default -> { }
    }
    this.lifetime = life;
    this.quadSize = size;
    this.gravity = gravity;
    this.friction = role == Fx.Role.DECAL ? 0f : motion.friction();
    this.hasPhysics = false;

    int rgb = o.color() >= 0 ? o.color() : visual.color();
    if (o.color() >= 0 || !FxFamilies.PRECOLORED.contains(family)) {
      setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
    }
    this.baseAlpha = role == Fx.Role.DECAL ? 0.85f : additive ? 0.7f : 1f; // aditivo mais fraco: sem clarão
    this.alpha = baseAlpha;
    if (role == Fx.Role.DECAL) this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
    setSpriteFromAge(sprites);
  }

  @Override
  public void tick() {
    super.tick();
    if (removed) return;
    setSpriteFromAge(sprites);
    if (motion.spiral() != 0f && role != Fx.Role.DECAL) {
      double c = Math.cos(motion.spiral()), s = Math.sin(motion.spiral());
      double nx = xd * c - zd * s, nz = xd * s + zd * c;
      xd = nx;
      zd = nz;
    }
    float t = (float) age / lifetime;
    float fade = role == Fx.Role.DECAL ? (t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f) : 1f - t * 0.6f;
    float flick = motion.flicker() > 0f ? 1f - motion.flicker() * random.nextFloat() : 1f;
    this.alpha = baseAlpha * fade * flick;
  }

  @Override
  public void render(VertexConsumer buffer, Camera camera, float partial) {
    if (role != Fx.Role.DECAL) {
      super.render(buffer, camera, partial);
      return;
    }
    // decal: quadrado deitado na face atingida, olhando para fora dela (os dois lados, para nao sumir de costas)
    Vec3 cam = camera.getPosition();
    float px = (float) (Mth.lerp(partial, xo, x) - cam.x);
    float py = (float) (Mth.lerp(partial, yo, y) - cam.y);
    float pz = (float) (Mth.lerp(partial, zo, z) - cam.z);
    Quaternionf q = face.getRotation(); // UP -> a face; o quadrado base fica no plano XZ
    q.rotateY(roll);
    Vector3f[] corners = {new Vector3f(-1, 0, -1), new Vector3f(-1, 0, 1), new Vector3f(1, 0, 1), new Vector3f(1, 0, -1)};
    for (Vector3f v : corners) v.mul(quadSize).rotate(q).add(px, py, pz);
    float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
    int lc = getLightColor(partial);
    vertex(buffer, corners[0], u1, v1, lc);
    vertex(buffer, corners[1], u1, v0, lc);
    vertex(buffer, corners[2], u0, v0, lc);
    vertex(buffer, corners[3], u0, v1, lc);
    vertex(buffer, corners[3], u0, v1, lc);
    vertex(buffer, corners[2], u0, v0, lc);
    vertex(buffer, corners[1], u1, v0, lc);
    vertex(buffer, corners[0], u1, v1, lc);
  }

  private void vertex(VertexConsumer b, Vector3f p, float u, float v, int light) {
    b.vertex(p.x(), p.y(), p.z()).uv(u, v).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
  }

  @Override
  public ParticleRenderType getRenderType() {
    return additive ? ADDITIVE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Override
  protected int getLightColor(float partialTick) {
    if (light >= 15) return LightTexture.FULL_BRIGHT;
    BlockPos pos = BlockPos.containing(x, y, z);
    int packed = level.hasChunkAt(pos) ? net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos) : 0;
    int block = Math.max(LightTexture.block(packed), light);
    return LightTexture.pack(block, LightTexture.sky(packed));
  }

  public static class Provider implements ParticleProvider<ElementFxOptions> {
    private final SpriteSet sprites;
    private final String family;

    public Provider(SpriteSet sprites, String family) {
      this.sprites = sprites;
      this.family = family;
    }

    @Override
    public Particle createParticle(ElementFxOptions o, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
      return new ElementFxParticle(level, x, y, z, dx, dy, dz, sprites, family, o);
    }
  }
}
