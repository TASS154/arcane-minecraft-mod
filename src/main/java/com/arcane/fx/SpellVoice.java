package com.arcane.fx;

import com.arcane.execution.SpellElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Voz da magia (Fase L): cada inscricao vira uma pequena melodia, igual sempre que for a mesma inscricao. Funcao pura e
 * deterministica: o servidor compoe, empacota em dois {@code long} ({@link #pack}) e manda no pacote de efeito; o
 * cliente toca ({@code client.VoicePlayer}). NAO ouvido em jogo.
 *
 * <ul>
 *   <li>Grau da escala pentatonica maior (0, 2, 4, 7, 9 semitons): hash estavel (FNV-1a) do id do glifo.</li>
 *   <li>Oitava pela categoria: forma grave, efeito no meio, modificador agudo (filtro e condicao no meio).</li>
 *   <li>No maximo 8 notas: mais que isso, ficam a primeira, a ultima e as mais pesadas (custo), na ordem original.</li>
 *   <li>Tom sempre entre 0,5 e 2,0 (o limite do Minecraft): nota fora do registro dobra de oitava para dentro.</li>
 *   <li>Rituais e magias grandes: mais uma quinta, uma oitava e uma cauda longa.</li>
 * </ul>
 */
public final class SpellVoice {
  private SpellVoice() {}

  public static final int MAX_NOTES = 8;
  /** Notas extras de magia grande (quinta e oitava) alem das 8. */
  public static final int MAX_WITH_FLOURISH = MAX_NOTES + 2;
  public static final int[] PENTATONIC = {0, 2, 4, 7, 9};
  /** Ticks entre notas seguidas. */
  public static final int STEP_TICKS = 3;
  public static final float MIN_PITCH = 0.5f, MAX_PITCH = 2.0f;

  /** Uma nota: grau (0-4), oitava (0 grave, 1 meio, 2 aguda), atraso em ticks, volume (0-1) e cauda longa. */
  public record Note(int degree, int octave, int delay, float volume, boolean longTail) {
    public int semitone() { return PENTATONIC[degree] + 12 * (octave - 1); }

    /** Tom de tocar (0,5 a 2,0): dobra a oitava para dentro do registro. */
    public float pitch() {
      int s = semitone();
      while (s > 12) s -= 12;
      while (s < -12) s += 12;
      return (float) Math.max(MIN_PITCH, Math.min(MAX_PITCH, Math.pow(2.0, s / 12.0)));
    }
  }

  /** Categoria de um glifo para a voz (o resto conta como efeito). */
  public enum Kind { FORM, EFFECT, MODIFIER }

  /** FNV-1a de 32 bits sobre o UTF-8 do id: o mesmo em toda JVM (String.hashCode tambem seria, mas este espalha melhor). */
  public static int hash(String id) {
    int h = 0x811C9DC5;
    for (byte b : id.getBytes(StandardCharsets.UTF_8)) {
      h ^= (b & 0xFF);
      h *= 0x01000193;
    }
    return h;
  }

  public static int degreeOf(String glyphId) { return Math.floorMod(hash(glyphId), PENTATONIC.length); }

  public static int octaveOf(Kind kind) {
    return switch (kind) {
      case FORM -> 0;
      case EFFECT -> 1;
      case MODIFIER -> 2;
    };
  }

  /** Compoe com a categoria e o peso de cada glifo dados (testes e o preview_voice.py usam a mesma regra). */
  public static List<Note> compose(List<String> glyphIds, Function<String, Kind> kindOf, ToIntFunction<String> weightOf,
                                   SpellElement dominant, double power, boolean big) {
    List<Integer> keep = keptIndices(glyphIds, weightOf);
    float base = (float) Math.max(0.35, Math.min(1.0, 0.55 + 0.15 * power));
    List<Note> out = new ArrayList<>();
    int step = 0;
    for (int i : keep) {
      String id = glyphIds.get(i);
      Kind k = kindOf.apply(id);
      float vol = base * (k == Kind.EFFECT ? 1.0f : k == Kind.FORM ? 0.9f : 0.7f);
      out.add(new Note(degreeOf(id), octaveOf(k), step * STEP_TICKS, Math.min(1f, vol), false));
      step++;
    }
    if (big && !out.isEmpty()) {
      Note last = out.get(out.size() - 1);
      // quinta acima da ultima nota (grau +3 na pentatonica = quinta justa a partir da tonica) e a oitava da primeira
      out.add(new Note(Math.min(4, (last.degree() + 3) % 5), last.octave(), last.delay() + STEP_TICKS, last.volume(), false));
      Note first = out.get(0);
      out.add(new Note(first.degree(), Math.min(2, first.octave() + 1), last.delay() + STEP_TICKS, first.volume(), true));
    }
    return out;
  }

  /** Indices mantidos: todos ate 8; acima disso, o primeiro, o ultimo e os de maior peso (empate: o mais cedo). */
  static List<Integer> keptIndices(List<String> ids, ToIntFunction<String> weightOf) {
    List<Integer> all = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) all.add(i);
    if (ids.size() <= MAX_NOTES) return all;
    List<Integer> middle = new ArrayList<>(all.subList(1, ids.size() - 1));
    middle.sort((a, b) -> {
      int c = Integer.compare(weightOf.applyAsInt(ids.get(b)), weightOf.applyAsInt(ids.get(a)));
      return c != 0 ? c : Integer.compare(a, b);
    });
    List<Integer> chosen = new ArrayList<>(middle.subList(0, MAX_NOTES - 2));
    chosen.add(0);
    chosen.add(ids.size() - 1);
    chosen.sort(Integer::compare);
    return chosen;
  }

  /** Compoe com o registro de glifos (categoria e custo de cada um). */
  public static List<Note> compose(List<String> glyphIds, SpellElement dominant, double power) {
    return compose(glyphIds, dominant, power, false);
  }

  public static List<Note> compose(List<String> glyphIds, SpellElement dominant, double power, boolean big) {
    return compose(glyphIds, SpellVoice::kindFromRegistry, SpellVoice::weightFromRegistry, dominant, power, big);
  }

  static Kind kindFromRegistry(String id) {
    var def = com.arcane.glyph.GlyphRegistry.get(new net.minecraft.resources.ResourceLocation("arcane", id));
    if (def.isEmpty()) return Kind.EFFECT;
    return switch (def.get().category()) {
      case FORM -> Kind.FORM;
      case MODIFIER -> Kind.MODIFIER;
      default -> Kind.EFFECT;
    };
  }

  static int weightFromRegistry(String id) {
    return com.arcane.glyph.GlyphRegistry.get(new net.minecraft.resources.ResourceLocation("arcane", id))
        .map(d -> d.cost().mana()).orElse(1);
  }

  /** Maestria alta (Fase M): um acorde limpo (terca da pentatonica) junto da ultima nota. So som. */
  public static List<Note> withMastery(List<Note> notes, double mastery) {
    if (notes.isEmpty() || !CastStyle.polished(mastery) || notes.size() >= 12) return notes;
    List<Note> out = new ArrayList<>(notes);
    Note last = notes.get(notes.size() - 1);
    out.add(new Note((last.degree() + 2) % 5, last.octave(), last.delay(), last.volume() * 0.7f, last.longTail()));
    return out;
  }

  // ------------------------------------------------------------------ empacotar (pacote de efeito)
  /**
   * Cada nota em 15 bits: grau (3), oitava (2), atraso/3 (4), volume em 16 passos (4), cauda (1), mais 1 bit de "ha
   * nota". Ate 4 notas por {@code long}; 10 notas cabem em 3 longs, entao o pacote leva {@code long[3]}.
   */
  public static long[] pack(List<Note> notes) {
    long[] out = new long[3];
    for (int i = 0; i < Math.min(notes.size(), 12); i++) {
      Note n = notes.get(i);
      long bits = 1L
          | ((long) (n.degree() & 7) << 1)
          | ((long) (n.octave() & 3) << 4)
          | ((long) (Math.min(15, n.delay() / STEP_TICKS) & 15) << 6)
          | ((long) (Math.round(n.volume() * 15) & 15) << 10)
          | ((n.longTail() ? 1L : 0L) << 14);
      out[i / 4] |= bits << ((i % 4) * 16);
    }
    return out;
  }

  public static List<Note> unpack(long[] packed) {
    List<Note> out = new ArrayList<>();
    for (int i = 0; i < packed.length * 4; i++) {
      long bits = (packed[i / 4] >>> ((i % 4) * 16)) & 0xFFFF;
      if ((bits & 1) == 0) break;
      int degree = (int) ((bits >>> 1) & 7);
      out.add(new Note(Math.min(4, degree), (int) ((bits >>> 4) & 3), (int) ((bits >>> 6) & 15) * STEP_TICKS,
          ((bits >>> 10) & 15) / 15f, ((bits >>> 14) & 1) == 1));
    }
    return out;
  }

  /** Familia da amostra sustentada pelo elemento dominante (uma amostra por familia em sounds.json). */
  public static String family(SpellElement e) {
    return switch (e) {
      case FIRE, BLOOD -> "fire";
      case FROST, WATER -> "frost";
      case STORM, SOUND -> "storm";
      case VENOM, LIFE, NATURE -> "life";
      case FORCE, EARTH -> "earth";
      case VOID, SHADOW -> "void";
      default -> "arcane"; // ARCANE, LIGHT, TIME
    };
  }

  public static final List<String> FAMILIES = List.of("arcane", "fire", "frost", "storm", "life", "earth", "void");
}
