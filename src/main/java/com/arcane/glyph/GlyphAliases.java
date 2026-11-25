package com.arcane.glyph;

import com.arcane.inscription.GlyphInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ids de glifo antigos que foram renomeados, fundidos ou cortados com destino. Tudo que le um id salvo (inscricoes no
 * grimorio, no mundo, em itens e altares; conhecimento do grimorio; notas de glifo; estudo na bancada; comandos) passa por
 * aqui, entao grimorios e inscricoes antigos continuam validos depois de uma auditoria.
 *
 * <p>Um alias pode fixar parametros no destino (fusao com {@code kind}: "incendiar" vira "fogo" com {@code kind = 1}). Os
 * parametros fixados so entram se a inscricao antiga nao os tiver. Cadeias (a -> b -> c) sao seguidas.
 *
 * <p>Os aliases aprovados ficam em {@link #APPROVED} (ver docs/AUDITORIA_GLIFOS.md). Glifo cortado SEM alias faz as magias
 * antigas que o usam falharem com "glifo desconhecido": por isso todo corte aprovado entra aqui.
 */
public final class GlyphAliases {
  public record Alias(ResourceLocation target, Map<String, Double> fixedParams) {
    public Alias {
      fixedParams = Map.copyOf(fixedParams);
    }
  }

  /** Aliases aprovados na auditoria de glifos. Vazio ate a aprovacao (docs/AUDITORIA_GLIFOS.md). */
  private static final Map<String, Alias> APPROVED = Map.of();

  private static final Map<ResourceLocation, Alias> ALIASES = new ConcurrentHashMap<>();
  private static final int MAX_CHAIN = 8;

  static {
    APPROVED.forEach((from, alias) -> ALIASES.put(new ResourceLocation("arcane", from), alias));
  }

  private GlyphAliases() {}

  /** O id atual de um id salvo (ele mesmo, se nao for antigo). */
  public static ResourceLocation resolve(ResourceLocation id) {
    ResourceLocation current = id;
    for (int i = 0; i < MAX_CHAIN; i++) {
      Alias alias = ALIASES.get(current);
      if (alias == null) return current;
      current = alias.target();
    }
    return current;
  }

  /** Converte uma instancia salva: troca o id e acrescenta os parametros fixados pelo alias (sem sobrescrever). */
  public static GlyphInstance migrate(GlyphInstance instance) {
    ResourceLocation current = instance.glyphId();
    Map<String, Double> params = null;
    for (int i = 0; i < MAX_CHAIN; i++) {
      Alias alias = ALIASES.get(current);
      if (alias == null) break;
      if (!alias.fixedParams().isEmpty()) {
        if (params == null) params = new HashMap<>(instance.parameters());
        for (var e : alias.fixedParams().entrySet()) params.putIfAbsent(e.getKey(), e.getValue());
      }
      current = alias.target();
    }
    if (current.equals(instance.glyphId())) return instance;
    return new GlyphInstance(current, params == null ? instance.parameters() : Map.copyOf(params));
  }

  public static boolean isAlias(ResourceLocation id) {
    return ALIASES.containsKey(id);
  }

  /** Todos os aliases ativos (para docs e testes). */
  public static Map<ResourceLocation, Alias> all() {
    return Map.copyOf(ALIASES);
  }

  /** Registra um alias em tempo de execucao (testes; nao use para aliases definitivos: ponha em {@link #APPROVED}). */
  public static void register(ResourceLocation from, ResourceLocation to, Map<String, Double> fixedParams) {
    if (from.equals(to)) throw new IllegalArgumentException("alias para si mesmo: " + from);
    ALIASES.put(from, new Alias(to, fixedParams));
  }

  public static void unregister(ResourceLocation from) {
    ALIASES.remove(from);
  }
}
