package com.arcane.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import javax.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ponto de extensao para protecao de terreno de outros mods (claims, regioes). O Arcane pergunta antes de uma magia
 * mexer no mundo ({@code com.arcane.execution.Targeting#mayAlterWorld}) ou quebrar blocos ({@code #mayBreakBlocks}).
 * Sem provedor registrado, nada muda (padrao: permite). Basta um provedor negar.
 *
 * <p>Registre no setup comum do seu mod: {@code ProtectionProvider.register((level, caster, pos, kind) -> ...)}.
 */
@FunctionalInterface
public interface ProtectionProvider {
  /** O que a magia quer fazer: mexer no mundo (gelo, fogo, luz, barreira) ou quebrar blocos (cratera, cavar). */
  enum Kind { ALTER, BREAK }

  /**
   * @param caster quem conjura (null em magias sem dono, como runas antigas)
   * @param pos    o bloco afetado, ou a posicao de quem conjura quando a magia ainda nao sabe o bloco
   * @return false para negar
   */
  boolean allows(ServerLevel level, @Nullable LivingEntity caster, BlockPos pos, Kind kind);

  List<ProtectionProvider> PROVIDERS = new CopyOnWriteArrayList<>();

  static void register(ProtectionProvider provider) { PROVIDERS.add(provider); }

  static void unregister(ProtectionProvider provider) { PROVIDERS.remove(provider); }

  /** Todos os provedores permitem? (sem nenhum: sim) */
  static boolean allowedByAll(ServerLevel level, @Nullable LivingEntity caster, BlockPos pos, Kind kind) {
    for (ProtectionProvider p : PROVIDERS) if (!p.allows(level, caster, pos, kind)) return false;
    return true;
  }
}
