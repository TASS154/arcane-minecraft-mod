package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.CastingService;
import com.arcane.execution.SpellElement;
import com.arcane.fx.SpellVoice;
import com.arcane.inscription.Inscription;
import com.arcane.network.packet.S2CShapeFxPacket;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Voz da magia (Fase L), parte do servidor. O som NAO foi ouvido. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class VoiceGameTests extends GameTestSupport {

  /** No maximo uma voz por jogador a cada 0,5 s; jogadores diferentes nao se atrapalham. */
  @GameTest(template = "empty", batch = "voice_1")
  public static void oneVoicePerHalfSecond(GameTestHelper h) {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    long t = 1_000_000L + h.getLevel().getGameTime();
    h.assertTrue(CastingService.voiceAllowed(a, t), "primeira voz toca");
    h.assertTrue(!CastingService.voiceAllowed(a, t + 5), "0,25 s depois: so o som simples");
    h.assertTrue(CastingService.voiceAllowed(b, t + 5), "outro jogador toca");
    h.assertTrue(CastingService.voiceAllowed(a, t + CastingService.VOICE_GAP_TICKS), "0,5 s depois: toca de novo");
    h.succeed();
  }

  /** A voz viaja no pacote de efeito e volta igual; magia longa fica em 8 notas (10 com o floreio de magia grande). */
  @GameTest(template = "empty", batch = "voice_1")
  public static void voiceTravelsInTheFxPacket(GameTestHelper h) {
    var spell = new Inscription(List.of(g("projectile"), g("damage"), g("fire"), g("power"), g("slow"), g("poison"),
        g("weaken"), g("blind"), g("lengthen"), g("swift")));
    var voice = CastingService.voiceFor(spell, SpellElement.FIRE, false);
    var notes = SpellVoice.unpack(voice.notes());
    h.assertTrue(notes.size() == SpellVoice.MAX_NOTES || notes.size() == SpellVoice.MAX_WITH_FLOURISH, "notas: " + notes.size());
    h.assertTrue(SpellVoice.FAMILIES.get(voice.family()).equals("fire"), "familia do fogo");
    var big = SpellVoice.unpack(CastingService.voiceFor(spell, SpellElement.FIRE, true).notes());
    h.assertTrue(big.size() == SpellVoice.MAX_WITH_FLOURISH && big.get(big.size() - 1).longTail(), "ritual: floreio com cauda longa");
    var packet = new S2CShapeFxPacket(S2CShapeFxPacket.CAST, new Vec3(1, 2, 3), Vec3.ZERO, 1.5f, 0xFF8C28, 1, 16, voice);
    var buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    S2CShapeFxPacket.encode(packet, buf);
    var back = S2CShapeFxPacket.decode(buf);
    h.assertTrue(back.voice() != null && SpellVoice.unpack(back.voice().notes()).equals(notes), "a voz volta igual");
    var plain = new S2CShapeFxPacket(S2CShapeFxPacket.IMPACT, Vec3.ZERO, Vec3.ZERO, 1f, 0, 0, 12);
    var buf2 = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    S2CShapeFxPacket.encode(plain, buf2);
    h.assertTrue(S2CShapeFxPacket.decode(buf2).voice() == null, "efeito sem voz continua sem voz");
    h.assertTrue(buf2.readableBytes() == 0 && buf.readableBytes() == 0, "nada sobra no pacote");
    h.succeed();
  }
}
