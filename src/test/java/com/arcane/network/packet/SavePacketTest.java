package com.arcane.network.packet;

import com.arcane.TestGlyphs;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** O servidor nunca confia nos valores vindos da bancada. */
class SavePacketTest {
  @BeforeAll
  static void loadGlyphs() {
    TestGlyphs.load();
  }

  private static ResourceLocation id(String path) {
    return new ResourceLocation("arcane", path);
  }

  @Test
  void valuesAreClampedAndDefaultsAreDropped() {
    assertEquals(Map.of("amount", 20.0), C2SSaveInscriptionPacket.sanitize(id("damage"), Map.of("amount", 1e9)));
    assertEquals(Map.of("amount", 1.0), C2SSaveInscriptionPacket.sanitize(id("damage"), Map.of("amount", -3.0)));
    assertEquals(Map.of(), C2SSaveInscriptionPacket.sanitize(id("damage"), Map.of("amount", 4.0)), "padrao nao e guardado");
  }

  @Test
  void unknownKeysGlyphsAndNaNAreIgnored() {
    assertEquals(Map.of(), C2SSaveInscriptionPacket.sanitize(id("damage"), Map.of("inventado", 5.0)));
    assertEquals(Map.of(), C2SSaveInscriptionPacket.sanitize(id("nao_existe"), Map.of("amount", 5.0)));
    assertEquals(Map.of(), C2SSaveInscriptionPacket.sanitize(id("damage"), Map.of("amount", Double.NaN)));
  }

  @Test
  void packetRoundTripsAndBoundsItsSize() {
    var packet = new C2SSaveInscriptionPacket("Minha magia", List.of(
        new C2SSaveInscriptionPacket.Entry("arcane:projectile", Map.of()),
        new C2SSaveInscriptionPacket.Entry("arcane:damage", Map.of("amount", 9.0))));
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    C2SSaveInscriptionPacket.encode(packet, buf);
    var decoded = C2SSaveInscriptionPacket.decode(buf);
    assertEquals("Minha magia", decoded.name());
    assertEquals(2, decoded.glyphs().size());
    assertEquals(9.0, decoded.glyphs().get(1).params().get("amount"));

    // Um cliente malicioso declara milhoes de glifos: o decodificador para no limite.
    FriendlyByteBuf evil = new FriendlyByteBuf(Unpooled.buffer());
    evil.writeUtf("x");
    evil.writeVarInt(5_000_000);
    for (int i = 0; i < C2SSaveInscriptionPacket.MAX_GLYPHS + 1; i++) {
      evil.writeUtf("arcane:self");
      evil.writeVarInt(0);
    }
    assertEquals(C2SSaveInscriptionPacket.MAX_GLYPHS + 1, C2SSaveInscriptionPacket.decode(evil).glyphs().size());
  }
}
