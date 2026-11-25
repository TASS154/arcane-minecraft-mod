package com.arcane.fx;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Particula de elemento: a familia de sprites e o tipo; o resto vai aqui. {@code element} e o ordinal de
 * {@link com.arcane.execution.SpellElement} (o movimento e a luz vem do json dele), {@code role} e o papel
 * ({@link Fx.Role}), {@code scale} multiplica o tamanho, {@code color} sobrescreve a cor (-1 = a do elemento) e
 * {@code face} e a direcao do bloco atingido (so decais).
 */
public record ElementFxOptions(ParticleType<ElementFxOptions> type, int element, int role, float scale, int color, int face)
    implements ParticleOptions {

  @Override
  public ParticleType<?> getType() { return type; }

  @Override
  public void writeToNetwork(FriendlyByteBuf buf) {
    buf.writeVarInt(element);
    buf.writeByte(role);
    buf.writeFloat(scale);
    buf.writeInt(color);
    buf.writeByte(face);
  }

  @Override
  public String writeToString() {
    return ForgeRegistries.PARTICLE_TYPES.getKey(type) + " " + element + " " + role + " " + scale + " " + color + " " + face;
  }

  public static Codec<ElementFxOptions> codec(ParticleType<ElementFxOptions> type) {
    return RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("element").forGetter(ElementFxOptions::element),
        Codec.INT.optionalFieldOf("role", 0).forGetter(ElementFxOptions::role),
        Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(ElementFxOptions::scale),
        Codec.INT.optionalFieldOf("color", -1).forGetter(ElementFxOptions::color),
        Codec.INT.optionalFieldOf("face", 1).forGetter(ElementFxOptions::face)
    ).apply(i, (e, r, s, c, f) -> new ElementFxOptions(type, e, r, s, c, f)));
  }

  @SuppressWarnings("deprecation")
  public static final ParticleOptions.Deserializer<ElementFxOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
    @Override
    public ElementFxOptions fromCommand(ParticleType<ElementFxOptions> type, StringReader reader) throws CommandSyntaxException {
      reader.expect(' ');
      int element = reader.readInt();
      int role = 0;
      float scale = 1f;
      if (reader.canRead()) {
        reader.expect(' ');
        role = reader.readInt();
      }
      if (reader.canRead()) {
        reader.expect(' ');
        scale = reader.readFloat();
      }
      return new ElementFxOptions(type, element, role, scale, -1, 1);
    }

    @Override
    public ElementFxOptions fromNetwork(ParticleType<ElementFxOptions> type, FriendlyByteBuf buf) {
      return new ElementFxOptions(type, buf.readVarInt(), buf.readByte(), buf.readFloat(), buf.readInt(), buf.readByte());
    }
  };
}
