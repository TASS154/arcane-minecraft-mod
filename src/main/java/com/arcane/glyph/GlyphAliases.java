package com.arcane.glyph;

import com.arcane.inscription.GlyphInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ids de glifo antigos que foram renomeados, fundidos ou cortados com destino. Tudo que le um id salvo (inscricoes no
 * grimorio, no mundo, em itens e altares; conhecimento do grimorio; notas de glifo; estudo na bancada; comandos) passa por
 * aqui, entao grimorios e inscricoes antigos continuam validos depois de uma auditoria.
 *
 * <p>Um alias pode fixar parametros no destino (fusao com {@code kind}: "incendiar" vira "fogo" com {@code kind = 1}). Os
 * parametros fixados so entram se a inscricao antiga nao os tiver. Cadeias (a -> b -> c) sao seguidas.
 *
 * <p>Os aliases aprovados ficam em {@link #APPROVED} (ver docs/AUDITORIA_GLIFOS.md, "Mapa de aliases"). Glifo cortado SEM
 * alias faz as magias antigas que o usam falharem com "glifo desconhecido": por isso todo corte aprovado entra aqui.
 *
 * <p><b>Dropado:</b> um glifo cortado sem substituto aponta para {@link #DROPPED}. Ao ler uma inscricao salva, ele e
 * retirado da lista (o resto da magia fica como estava, na mesma ordem); o conhecimento dele no grimorio e descartado.
 * Um alias tambem pode descartar parametros que o destino nao tem ({@code droppedParams}).
 */
public final class GlyphAliases {
  public record Alias(ResourceLocation target, Map<String, Double> fixedParams, Set<String> droppedParams) {
    public Alias {
      fixedParams = Map.copyOf(fixedParams);
      droppedParams = Set.copyOf(droppedParams);
    }

    public Alias(ResourceLocation target, Map<String, Double> fixedParams) {
      this(target, fixedParams, Set.of());
    }
  }

  /** Destino de um glifo cortado sem substituto: sai da inscricao ao carregar. */
  public static final ResourceLocation DROPPED = new ResourceLocation("arcane", "dropped_glyph");

  private static Alias to(String id) {
    return new Alias(new ResourceLocation("arcane", id), Map.of());
  }

  /**
   * Aliases aprovados (decisoes de Everton, prompt de 26 nov; tabela em docs/AUDITORIA_GLIFOS.md). Nunca reaproveite um
   * id daqui para um glifo novo: as magias salvas com o id antigo virariam o glifo novo.
   */
  private static final Map<String, Alias> APPROVED = Map.of(
      // Planar era Pena mais cara e mais curta: a duracao escrita cabe na Pena (20 a 24.000)
      "glide", to("feather"),
      // Atraso compilava igual a Apos Segundos (mesmo parametro seconds); a magia fica 1 de mana mais barata
      "delay", to("after_seconds"),
      // Ferido (filtro) cortado sem substituto: a magia perde o filtro e volta a atingir todos os alvos da forma
      "wounded", new Alias(DROPPED, Map.of()),
      // Inverter cortado: o efeito anterior fica como foi escrito (sem a troca pelo oposto)
      "invert", new Alias(DROPPED, Map.of()),
      // efeito Chuva cortado (vai nascer a forma Chuva, com outro id): o mais proximo e Molhar, que fica com a duracao
      "rain", new Alias(new ResourceLocation("arcane", "wet"), Map.of(), Set.of("radius")),
      // Carinho (curava 4 HP em animais) funde em Cura com os mesmos 4 HP; Cura vale para qualquer criatura
      "pet", new Alias(new ResourceLocation("arcane", "heal"), Map.of("amount", 4.0)));

  private static final Map<ResourceLocation, Alias> ALIASES = new ConcurrentHashMap<>();
  private static final int MAX_CHAIN = 8;

  static {
    APPROVED.forEach((from, alias) -> ALIASES.put(new ResourceLocation("arcane", from), alias));
  }

  private GlyphAliases() {}

  /** O id atual de um id salvo (ele mesmo, se nao for antigo; {@link #DROPPED} se foi cortado sem substituto). */
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
      if (!alias.fixedParams().isEmpty() || !alias.droppedParams().isEmpty()) {
        if (params == null) params = new HashMap<>(instance.parameters());
        for (String k : alias.droppedParams()) params.remove(k);
        for (var e : alias.fixedParams().entrySet()) params.putIfAbsent(e.getKey(), e.getValue());
      }
      current = alias.target();
    }
    if (current.equals(instance.glyphId())) return instance;
    return new GlyphInstance(current, params == null ? instance.parameters() : Map.copyOf(params));
  }

  /** Se o id salvo foi cortado sem substituto (a inscricao o retira ao carregar). */
  public static boolean isDropped(ResourceLocation id) {
    return DROPPED.equals(resolve(id));
  }

  /** Migra uma lista salva: troca ids antigos e retira os dropados, mantendo a ordem do resto. */
  public static java.util.List<GlyphInstance> migrateAll(java.util.List<GlyphInstance> glyphs) {
    java.util.List<GlyphInstance> out = new java.util.ArrayList<>(glyphs.size());
    for (GlyphInstance g : glyphs) {
      GlyphInstance m = migrate(g);
      if (!DROPPED.equals(m.glyphId())) out.add(m);
    }
    return out.size() == glyphs.size() && out.equals(glyphs) ? glyphs : java.util.List.copyOf(out);
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
