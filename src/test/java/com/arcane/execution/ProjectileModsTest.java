package com.arcane.execution;

import com.arcane.TestGlyphs;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionCompiler;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.arcane.TestGlyphs.g;
import static org.junit.jupiter.api.Assertions.*;

/** Modificadores de Projetil (Fase J): leitura, orcamento que degrada, poder e trajetoria. */
class ProjectileModsTest {
  @BeforeAll
  static void loadGlyphs() { TestGlyphs.load(); }

  private static CompiledExecutionPlan plan(GlyphInstance... glyphs) {
    return InscriptionCompiler.compile(new Inscription(List.of(glyphs)));
  }

  @Test
  void glyphsWriteTheirParameterIntoTheForm() {
    var p = plan(g("projectile"), g("homing", "turn", 3), g("ricochet", "bounces", 4), g("fragment", "count", 5), g("arc", "height", 6), g("damage"));
    var m = ProjectileMods.of(p.delivery().parameters());
    assertEquals(new ProjectileMods.Mods(3, 4, 5, 6.0), m);
    var defaults = ProjectileMods.of(plan(g("projectile"), g("damage"), g("homing"), g("ricochet"), g("fragment"), g("arc")).delivery().parameters());
    assertEquals(new ProjectileMods.Mods(2, 2, 3, 4.0), defaults, "padroes dos glifos, depois do efeito tambem vale");
    assertFalse(ProjectileMods.of(plan(g("projectile"), g("damage")).delivery().parameters()).any());
    assertTrue(ProjectileMods.present(plan(g("ray"), g("homing"), g("damage")).delivery().parameters()), "o parametro chega na forma Raio (a previa avisa)");
  }

  @Test
  void valuesAreClamped() {
    var m = ProjectileMods.of(Map.of("homing", 9.0, "ricochet", -2.0, "fragment", 1.0, "arc", 99.0));
    assertEquals(3, m.homing());
    assertEquals(0, m.ricochet());
    assertEquals(2, m.fragment(), "Fragmentar vai de 2 a 6");
    assertEquals(16.0, m.arc());
  }

  @Test
  void allFourAtMaxNeverBlowTheBudgetAndDegradeFragmentsFirst() {
    var all = new ProjectileMods.Mods(3, 4, 6, 12);
    assertEquals(11, all.triggers());
    var b = ProjectileMods.budgeted(all, 8);
    assertTrue(b.triggers() <= 8, "dentro do teto: " + b);
    assertEquals(4, b.ricochet(), "os ricochetes ficam; os cacos caem primeiro");
    assertEquals(3, b.fragment());
    assertEquals(3, b.homing());
    assertEquals(12.0, b.arc());
    var tight = ProjectileMods.budgeted(all, 4);
    assertTrue(tight.triggers() <= 4, tight.toString());
    assertEquals(2, tight.fragment(), "fragmenta no minimo em 2, depois corta ricochete");
    assertEquals(1, tight.ricochet());
    var one = ProjectileMods.budgeted(all, 1);
    assertEquals(1, one.triggers(), "teto 1: so o acerto final, nada de erro");
    for (int cap = 1; cap <= 12; cap++) {
      for (int r = 0; r <= 4; r++) for (int f = 0; f <= 6; f++) {
        if (f == 1) continue;
        assertTrue(ProjectileMods.budgeted(new ProjectileMods.Mods(1, r, f, 0), cap).triggers() <= Math.max(1, cap));
      }
    }
  }

  @Test
  void bounceLosesPowerAndFragmentsStayUnderTheCap() {
    assertEquals(1.0, ProjectileMods.bouncePower(0, 0.25), 1e-9);
    assertEquals(0.5625, ProjectileMods.bouncePower(2, 0.25), 1e-9);
    for (int n = 2; n <= 6; n++) {
      double each = ProjectileMods.fragmentPower(n, 1.2, 0.5);
      assertTrue(each * n <= 1.2 + 1e-9, "total dos cacos abaixo do teto (" + n + ")");
      assertTrue(each <= 0.5 + 1e-9);
    }
    assertEquals(0.5, ProjectileMods.fragmentPower(2, 1.2, 0.5), 1e-9, "dois cacos: cada um no maximo metade");
  }

  @Test
  void steeringIsLimitedAndKeepsSpeed() {
    Vec3 v = new Vec3(0, 0, 1.5);
    Vec3 toSide = new Vec3(1, 0, 0);
    double max = Math.toRadians(9);
    Vec3 out = ProjectileMods.steer(v, toSide, max);
    assertEquals(1.5, out.length(), 1e-9, "velocidade mantida");
    double angle = Math.acos(out.normalize().dot(v.normalize()));
    assertEquals(max, angle, 1e-6, "curva no maximo o limite por tick");
    Vec3 close = ProjectileMods.steer(v, new Vec3(0.05, 0, 1), max);
    assertEquals(0.0, Math.acos(Math.min(1, close.normalize().dot(new Vec3(0.05, 0, 1).normalize()))), 1e-6, "perto: aponta direto");
    Vec3 behind = ProjectileMods.steer(v, new Vec3(0, 0, -1), max);
    assertEquals(1.5, behind.length(), 1e-9, "alvo atras: ainda vira, sem NaN");
    assertEquals(Math.toRadians(3), ProjectileMods.turnPerTick(1, 3.0), 1e-12);
  }

  @Test
  void coneAndReflect() {
    Vec3 o = Vec3.ZERO, f = new Vec3(0, 0, 1);
    assertTrue(ProjectileMods.inCone(o, f, new Vec3(2, 0, 5), 35, 16));
    assertFalse(ProjectileMods.inCone(o, f, new Vec3(5, 0, 2), 35, 16), "fora do cone");
    assertFalse(ProjectileMods.inCone(o, f, new Vec3(0, 0, -5), 35, 16), "atras");
    assertFalse(ProjectileMods.inCone(o, f, new Vec3(0, 0, 20), 35, 16), "longe demais");
    Vec3 r = ProjectileMods.reflect(new Vec3(0.3, -1, 0.5), new Vec3(0, 1, 0));
    assertEquals(new Vec3(0.3, 1, 0.5), r);
  }

  @Test
  void arcLandsOnTheAimedPointAndClearsCover() {
    Vec3 from = new Vec3(0, 2, 0), to = new Vec3(0, 0, 20);
    double g = ProjectileMods.ARC_GRAVITY;
    Vec3 v0 = ProjectileMods.arcLaunch(from, to, 4, g);
    // tempo total: subida ate o topo e queda ate o alvo
    double apex = 2 + 4;
    double t = v0.y / g + Math.sqrt(2 * (apex - to.y) / g);
    int ticks = (int) Math.round(t);
    Vec3 land = ProjectileMods.arcAt(from, v0, g, ticks);
    assertTrue(land.distanceTo(to) < 1.0, "cai perto do ponto mirado: " + land);
    // passa por cima de um muro de 3 blocos de altura no meio do caminho
    Vec3 mid = ProjectileMods.arcAt(from, v0, g, ticks / 2);
    assertTrue(mid.y > 3.0, "no meio do caminho, acima do muro: " + mid);
    double peak = 0;
    for (int i = 0; i <= ticks; i++) peak = Math.max(peak, ProjectileMods.arcAt(from, v0, g, i).y);
    assertTrue(peak >= apex - 0.5 && peak <= apex + 0.5, "topo na altura pedida (passo discreto passa um pouco): " + peak);
  }
}
