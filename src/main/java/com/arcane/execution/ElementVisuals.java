package com.arcane.execution;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Aparencia de cada elemento, lida de {@code data/arcane/elements/<id>.json} (uma vez, do jar; vale no cliente e no
 * servidor). Campo ausente = o padrao embutido aqui, que reproduz o comportamento anterior a estes arquivos. Nao muda
 * mecanica: as interacoes de dano continuam em {@link SpellElement#mechanic}.
 *
 * <pre>
 * {
 *   "color": "#FF8C28", "secondary": "#FFD24A",
 *   "sprite": "ember",                  // familia de sprites (particles/fx_&lt;familia&gt;.json)
 *   "additive": false,                  // brilho somado (luz, raio) em vez de translucido
 *   "light": 15,                        // 0..15: brilho proprio da particula
 *   "motion": { "gravity": -0.012, "friction": 0.92, "life": [16, 30], "size": 0.1, "jitter": 0.025,
 *               "spiral": 0.0, "flicker": 0.0 },
 *   "decal": "scorch",                  // marca deixada no bloco atingido (particles/fx_decal_&lt;nome&gt;.json)
 *   "sounds": { "cast": "arcane:element.fire.cast", "impact": "arcane:element.fire.impact" }
 * }
 * </pre>
 */
public final class ElementVisuals {
  /**
   * Movimento da particula. gravity &lt; 0 sobe; spiral = radianos por tick girando em volta do eixo vertical;
   * flicker = 0..1, quanto o brilho tremeluz.
   */
  public record Motion(float gravity, float friction, int minLife, int maxLife, float size, float jitter, float spiral, float flicker) {}

  public record Visual(int color, int secondary, String sprite, boolean additive, int light, Motion motion, String decal,
                       String castSound, String impactSound) {}

  private static volatile Map<SpellElement, Visual> cache;

  private ElementVisuals() {}

  public static Visual of(SpellElement element) {
    Map<SpellElement, Visual> c = cache;
    if (c == null) c = load();
    return c.get(element);
  }

  /** Recarrega (testes e /reload de dev). */
  public static synchronized Map<SpellElement, Visual> load() {
    Map<SpellElement, Visual> m = new EnumMap<>(SpellElement.class);
    for (SpellElement e : SpellElement.values()) {
      Visual base = defaults(e);
      String id = e.name().toLowerCase(Locale.ROOT);
      try (var in = ElementVisuals.class.getResourceAsStream("/data/arcane/elements/" + id + ".json")) {
        m.put(e, in == null ? base : parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject(), base));
      } catch (Exception ex) {
        com.mojang.logging.LogUtils.getLogger().error("Elemento {}: json invalido, usando o padrao", id, ex);
        m.put(e, base);
      }
    }
    cache = Map.copyOf(m);
    return cache;
  }

  static Visual parse(JsonObject j, Visual d) {
    Motion dm = d.motion();
    Motion motion = dm;
    if (j.has("motion")) {
      JsonObject mo = j.getAsJsonObject("motion");
      int minLife = dm.minLife(), maxLife = dm.maxLife();
      if (mo.has("life")) {
        var life = mo.getAsJsonArray("life");
        minLife = life.get(0).getAsInt();
        maxLife = life.get(life.size() - 1).getAsInt();
      }
      motion = new Motion(f(mo, "gravity", dm.gravity()), f(mo, "friction", dm.friction()), Math.max(1, minLife),
          Math.max(minLife, maxLife), f(mo, "size", dm.size()), f(mo, "jitter", dm.jitter()), f(mo, "spiral", dm.spiral()),
          Math.max(0f, Math.min(1f, f(mo, "flicker", dm.flicker()))));
    }
    JsonObject sounds = j.has("sounds") ? j.getAsJsonObject("sounds") : new JsonObject();
    return new Visual(color(j, "color", d.color()), color(j, "secondary", d.secondary()), s(j, "sprite", d.sprite()),
        j.has("additive") ? j.get("additive").getAsBoolean() : d.additive(),
        j.has("light") ? Math.max(0, Math.min(15, j.get("light").getAsInt())) : d.light(), motion, s(j, "decal", d.decal()),
        s(sounds, "cast", d.castSound()), s(sounds, "impact", d.impactSound()));
  }

  private static float f(JsonObject o, String k, float d) {
    JsonElement e = o.get(k);
    return e == null ? d : e.getAsFloat();
  }

  private static String s(JsonObject o, String k, String d) {
    JsonElement e = o.get(k);
    return e == null ? d : e.getAsString();
  }

  private static int color(JsonObject o, String k, int d) {
    JsonElement e = o.get(k);
    if (e == null) return d;
    String v = e.getAsString().trim();
    if (v.startsWith("#")) v = v.substring(1);
    return Integer.parseInt(v, 16) & 0xFFFFFF;
  }

  /** O comportamento de antes dos arquivos: cor e particula do enum, perfil de movimento da familia antiga. */
  static Visual defaults(SpellElement e) {
    String sprite = switch (e) {
      case FIRE, BLOOD -> "ember";
      case FROST, WATER -> "frost";
      case STORM, SOUND -> "bolt";
      case VENOM -> "venom";
      case FORCE, EARTH -> "force";
      case VOID, SHADOW -> "void";
      case LIFE, NATURE -> "life";
      default -> "spark";
    };
    String id = e.name().toLowerCase(Locale.ROOT);
    return new Visual(e.defaultColor(), e.defaultSecondary(), sprite, false, 15, motionOf(sprite), "rune",
        "arcane:element." + id + ".cast", "arcane:element." + id + ".impact");
  }

  /** Os perfis que ArcaneSparkParticle usava por tipo de particula. */
  static Motion motionOf(String family) {
    return switch (family) {
      case "ember" -> new Motion(-0.012f, 0.92f, 16, 30, 0.1f, 0.025f, 0f, 0f);
      case "frost" -> new Motion(0.002f, 0.94f, 22, 38, 0.1f, 0.01f, 0f, 0f);
      case "bolt" -> new Motion(0f, 0.8f, 5, 9, 0.16f, 0.05f, 0f, 0f);
      case "venom" -> new Motion(0.03f, 0.9f, 14, 24, 0.09f, 0.015f, 0f, 0f);
      case "force" -> new Motion(0f, 0.88f, 12, 20, 0.13f, 0.06f, 0f, 0f);
      case "void" -> new Motion(0.015f, 0.92f, 18, 32, 0.12f, 0.02f, 0f, 0f);
      case "life" -> new Motion(-0.02f, 0.93f, 18, 30, 0.11f, 0.012f, 0f, 0f);
      default -> new Motion(0f, 0.9f, 14, 26, 0.09f, 0.01f, 0f, 0f);
    };
  }
}
