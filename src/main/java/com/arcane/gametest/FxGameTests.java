package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.SpellElement;
import com.arcane.fx.Fx;
import com.arcane.fx.FxSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Fachada de efeitos: orcamento com raio grande e elementos com json valido. */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class FxGameTests {

  /**
   * Raio 128: o anel e a casca de esfera (cerca de 206 mil blocos) mandam particulas proporcionais ao raio, abaixo do
   * teto por conjuracao, e o custo de mandar fica pequeno.
   */
  @GameTest(template = "empty", batch = "fx_budget_1")
  public static void hugeRadiusStaysLinearAndUnderTheCap(GameTestHelper h) {
    var level = h.getLevel();
    Vec3 center = h.absoluteVec(new Vec3(3, 2, 3));
    int cap = FxSettings.maxPerCast();
    int[] ring = new int[3];
    double[] radii = {16, 64, 128};
    for (int i = 0; i < radii.length; i++) {
      var src = h.spawn(EntityType.ARMOR_STAND, new BlockPos(1 + i, 1, 1)); // uma fonte por medida
      ring[i] = Fx.ring(level, src, center, radii[i], SpellElement.FIRE, Fx.Role.BODY, 2.0);
    }
    h.assertTrue(ring[2] <= cap, "anel de raio 128 abaixo do teto (" + ring[2] + " de " + cap + ")");
    h.assertTrue(ring[2] <= ring[1] * 2 + 1 && ring[1] <= ring[0] * 4 + 1,
        "cresce no maximo linear com o raio (16: " + ring[0] + ", 64: " + ring[1] + ", 128: " + ring[2] + ")");

    // casca de esfera de raio 128 (so a lista de posicoes, sem blocos no mundo)
    List<BlockPos> shell = new ArrayList<>();
    int r = 128;
    BlockPos c = BlockPos.containing(center);
    for (int k = 0; k < 4 * Math.PI * r * r; k++) {
      double z = 1 - 2.0 * (k + 0.5) / (4 * Math.PI * r * r), phi = k * 2.399963;
      double rr = Math.sqrt(1 - z * z);
      shell.add(c.offset((int) (Math.cos(phi) * rr * r), (int) (z * r), (int) (Math.sin(phi) * rr * r)));
    }
    var src = h.spawn(EntityType.ARMOR_STAND, new BlockPos(5, 1, 1));
    long t0 = System.nanoTime();
    int sparks = Fx.shellSparks(level, src, shell, SpellElement.ARCANE);
    double ms = (System.nanoTime() - t0) / 1.0e6;
    h.assertTrue(sparks <= cap, "casca de " + shell.size() + " blocos manda " + sparks + " faiscas (teto " + cap + ")");
    h.assertTrue(sparks < shell.size() / 100, "uma amostra, nao um por bloco (" + sparks + ")");
    h.assertTrue(ms < 50, "mandar as faiscas custa pouco (" + ms + " ms)");
    System.out.println("[fx-budget] anel 16/64/128: " + ring[0] + "/" + ring[1] + "/" + ring[2] + "; casca r128 "
        + shell.size() + " blocos -> " + sparks + " faiscas em " + String.format("%.2f", ms) + " ms; teto " + cap);
    h.succeed();
  }

  /** Todo elemento tem sprite registrado, decal registrado e som registrado (json valido, sem nome errado). */
  @GameTest(template = "empty", batch = "fx_elements_1")
  public static void everyElementPointsToRegisteredSpritesAndSounds(GameTestHelper h) {
    List<String> wrong = new ArrayList<>();
    for (SpellElement e : SpellElement.values()) {
      var v = e.visual();
      if (!com.arcane.registry.ModParticles.FX.containsKey(v.sprite())) wrong.add(e + " sprite " + v.sprite());
      if (!com.arcane.registry.ModParticles.FX.containsKey("decal_" + v.decal())) wrong.add(e + " decal " + v.decal());
      for (String s : List.of(v.castSound(), v.impactSound())) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(s);
        if (rl == null || !net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(rl)) wrong.add(e + " som " + s);
      }
    }
    h.assertTrue(wrong.isEmpty(), "referencias invalidas nos json de elemento: " + wrong);
    h.succeed();
  }
}
