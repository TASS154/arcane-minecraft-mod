package com.arcane.client;

import com.arcane.execution.SpellElement;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Folha de contato elemento x forma (dev). Roda quando {@code run/arcane_autotest.flag} contem a palavra "elementos".
 * Um suporte de armadura invisivel no palco conjura cada elemento em cada forma contra uma parede; antes de cada
 * conjuracao o cliente limpa as particulas, e a foto sai alguns ticks depois em
 * {@code run/screenshots/autotest_sheet_<elemento>_<forma>.png}. {@code tools/dev/contact_sheet.py} monta a folha.
 */
final class DevContactSheet {
  private DevContactSheet() {}

  /** Forma e o tick da foto depois da conjuracao. */
  private record Form(String id, int shotAt) {}

  private static final List<Form> FORMS = List.of(new Form("projectile", 7), new Form("ray", 4), new Form("aura", 7),
      new Form("wave", 7), new Form("pillar", 8));

  /** Um efeito que representa cada elemento e nao estraga o palco (sem cavar nem incendiar). */
  private static String effectOf(SpellElement e) {
    return switch (e) {
      case ARCANE -> "mark";
      case FIRE -> "fire";
      case FROST -> "freeze";
      case STORM -> "electrify";
      case VENOM -> "poison";
      case FORCE -> "levitate";
      case VOID -> "blind";
      case LIFE -> "heal";
      case EARTH -> "rumble";
      case WATER -> "wet";
      case LIGHT -> "reveal";
      case SHADOW -> "darkness";
      case BLOOD -> "cut";
      case TIME -> "remember";
      case NATURE -> "grow";
      case SOUND -> "melody";
    };
  }

  /** Pares de elementos (corpo, rastro) para ver a composicao no projetil. */
  private static final SpellElement[][] PAIRS = {
      {SpellElement.FIRE, SpellElement.FROST}, {SpellElement.STORM, SpellElement.WATER},
      {SpellElement.VOID, SpellElement.LIFE}, {SpellElement.EARTH, SpellElement.NATURE}};

  private record Shot(String name, List<GlyphInstance> glyphs, int shotAt) {}

  private static final int SLOT = 34; // ticks por foto
  private static List<Shot> shots;
  private static Vec3 base;
  private static ArmorStand stand;

  private static GlyphInstance g(String id) { return new GlyphInstance(new ResourceLocation("arcane", id)); }

  private static List<Shot> plan() {
    List<Shot> list = new ArrayList<>();
    for (SpellElement e : SpellElement.values()) {
      for (Form f : FORMS) {
        list.add(new Shot(e.name().toLowerCase(Locale.ROOT) + "_" + f.id(), List.of(g(f.id()), g(effectOf(e))), f.shotAt()));
      }
    }
    for (SpellElement[] p : PAIRS) {
      list.add(new Shot("pair_" + p[0].name().toLowerCase(Locale.ROOT) + "_" + p[1].name().toLowerCase(Locale.ROOT),
          List.of(g("projectile"), g(effectOf(p[0])), g(effectOf(p[1]))), 7));
    }
    return list;
  }

  /** Um passo do roteiro; devolve false quando acabou. */
  static boolean step(Minecraft mc, int t, Consumer<String> log, Consumer<Consumer<ServerPlayer>> server, Consumer<String> cmd) {
    var p = mc.player;
    if (t == 40) {
      shots = plan();
      cmd.accept("gamemode creative");
      cmd.accept("time set 6000");
      cmd.accept("gamerule doDaylightCycle false");
      cmd.accept("weather clear 100000");
      cmd.accept("gamerule doMobSpawning false");
      cmd.accept("kill @e[type=!player]");
      mc.options.hideGui = true;
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      base = new Vec3(Math.floor(p.getX()), Math.floor(p.getY()) + 30, Math.floor(p.getZ()));
      int bx = (int) base.x, by = (int) base.y, bz = (int) base.z;
      // palco no ar (longe do terreno): chao, parede a direita (leste) e um fundo escuro atras
      cmd.accept(String.format("fill %d %d %d %d %d %d smooth_stone", bx - 8, by - 1, bz - 3, bx + 10, by - 1, bz + 16));
      cmd.accept(String.format("fill %d %d %d %d %d %d polished_andesite", bx + 4, by, bz + 2, bx + 4, by + 6, bz + 12));
      cmd.accept(String.format("fill %d %d %d %d %d %d gray_concrete", bx - 8, by, bz + 16, bx + 10, by + 8, bz + 16));
      server.accept(sp -> {
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        sp.connection.teleport(base.x + 0.5, base.y + 0.6, base.z + 2.5, -12f, 14f); // perto: as particulas sao pequenas
        var l = sp.serverLevel();
        stand = EntityType.ARMOR_STAND.create(l);
        stand.moveTo(base.x - 1.5, base.y, base.z + 7.5, -90f, 0f); // olhando para o leste (a parede)
        stand.setYHeadRot(-90f);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        l.addFreshEntity(stand);
      });
      log.accept("folha de contato: " + plan().size() + " fotos, palco em " + base);
      return true;
    }
    if (t < 60 || shots == null) return true;
    int i = (t - 60) / SLOT, k = (t - 60) % SLOT;
    if (i >= shots.size()) {
      log.accept("fim do roteiro");
      return false;
    }
    Shot s = shots.get(i);
    if (k == 0) {
      mc.particleEngine.setLevel(mc.level); // limpa as particulas da foto anterior (clearParticles e privado)
      server.accept(sp -> {
        if (stand == null) return;
        com.arcane.execution.WorldScars.clearAll(); // residuos das conjuracoes anteriores sujavam as fotos seguintes
        // Estrondo e afins empurram quem esta perto: camera e suporte voltam ao lugar antes de cada foto
        sp.setDeltaMovement(Vec3.ZERO);
        sp.connection.teleport(base.x + 0.5, base.y + 0.6, base.z + 2.5, -12f, 14f);
        stand.removeAllEffects(); // Levitacao, Cegar, Veneno no suporte soltavam particulas nas fotos seguintes
        stand.clearFire();
        stand.setDeltaMovement(Vec3.ZERO);
        stand.moveTo(base.x - 1.5, base.y, base.z + 7.5, -90f, 0f);
        stand.setYRot(-90f);
        stand.setXRot(0f);
        var r = com.arcane.execution.CastingService.tryCast(stand, new Inscription(s.glyphs()), sp.serverLevel());
        if (!r.success()) log.accept("  " + s.name() + ": falhou " + r.message());
      });
    }
    if (k == s.shotAt()) {
      Screenshot.grab(mc.gameDirectory, "autotest_sheet_" + s.name() + ".png", mc.getMainRenderTarget(), msg -> { });
    }
    return true;
  }
}
