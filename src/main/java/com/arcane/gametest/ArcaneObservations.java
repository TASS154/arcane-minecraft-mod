package com.arcane.gametest;

import com.arcane.ArcaneMod;
import com.arcane.execution.CastingService;
import com.arcane.execution.SpellPreview;
import com.arcane.execution.Zones;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bateria de OBSERVACAO: conjura dezenas de combinacoes num mundo real, espera o efeito acontecer e anota o que mudou
 * (vida, fogo, efeitos, posicao, blocos, entidades novas). Nao afirma nada: o resultado vai para run/observacoes.md.
 * So roda com -Darcane.observe=true (nas execucoes normais termina na hora).
 */
@GameTestHolder(ArcaneMod.MODID)
@PrefixGameTestTemplate(false)
public class ArcaneObservations {
  private static final int W = 15, H = 8;

  private record Combo(String scenario, String spell, int ticks, float pitch) {}

  private static final List<Combo> COMBOS = new ArrayList<>();

  private static void m(String spell) { COMBOS.add(new Combo("mobs", spell, 30, 0)); }
  private static void mz(String spell) { COMBOS.add(new Combo("mobs", spell, 70, 0)); }
  private static void b(String scenario, String spell) { COMBOS.add(new Combo(scenario, spell, 30, 0)); }

  static {
    // ---- criaturas: vaca a 4 blocos a frente, ovelha ao lado, husk (hostil) atras
    for (String e : new String[]{"damage", "fire", "heal", "poison", "slow", "push", "attract", "mark", "thunder", "explode",
        "freeze", "gravity", "cut", "blind", "wither", "levitate", "speed", "shield", "invisible"}) m("ray," + e);
    for (String e : new String[]{"damage", "push", "attract", "freeze", "gravity", "poison", "heal", "fire"}) m("aura," + e);
    m("wave,damage");
    m("wave,push");
    m("wave,shape_sphere,damage");
    m("aura,shape_cone,damage");
    m("aura,shape_column,damage");
    m("aura,hostile,damage");
    m("aura,animals,damage");
    m("aura,nearest,damage");
    m("aura,area,damage");
    m("aura,power,damage");
    m("ray,power,damage");
    m("ray,weaken,damage");
    m("ray,damage,damage");
    m("projectile,damage");
    m("projectile,explode");
    m("projectile,fire");
    m("touch,damage");
    m("pillar,damage");
    m("pillar,thunder");
    m("pillar,origin_self,damage");
    m("pillar,origin_aim,fire");
    m("self,speed");
    m("self,shield");
    m("self,blink");
    m("self,levitate");
    m("self,heal");
    mz("aura,zone@seconds=2,damage@amount=1");
    mz("aura,zone@seconds=2,fire");
    mz("aura,zone@seconds=2,on_zone_end,explode");
    mz("aura,zone@seconds=2,heal");
    mz("projectile,zone@seconds=2,damage@amount=1");
    // ---- blocos
    b("glass", "ray,damage");
    b("glass", "ray,cut");
    b("glass", "ray,explode");
    b("ice", "ray,fire");
    b("ice", "ray,freeze");
    b("tnt", "ray,fire");
    b("tnt", "ray,ignite");
    b("grass", "ray,heal");
    b("grass", "ray,ignite");
    b("grass", "ray,poison");
    b("grass", "ray,push");
    b("sand", "ray,gravity");
    b("stone", "ray,dig");
    b("stone", "ray,explode");
    b("stone", "ray,ignite");
    b("stone", "ray,damage");
    b("stone", "ray,freeze");
    b("water", "ray,freeze");
    // ---- ponto vazio: arena sem nada na mira; o raio de alcance 8 termina no ar sobre o chao
    for (String e : new String[]{"fire", "ignite", "explode", "thunder", "poison", "freeze", "heal", "damage"}) {
      COMBOS.add(new Combo("open", "ray@range=8," + e, 30, 0));
    }
    COMBOS.add(new Combo("open", "ray," + "fire", 30, 0));
    for (String e : new String[]{"push", "poison", "damage", "attract", "heal", "explode"}) {
      COMBOS.add(new Combo("openmob", "ray@range=8," + e, 30, 0));
    }
    COMBOS.add(new Combo("open", "pillar@range=8,fire", 30, 0));
    COMBOS.add(new Combo("open", "pillar@range=8,thunder", 30, 0));
    COMBOS.add(new Combo("openmob", "pillar@range=8,damage", 30, 0));
    COMBOS.add(new Combo("openmob", "pillar@range=8,push", 30, 0));
    COMBOS.add(new Combo("open", "pillar@range=8,origin_self,fire", 30, 0));
    COMBOS.add(new Combo("open", "pillar,origin_self,poison", 30, 0));
    // ---- combinacoes mistas, juramentos, filtros, condicoes
    m("ray,damage,fire");
    m("ray,fire,heal");
    m("ray,damage,fire,poison");
    m("aura,attract,damage");
    m("aura,push,damage");
    m("aura,freeze,damage");
    m("aura,hostile,fire");
    m("aura,wounded,heal");
    m("aura,marked,damage");
    m("aura,origin_aim,damage");
    m("wave,shape_ring,damage");
    m("wave,shape_dome,damage");
    m("ray,damage,vow_blood");
    m("ray,damage,vow_stillness");
    m("ray,damage,vow_oath");
    COMBOS.add(new Combo("mobs", "ray,delay,damage", 110, 0));
    mz("aura,zone@seconds=2,poison");
    mz("aura,zone@seconds=2,push");
    mz("aura,zone@seconds=2&interval=0.5,damage@amount=1");
    mz("aura,shape_dome,zone@seconds=2,damage@amount=1");
    b("sky", "barrier,damage");
    b("sky", "barrier,shape_dome,heal");
    b("sky", "barrier,shape_ring,damage");
    b("sky", "barrier,shape_column,origin_self,damage");
    b("sky", "barrier,shape_sphere,origin_self,heal");
    b("sky", "barrier,shape_cone,damage");
    b("sky", "barrier,zone@seconds=2,damage");
  }

  private static GlyphInstance glyph(String token) {
    String[] parts = token.split("@", 2);
    Map<String, Double> params = new HashMap<>();
    if (parts.length == 2) for (String kv : parts[1].split("&")) {
      String[] p = kv.split("=");
      params.put(p[0], Double.parseDouble(p[1]));
    }
    return new GlyphInstance(new ResourceLocation("arcane", parts[0]), params);
  }

  private static String blockName(BlockState s) {
    return s.isAir() ? "ar" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
  }

  /** Cena: limpa a arena, monta o chao, os alvos e devolve o conjurador. */
  private static LivingEntity setup(GameTestHelper h, String scenario) {
    AABB box = new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(W, H, W))).inflate(14);
    for (Entity e : h.getLevel().getEntities((Entity) null, box, x -> true)) e.discard();
    for (int x = 0; x < W; x++) for (int z = 0; z < W; z++) {
      h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
      for (int y = 1; y < H; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
    }
    var caster = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(7, 1, 5));
    caster.setYRot(0);
    switch (scenario) {
      case "mobs" -> {
        mob(h, EntityType.COW, 7, 1, 9);
        mob(h, EntityType.SHEEP, 3, 1, 5);
        mob(h, EntityType.HUSK, 7, 1, 2);
      }
      case "glass" -> h.setBlock(new BlockPos(7, 1, 9), Blocks.GLASS);
      case "ice" -> h.setBlock(new BlockPos(7, 1, 9), Blocks.ICE);
      case "tnt" -> h.setBlock(new BlockPos(7, 1, 9), Blocks.TNT);
      case "grass" -> h.setBlock(new BlockPos(7, 1, 9), Blocks.GRASS_BLOCK);
      case "stone" -> h.setBlock(new BlockPos(7, 1, 9), Blocks.STONE);
      case "sand" -> {
        h.setBlock(new BlockPos(7, 0, 9), Blocks.AIR);
        h.setBlock(new BlockPos(7, 1, 9), Blocks.SAND);
      }
      case "water" -> {
        h.setBlock(new BlockPos(7, 1, 9), Blocks.STONE);
        h.setBlock(new BlockPos(7, 2, 9), Blocks.WATER);
        h.setBlock(new BlockPos(6, 2, 9), Blocks.STONE);
        h.setBlock(new BlockPos(8, 2, 9), Blocks.STONE);
      }
      case "sky" -> mob(h, EntityType.COW, 7, 1, 8);
      case "openmob" -> mob(h, EntityType.COW, 7, 1, 13);
      default -> { }
    }
    return caster;
  }

  private static void mob(GameTestHelper h, EntityType<? extends Mob> type, int x, int y, int z) {
    Mob mob = h.spawnWithNoFreeWill(type, new BlockPos(x, y, z));
    mob.setHealth(mob.getMaxHealth() * 0.6f);
  }

  private static String entityKey(Entity e, int index) {
    return e.getType().getDescription().getString().toLowerCase(Locale.ROOT) + "#" + index;
  }

  private record Snap(Map<String, BlockState> blocks, List<Entity> entities, Map<Entity, double[]> pos, Map<Entity, Float> health) {}

  private static Snap snapshot(GameTestHelper h) {
    Map<String, BlockState> blocks = new HashMap<>();
    for (int x = 0; x < W; x++) for (int y = 0; y < H; y++) for (int z = 0; z < W; z++) {
      blocks.put(x + "," + y + "," + z, h.getBlockState(new BlockPos(x, y, z)));
    }
    AABB box = new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(W, H, W))).inflate(6);
    List<Entity> entities = h.getLevel().getEntities((Entity) null, box, x -> true);
    Map<Entity, double[]> pos = new HashMap<>();
    Map<Entity, Float> health = new HashMap<>();
    for (Entity e : entities) {
      pos.put(e, new double[]{e.getX(), e.getY(), e.getZ()});
      if (e instanceof LivingEntity l) health.put(e, l.getHealth());
    }
    return new Snap(blocks, entities, pos, health);
  }

  private static String observe(GameTestHelper h, Snap before, LivingEntity caster) {
    List<String> notes = new ArrayList<>();
    Map<String, Integer> kinds = new HashMap<>();
    for (Entity e : before.entities()) {
      int idx = kinds.merge(e.getType().toString(), 1, Integer::sum);
      String label = e == caster ? "conjurador" : entityKey(e, idx);
      List<String> ch = new ArrayList<>();
      if (e.isRemoved() || !e.isAlive()) {
        ch.add(e instanceof LivingEntity l && l.isDeadOrDying() ? "morreu" : "sumiu");
      } else {
        double[] p = before.pos().get(e);
        double d = Math.sqrt(Math.pow(e.getX() - p[0], 2) + Math.pow(e.getY() - p[1], 2) + Math.pow(e.getZ() - p[2], 2));
        if (d > 0.6) ch.add(String.format(Locale.ROOT, "moveu %.1f bl", d));
        if (e instanceof LivingEntity l) {
          float hb = before.health().get(e);
          if (Math.abs(l.getHealth() - hb) > 0.01) ch.add(String.format(Locale.ROOT, "vida %.0f->%.0f", hb, l.getHealth()));
          if (l.isOnFire()) ch.add("pegando fogo");
          if (l.getTicksFrozen() > 0) ch.add("congelando");
          for (MobEffectInstance fx : l.getActiveEffects()) {
            ch.add(fx.getEffect().getDescriptionId().replace("effect.minecraft.", "efeito:") + (fx.getAmplifier() > 0 ? " " + (fx.getAmplifier() + 1) : ""));
          }
        }
      }
      if (!ch.isEmpty()) notes.add(label + " " + String.join(", ", ch));
    }
    // blocos
    Map<String, Integer> diff = new TreeMap<>();
    for (int x = 0; x < W; x++) for (int y = 0; y < H; y++) for (int z = 0; z < W; z++) {
      BlockState was = before.blocks().get(x + "," + y + "," + z);
      BlockState now = h.getBlockState(new BlockPos(x, y, z));
      // lava que escorre de estruturas de outros testes vizinhos nao e efeito da magia
      if (was.getBlock() != now.getBlock() && now.getBlock() != Blocks.LAVA) diff.merge(blockName(was) + "->" + blockName(now), 1, Integer::sum);
    }
    diff.forEach((k, v) -> notes.add("bloco " + k + (v > 1 ? " x" + v : "")));
    // entidades novas
    AABB box = new AABB(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(W, H, W))).inflate(6);
    Map<String, Integer> fresh = new TreeMap<>();
    for (Entity e : h.getLevel().getEntities((Entity) null, box, x -> true)) {
      if (!before.entities().contains(e)) fresh.merge(e.getType().toString().replace("entity.minecraft.", "").replace("entity.arcane.", ""), 1, Integer::sum);
    }
    fresh.forEach((k, v) -> notes.add("nova entidade " + k + (v > 1 ? " x" + v : "")));
    return notes.isEmpty() ? "nada observado" : String.join("; ", notes);
  }

  private static String warnings(Inscription inscription) {
    long n = SpellPreview.describe(inscription).stream()
        .filter(c -> c.getContents() instanceof TranslatableContents t && t.getKey().startsWith("gui.arcane.preview.warn.")).count();
    return n == 0 ? "" : " ⚠" + n + " aviso(s) da previa";
  }

  @GameTest(template = "empty", timeoutTicks = 6000, batch = "isolated_observe")
  public static void observeManySpells(GameTestHelper h) {
    if (System.getProperty("arcane.observe") == null) {
      h.succeed();
      return;
    }
    List<String> rows = new ArrayList<>();
    step(h, 0, rows);
  }

  private static void step(GameTestHelper h, int i, List<String> rows) {
    if (i >= COMBOS.size()) {
      try {
        Path out = Path.of("run", "observacoes.md");
        Files.createDirectories(out.getParent());
        Files.write(out, ("| # | Cena | Magia | Resultado observado |\n|---|---|---|---|\n" + String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      h.succeed();
      return;
    }
    Combo c = COMBOS.get(i);
    LivingEntity caster = setup(h, c.scenario());
    caster.setXRot(c.pitch());
    List<GlyphInstance> glyphs = new ArrayList<>();
    for (String t : c.spell().split(",")) glyphs.add(glyph(t));
    Inscription inscription = new Inscription(glyphs);
    // deixa a cena assentar 2 ticks (a areia cai, os mobs pousam) antes de conjurar
    h.runAfterDelay(2, () -> {
      Snap before = snapshot(h);
      String cast;
      try {
        var result = CastingService.tryCast(caster, inscription, h.getLevel());
        cast = result.success() ? "" : "FALHOU: " + result.message() + ". ";
      } catch (RuntimeException e) {
        cast = "EXCECAO " + e + ". ";
      }
      final String castNote = cast;
      h.runAfterDelay(c.ticks(), () -> {
        String result = observe(h, before, caster);
        Zones.clearFor(caster.getUUID());
        rows.add("| " + (i + 1) + " | " + c.scenario() + " | `" + c.spell() + "` | " + castNote + result + warnings(inscription) + " |");
        step(h, i + 1, rows);
      });
    });
  }
}
