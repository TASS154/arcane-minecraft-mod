package com.arcane.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Particula arcana de 4 quadros que encolhe e esmaece. O perfil define o movimento de cada elemento:
 * brasa sobe, veneno pinga, gelo flutua devagar, raio dura pouco, forca se expande, vazio afunda, vida sobe.
 */
public class ArcaneSparkParticle extends TextureSheetParticle {
  /** gravity < 0 sobe; size em blocos; speedJitter espalha a velocidade inicial. */
  public record Profile(float gravity, int minLife, int maxLife, float size, float friction, float speedJitter) {
    public static final Profile SPARK = new Profile(0f, 14, 26, 0.09f, 0.9f, 0.01f);
    public static final Profile EMBER = new Profile(-0.012f, 16, 30, 0.1f, 0.92f, 0.025f);
    public static final Profile FROST = new Profile(0.002f, 22, 38, 0.1f, 0.94f, 0.01f);
    public static final Profile BOLT = new Profile(0f, 5, 9, 0.16f, 0.8f, 0.05f);
    public static final Profile VENOM = new Profile(0.03f, 14, 24, 0.09f, 0.9f, 0.015f);
    public static final Profile FORCE = new Profile(0f, 12, 20, 0.13f, 0.88f, 0.06f);
    public static final Profile VOID = new Profile(0.015f, 18, 32, 0.12f, 0.92f, 0.02f);
    public static final Profile STRIKE = new Profile(0f, 8, 8, 2.0f, 1f, 0f);
    public static final Profile SHOCK = new Profile(0f, 10, 10, 1.6f, 1f, 0f);
    public static final Profile LIFE = new Profile(-0.02f, 18, 30, 0.11f, 0.93f, 0.012f);
  }

  private final SpriteSet sprites;

  protected ArcaneSparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                                SpriteSet sprites, Profile profile) {
    super(level, x, y, z, dx, dy, dz);
    this.sprites = sprites;
    this.xd = dx + (random.nextDouble() - 0.5) * profile.speedJitter();
    this.yd = dy + (random.nextDouble() - 0.5) * profile.speedJitter();
    this.zd = dz + (random.nextDouble() - 0.5) * profile.speedJitter();
    this.lifetime = profile.minLife() + random.nextInt(Math.max(1, profile.maxLife() - profile.minLife() + 1));
    this.quadSize = profile.size() * (profile.speedJitter() == 0f ? 1f : 0.7f + random.nextFloat() * 0.6f);
    this.gravity = profile.gravity();
    this.friction = profile.friction();
    this.hasPhysics = false;
    setSpriteFromAge(sprites);
  }

  @Override
  public void tick() {
    super.tick();
    if (!removed) {
      setSpriteFromAge(sprites);
      this.alpha = quadSize > 1f ? 1.0f : 1.0f - ((float) age / lifetime) * 0.6f;
    }
  }

  @Override
  public ParticleRenderType getRenderType() {
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Override
  protected int getLightColor(float partialTick) {
    return 0xF000F0;
  }

  public static class Provider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;
    private final Profile profile;

    public Provider(SpriteSet sprites) {
      this(sprites, Profile.SPARK);
    }

    public Provider(SpriteSet sprites, Profile profile) {
      this.sprites = sprites;
      this.profile = profile;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
      return new ArcaneSparkParticle(level, x, y, z, dx, dy, dz, sprites, profile);
    }
  }
}
