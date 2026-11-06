package com.arcane.execution;

import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escolhe, no servidor, qual animacao o jogador faz (so visual; a mecanica nao depende dela). Os ids sao os mesmos de
 * {@code PlayerClips} no cliente. Conjuracoes direcionais seguidas formam um COMBO encadeado:
 * 1a mao direita, 2a mao esquerda, 3a o golpe final com as duas maos; passar de 2 s sem conjurar reinicia a sequencia.
 */
public final class PlayerAnims {
  public static final int CAST_RIGHT = 0, CAST_LEFT = 1, CAST_FINISH = 2, SELF = 3, AREA = 4, RITUAL = 5;
  public static final int COMBO_WINDOW_TICKS = 40;

  private record Combo(long lastTick, int step) {}

  private static final Map<UUID, Combo> COMBOS = new ConcurrentHashMap<>();

  private PlayerAnims() {}

  /** Passo do combo (0, 1, 2) para uma conjuracao direcional feita em {@code now}; atualiza o estado. */
  static int nextComboStep(UUID id, long now) {
    Combo prev = COMBOS.get(id);
    int step = prev != null && now - prev.lastTick() <= COMBO_WINDOW_TICKS ? (prev.step() + 1) % 3 : 0;
    COMBOS.put(id, new Combo(now, step));
    return step;
  }

  /** Clipe de uma forma: Eu, areas, ou direcional (com combo). */
  public static int clipFor(UUID id, long now, CompiledExecutionPlan plan) {
    String form = DeliverySpec.fromPlan(plan.delivery()).handler().getPath();
    return switch (form) {
      case "self" -> SELF;
      case "aura", "wave", "pillar" -> AREA;
      default -> switch (nextComboStep(id, now)) {
        case 0 -> CAST_RIGHT;
        case 1 -> CAST_LEFT;
        default -> CAST_FINISH;
      };
    };
  }

  public static void play(ServerPlayer player, int clip) {
    ModNetwork.sendPlayerAnim(player, clip);
  }

  public static void onCast(ServerPlayer player, CompiledExecutionPlan plan) {
    play(player, clipFor(player.getUUID(), player.level().getGameTime(), plan));
  }

  public static void forget(UUID id) {
    COMBOS.remove(id);
  }
}
