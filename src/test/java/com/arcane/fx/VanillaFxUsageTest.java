package com.arcane.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lista os usos de particulas e sons do jogo base (ParticleTypes.X, SoundEvents.X) que ainda nao passam pela fachada
 * {@link Fx}. Grava o relatorio em build/reports/vanilla_fx.txt. Os caminhos quentes (conjuracao, projetil, Fogo,
 * Gelo, Raio, impacto, aura) nao podem ter nenhum.
 */
class VanillaFxUsageTest {
  private static final Pattern USE = Pattern.compile("\\b(ParticleTypes|SoundEvents)\\.([A-Z_]+)");

  /** Caminhos quentes: tudo aqui ja usa a fachada. */
  private static final List<String> HOT_PATHS = List.of(
      "execution/CastingService.java", "entity/ArcaneProjectileEntity.java",
      "execution/handlers/FireEffectHandler.java", "execution/handlers/IgniteEffectHandler.java",
      "execution/handlers/FreezeEffectHandler.java", "execution/handlers/ThunderEffectHandler.java",
      "execution/Region.java", "execution/Targeting.java", "execution/ElementInteractions.java",
      "execution/ElectricTrail.java", "execution/Zones.java",
      "client/render/ShapeFxRenderer.java", "client/render/BeamRenderer.java");

  @Test
  void hotPathsUseOnlyTheFxFacadeAndTheRestIsListed() throws IOException {
    Path root = Path.of("src/main/java/com/arcane");
    Map<String, List<String>> uses = new TreeMap<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String rel = root.relativize(f).toString().replace('\\', '/');
        if (rel.startsWith("gametest/")) continue;
        List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
          String line = lines.get(i).trim();
          if (line.startsWith("import ") || line.startsWith("//") || line.startsWith("*")) continue;
          Matcher m = USE.matcher(line);
          while (m.find()) uses.computeIfAbsent(rel, k -> new ArrayList<>()).add((i + 1) + ": " + m.group(1) + "." + m.group(2));
        }
      }
    }
    StringBuilder report = new StringBuilder("Usos de particulas e sons do jogo base fora da fachada Fx\n\n");
    int total = 0;
    for (var e : uses.entrySet()) {
      report.append(e.getKey()).append(" (").append(e.getValue().size()).append(")\n");
      for (String u : e.getValue()) report.append("  ").append(u).append('\n');
      total += e.getValue().size();
    }
    report.append("\nTotal: ").append(total).append(" usos em ").append(uses.size()).append(" arquivos\n");
    Path out = Path.of("build/reports/vanilla_fx.txt");
    Files.createDirectories(out.getParent());
    Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
    System.out.println("[vanilla-fx] " + total + " usos em " + uses.size() + " arquivos (relatorio em " + out + ")");

    List<String> hot = new ArrayList<>();
    for (String h : HOT_PATHS) if (uses.containsKey(h)) hot.add(h + " " + uses.get(h));
    assertTrue(hot.isEmpty(), "caminho quente com particula/som do jogo base: " + hot);
  }
}
