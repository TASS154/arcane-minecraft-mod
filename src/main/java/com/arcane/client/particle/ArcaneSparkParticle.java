package com.arcane.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class ArcaneSparkParticle extends TextureSheetParticle {
  private final SpriteSet sprites;

  protected ArcaneSparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
    super(level, x, y, z, dx, dy, dz);
    this.sprites = sprites;
    this.xd = dx + (random.nextDouble() - 0.5) * 0.01;
    this.yd = dy + (random.nextDouble() - 0.5) * 0.01;
    this.zd = dz + (random.nextDouble() - 0.5) * 0.01;
    this.lifetime = 14 + random.nextInt(12);
    this.quadSize = 0.06f + random.nextFloat() * 0.06f;
    this.gravity = 0f;
    this.friction = 0.9f;
    this.hasPhysics = false;
    setSpriteFromAge(sprites);
  }

  @Override
  public void tick() {
    super.tick();
    if (!removed) {
      setSpriteFromAge(sprites);
      this.alpha = 1.0f - ((float) age / lifetime) * 0.7f;
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

    public Provider(SpriteSet sprites) {
      this.sprites = sprites;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
      return new ArcaneSparkParticle(level, x, y, z, dx, dy, dz, sprites);
    }
  }
}
