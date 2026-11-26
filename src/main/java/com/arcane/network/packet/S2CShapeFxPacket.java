package com.arcane.network.packet;

import com.arcane.client.render.ShapeFxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Efeito visual 3D de uma forma (aura, onda, pilar), impacto ou ritual. So visual; o dano e do servidor. */
public record S2CShapeFxPacket(int type, Vec3 pos, Vec3 dir, float size, int rgb, int element, int ticks, Voice voice) {
  /**
   * Campo compacto da voz da magia (Fase L), so no efeito de conjuracao: familia da amostra, notas empacotadas por
   * {@link com.arcane.fx.SpellVoice#pack} e, da Fase M, maestria (0 a 1) e assinatura do conjurador. Ausente = so o som
   * simples de sempre.
   */
  public record Voice(int family, long[] notes, float mastery, int signature, int caster) {}

  public S2CShapeFxPacket(int type, Vec3 pos, Vec3 dir, float size, int rgb, int element, int ticks) {
    this(type, pos, dir, size, rgb, element, ticks, null);
  }

  /** Somado a {@code ticks}: efeito mantido (magia segurada); o cliente o renova em vez de repeti-lo. */
  public static final int SUSTAIN = 1000;
  public static final int AURA = 0, WAVE = 1, PILLAR = 2, IMPACT = 3, RITUAL = 4, HOLOGRAM = 5, CAST = 6;
  /**
   * Efeito unico (glifos originais e Hidra): {@code element} diz qual (FX_*), {@code size} o raio, {@code dir} um
   * segundo ponto ou altura, conforme o efeito. Desenhado por OriginalFxRenderer.
   */
  public static final int ORIGINAL = 7;
  public static final int FX_TIME_STOP = 0, FX_REWIND = 1, FX_SPACE_COLLAPSE = 2, FX_ANNIHILATE = 3, FX_ECLIPSE = 4, FX_GENESIS = 5,
      FX_GRAVITY = 6, FX_WARN = 7, FX_DEVASTATE = 8, FX_BREAKOUT = 9, FX_AWAKEN = 10, FX_ROAR = 11, FX_SEAL_BREAK = 12,
      FX_SHOCKWAVE = 13;

  public static void encode(S2CShapeFxPacket p, FriendlyByteBuf buf) {
    buf.writeVarInt(p.type);
    buf.writeDouble(p.pos.x).writeDouble(p.pos.y).writeDouble(p.pos.z);
    buf.writeFloat((float) p.dir.x).writeFloat((float) p.dir.y).writeFloat((float) p.dir.z);
    buf.writeFloat(p.size);
    buf.writeInt(p.rgb);
    buf.writeVarInt(p.element);
    buf.writeVarInt(p.ticks);
    buf.writeBoolean(p.voice != null);
    if (p.voice != null) {
      buf.writeByte(p.voice.family());
      for (int i = 0; i < 3; i++) buf.writeLong(i < p.voice.notes().length ? p.voice.notes()[i] : 0L);
      buf.writeByte(Math.round(Math.max(0f, Math.min(1f, p.voice.mastery())) * 255));
      buf.writeInt(p.voice.signature());
      buf.writeVarInt(p.voice.caster());
    }
  }

  public static S2CShapeFxPacket decode(FriendlyByteBuf buf) {
    int type = buf.readVarInt();
    Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    Vec3 dir = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
    float size = buf.readFloat();
    int rgb = buf.readInt(), element = buf.readVarInt(), ticks = buf.readVarInt();
    Voice voice = null;
    if (buf.readBoolean()) {
      int family = buf.readUnsignedByte();
      long[] notes = {buf.readLong(), buf.readLong(), buf.readLong()};
      float mastery = buf.readUnsignedByte() / 255f;
      int signature = buf.readInt();
      voice = new Voice(family, notes, mastery, signature, buf.readVarInt());
    }
    return new S2CShapeFxPacket(type, pos, dir, size, rgb, element, ticks, voice);
  }

  public static void handle(S2CShapeFxPacket p, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> onClient(p));
    ctx.get().setPacketHandled(true);
  }

  @OnlyIn(Dist.CLIENT)
  private static void onClient(S2CShapeFxPacket p) {
    ShapeFxRenderer.add(p.type, p.pos, p.dir, p.size, p.rgb, p.element, p.ticks);
    if (p.voice != null && p.voice.family() < com.arcane.fx.SpellVoice.FAMILIES.size()) {
      com.arcane.client.VoicePlayer.play(com.arcane.fx.SpellVoice.FAMILIES.get(p.voice.family()),
          com.arcane.fx.SpellVoice.unpack(p.voice.notes()), p.pos.x, p.pos.y, p.pos.z, p.voice.mastery());
      if (p.type == CAST) com.arcane.client.CastStyleFx.onCast(p.pos, p.element, p.voice.mastery(), p.voice.signature(), p.voice.caster());
    }
  }
}
