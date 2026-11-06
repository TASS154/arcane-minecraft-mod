package com.arcane.client.anim;

import java.util.List;

/**
 * Clipes de animacao do corpo do jogador (graus). Cada clipe e uma lista de quadros-chave ENCADEADOS: cada quadro leva
 * {@code ticks} para ir da pose anterior ate a sua. Convencao do modelo: bracos pendurados = 0; x negativo levanta o
 * braco para frente (-90 horizontal, -180 acima da cabeca); z abre o braco para o lado; y gira para dentro/fora.
 */
public final class PlayerClips {
  /** Pose alvo: braco direito (x,y,z), braco esquerdo (x,y,z), giro do tronco, inclinacao do tronco, cabeca (x). */
  public record Key(int ticks, float rx, float ry, float rz, float lx, float ly, float lz, float twist, float lean, float head) {}

  public record Clip(String name, List<Key> keys) {
    public int total() {
      int t = 0;
      for (Key k : keys) t += k.ticks();
      return t;
    }
  }

  public static final Clip CAST_RIGHT = new Clip("cast_right", List.of(
      new Key(3, -150, -15, 20, 8, 0, -4, -18, -4, -10),      // recua o braco para cima (preparo)
      new Key(3, -92, 8, 4, 8, 0, -4, 22, 6, 4),               // estoca para frente
      new Key(5, -90, 4, 2, 10, 0, -6, 18, 6, 2),              // segura
      new Key(6, 4, 0, 4, 2, 0, -2, 0, 0, 0)));                // volta

  public static final Clip CAST_LEFT = new Clip("cast_left", List.of(
      new Key(3, 8, 0, 4, -150, 15, -20, 18, -4, -10),
      new Key(3, 8, 0, 4, -92, -8, -4, -22, 6, 4),
      new Key(5, 10, 0, 6, -90, -4, -2, -18, 6, 2),
      new Key(6, 2, 0, 2, 4, 0, -4, 0, 0, 0)));

  /** Terceiro golpe do combo: as duas maos sobem, descem com forca e o corpo cai junto. */
  public static final Clip CAST_FINISH = new Clip("cast_finish", List.of(
      new Key(4, -160, -10, 14, -160, 10, -14, 0, -10, -18),   // maos acima da cabeca
      new Key(2, -170, 0, 8, -170, 0, -8, 0, -14, -22),        // tensiona
      new Key(3, -85, 0, 6, -85, 0, -6, 0, 24, 14),            // desce forte
      new Key(6, -80, 2, 8, -80, -2, -8, 0, 22, 10),           // segura inclinado
      new Key(8, 3, 0, 4, 3, 0, -4, 0, 0, 0)));

  /** Magia em si mesmo: cruza os bracos no peito e abre para cima. */
  public static final Clip SELF = new Clip("self", List.of(
      new Key(4, -55, -50, 0, -55, 50, 0, 0, 4, 12),
      new Key(5, -170, 0, 35, -170, 0, -35, 0, -6, -22),
      new Key(6, -165, 0, 30, -165, 0, -30, 0, -4, -18),
      new Key(7, 3, 0, 4, 3, 0, -4, 0, 0, 0)));

  /** Aura, onda e pilar: abre os bracos para os lados e bate as palmas para baixo. */
  public static final Clip AREA = new Clip("area", List.of(
      new Key(4, -20, 0, 95, -20, 0, -95, 0, -4, -8),
      new Key(3, -100, 0, 15, -100, 0, -15, 0, 18, 10),
      new Key(6, -60, 0, 12, -60, 0, -12, 0, 16, 8),
      new Key(8, 3, 0, 4, 3, 0, -4, 0, 0, 0)));

  /** Ritual, em tres tempos: bracos abertos balancando, palmas juntas, e golpe contra o chao. */
  public static final Clip RITUAL = new Clip("ritual", List.of(
      new Key(8, -150, 0, 70, -150, 0, -70, 0, -8, -24),
      new Key(8, -135, 0, 55, -165, 0, -85, 6, -8, -24),
      new Key(8, -165, 0, 85, -135, 0, -55, -6, -8, -24),
      new Key(5, -100, -75, 0, -100, 75, 0, 0, 6, 0),
      new Key(3, -175, 0, 10, -175, 0, -10, 0, -14, -28),
      new Key(4, -70, 0, 6, -70, 0, -6, 0, 30, 18),
      new Key(10, 3, 0, 4, 3, 0, -4, 0, 0, 0)));

  private static final Clip[] BY_ID = {CAST_RIGHT, CAST_LEFT, CAST_FINISH, SELF, AREA, RITUAL};

  private PlayerClips() {}

  public static Clip byId(int id) {
    return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
  }

  private static float smooth(float t) {
    t = Math.max(0f, Math.min(1f, t));
    return t * t * (3f - 2f * t);
  }

  /**
   * Pose no instante {@code t} (ticks desde o inicio) em {@code out[0..9]} (mesma ordem de {@link Key});
   * retorna o peso de mistura com a pose normal (0 a 1), que sobe no comeco e desce no fim.
   */
  public static float sample(Clip clip, float t, float[] out) {
    float total = clip.total();
    if (t < 0 || t >= total) return 0f;
    float start = 0;
    Key prev = null;
    for (Key k : clip.keys()) {
      if (t < start + k.ticks()) {
        float a = smooth((t - start) / k.ticks());
        float[] to = {k.rx, k.ry, k.rz, k.lx, k.ly, k.lz, k.twist, k.lean, k.head};
        float[] from = prev == null ? to : new float[] {prev.rx, prev.ry, prev.rz, prev.lx, prev.ly, prev.lz, prev.twist, prev.lean, prev.head};
        for (int i = 0; i < 9; i++) out[i] = from[i] + (to[i] - from[i]) * (prev == null ? 1f : a);
        break;
      }
      start += k.ticks();
      prev = k;
    }
    return smooth(t / 2f) * smooth((total - t) / 3f);
  }
}
