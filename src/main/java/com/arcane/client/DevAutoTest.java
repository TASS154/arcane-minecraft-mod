package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.entity.HydraEntity;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.registry.ModEntities;
import com.arcane.world.HydraLairs;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Roteiro de teste visual para desenvolvimento: so roda se existir o arquivo {@code arcane_autotest.flag} na pasta do
 * jogo (run/). Ao entrar num mundo, fotografa (run/screenshots/autotest_*.png) o visual de cada glifo original e uma
 * Hidra selada, a quebra dos selos, o despertar e a devastacao, e grava o que aconteceu em {@code arcane_autotest.log}.
 * Apaga o arquivo-gatilho ao comecar, entao roda uma vez so. Nao existe no jogo normal.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class DevAutoTest {
  private static int t = -1;
  private static boolean armed;
  private static boolean checked;
  private static Path log;
  private static final List<Runnable> PENDING = new ArrayList<>();
  private static Vec3 base;
  private static BlockPos lairCentre;
  private static int lairWaits;
  private static boolean oldPause;
  /** Entrada falsa do jogador: o roteiro "aperta" frente/lado/subir sem depender do teclado (a janela pode estar sem foco). */
  private static float fakeFwd, fakeSide;
  private static boolean fakeSprint;
  private static net.minecraft.client.player.Input realInput;

  private static void drive(float fwd, float side, boolean sprint) {
    var p = Minecraft.getInstance().player;
    if (realInput == null) realInput = p.input;
    fakeFwd = fwd; fakeSide = side; fakeSprint = sprint;
    p.input = new net.minecraft.client.player.Input() {
      @Override
      public void tick(boolean slow, float factor) {
        forwardImpulse = fakeFwd; leftImpulse = -fakeSide; up = fakeFwd > 0; down = false; left = fakeSide < 0; right = fakeSide > 0;
        jumping = false; shiftKeyDown = false;
      }
    };
    p.setSprinting(sprint);
  }

  private static void release() {
    var p = Minecraft.getInstance().player;
    if (realInput != null) p.input = realInput;
    realInput = null;
    p.setSprinting(false);
  }

  private DevAutoTest() {}

  private static void write(String line) {
    try {
      Files.writeString(log, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ignored) {
    }
  }

  private static void server(java.util.function.Consumer<ServerPlayer> action) {
    var mc = Minecraft.getInstance();
    var srv = mc.getSingleplayerServer();
    if (srv == null || mc.player == null) return;
    var id = mc.player.getUUID();
    srv.execute(() -> {
      ServerPlayer sp = srv.getPlayerList().getPlayer(id);
      if (sp == null) return;
      try {
        action.accept(sp);
      } catch (RuntimeException e) {
        write("ERRO no servidor: " + e);
      }
    });
  }

  private static void cmd(String c) {
    server(sp -> sp.server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c));
  }

  private static void shot(String name) {
    var mc = Minecraft.getInstance();
    Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.getMainRenderTarget(), msg -> write("foto " + name + ": " + msg.getString()));
  }

  private static void look(float yaw, float pitch) {
    var p = Minecraft.getInstance().player;
    p.setYRot(yaw);
    p.setXRot(pitch);
    p.yRotO = yaw;
    p.xRotO = pitch;
    server(sp -> sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), yaw, pitch));
  }

  private static void tp(double x, double y, double z, float yaw, float pitch) {
    server(sp -> sp.connection.teleport(x, y, z, yaw, pitch));
  }

  private static GlyphInstance g(String id, Object... kv) {
    if (kv.length == 0) return new GlyphInstance(new ResourceLocation("arcane", id));
    java.util.Map<String, Double> m = new java.util.HashMap<>();
    for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
    return new GlyphInstance(new ResourceLocation("arcane", id), m);
  }

  @SubscribeEvent
  public static void onTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    var mc = Minecraft.getInstance();
    if (!checked && mc.level != null && mc.player != null) {
      checked = true;
      Path flag = mc.gameDirectory.toPath().resolve("arcane_autotest.flag");
      if (Files.exists(flag)) {
        armed = true;
        t = 0;
        log = mc.gameDirectory.toPath().resolve("arcane_autotest.log");
        try {
          Files.deleteIfExists(flag);
          Files.deleteIfExists(log);
        } catch (IOException ignored) {
        }
        oldPause = mc.options.pauseOnLostFocus;
        mc.options.pauseOnLostFocus = false;
        write("inicio do roteiro");
      }
    }
    if (!armed || mc.player == null) return;
    if (mc.screen != null && !(mc.screen instanceof com.arcane.client.screen.RadialScreen) && !(mc.screen instanceof com.arcane.client.screen.ScepterScreen)) mc.setScreen(null);
    t++;
    step(mc);
  }

  /** Manda direto o visual de um glifo original (o efeito de jogo e coberto pelos GameTests). */
  private static void fx(int kind, Vec3 at, Vec3 dir, float radius, int ticks) {
    server(sp -> com.arcane.network.ModNetwork.sendShape(sp.serverLevel(), com.arcane.network.packet.S2CShapeFxPacket.ORIGINAL, at, dir, radius, 0xFFFFFF, kind, ticks));
  }

  private static int[][] STYLES = {
      {0, 0, 0, 0},   // lua crescente, haste branca, gema vermelha, ouro
      {1, 4, 8, 4},   // gaiola, obsidiana, roxa, ferro negro
      {2, 1, 6, 1},   // aro de monge, madeira, azul-clara, prata
      {3, 6, 17, 6},  // cristais, haste de cristal, diamante, celeste
      {4, 7, 4, 3},   // espiral do druida, galho, verde, cobre
      {5, 5, 19, 0},  // asas, toda de metal, solar, ouro
      {6, 2, 9, 2},   // estrela, retorcida, magenta, rose
      {7, 3, 14, 7},  // tridente, osso, negra, carmesim
      {8, 0, 5, 5},   // aureola, branca, ciano, marfim
      {9, 1, 16, 0},  // orbe, madeira, ametista, ouro
  };

  private static void styleHeld(int[] st) {
    server(sp -> {
      var stack = sp.getMainHandItem();
      for (int part = 0; part < 4; part++) com.arcane.item.ScepterItem.setStyle(stack, part, st[part]);
    });
  }

  // ------------------------------------------------------------------ luta contra Hidras de varios estagios
  private static final int[] STAGES = {1, 4, 7, 10, 14};
  private static final int ROUND = 160;   // ticks por estagio
  private static final int START = 80;
  private static int lastPower = -1;
  private static float hpBefore;

  private static double distFor(int stage) { return 10 + HydraEntity.scaleFor(stage) * 4.0; }

  private static void step(Minecraft mc) {
    var p = mc.player;
    if (t == 40) {
      cmd("gamemode creative");
      cmd("time set 6000");
      cmd("gamerule doDaylightCycle false");
      cmd("weather clear 100000");
      cmd("gamerule doMobSpawning false");
      cmd("gamerule mobGriefing true");
      cmd("kill @e[type=!player]");
      mc.options.hideGui = false;
      mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      base = p.position();
      write("base " + base);
      server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = BlockPos.containing(base);
        for (int x = -40; x <= 40; x++) for (int z = -20; z <= 70; z++) {
          l.setBlock(o.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
          for (int y = 0; y < 40; y++) l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
      });
      return;
    }
    if (t < START) return;
    int round = (t - START) / ROUND, at = (t - START) % ROUND;
    if (round >= STAGES.length) {
      if (at == 0) {
        cmd("kill @e[type=arcane:hydra]");
        cmd("gamemode creative");
        mc.options.pauseOnLostFocus = oldPause;
        write("fim do roteiro");
        armed = false;
      }
      return;
    }
    int stage = STAGES[round];
    double dist = distFor(stage);
    if (at == 0) {
      cmd("kill @e[type=arcane:hydra]");
      cmd("gamemode survival");
      server(sp -> {
        var l = sp.serverLevel();
        sp.setHealth(sp.getMaxHealth());
        sp.removeAllEffects();
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20 * 60, 3, false, false));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 20 * 60, 4, false, false));
        var mana = com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(sp);
        mana.setBaseMax(2000);
        mana.set(2000);
        HydraEntity h = ModEntities.HYDRA.get().create(l);
        h.setStage(stage);
        h.moveTo(base.x, base.y, base.z + dist, 180f, 0);
        l.addFreshEntity(h);
        h.setSealMask(0);
        h.setTarget(sp);
        sp.connection.teleport(base.x, base.y, base.z, 0f, -5f);
        hpBefore = sp.getHealth();
        lastPower = -1;
        write(String.format("== estagio %d: vida %.0f (efetiva %.0f), dano %.1f, cabecas %d, largura %.1f, devasta=%s, selada=%s",
            stage, h.getHealth(), h.effectiveMaxHealth(), h.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
            h.heads(), h.getBbWidth(), HydraEntity.devastates(stage), h.sealed()));
      });
      return;
    }
    // o jogador encara a Hidra e lanca uma magia a cada 1,5 s
    server(sp -> {
      var hs = sp.serverLevel().getEntitiesOfClass(HydraEntity.class, sp.getBoundingBox().inflate(80));
      if (hs.isEmpty()) return;
      HydraEntity h = hs.get(0);
      var eye = sp.getEyePosition();
      var to = h.position().add(0, h.getBbHeight() * 0.5, 0).subtract(eye);
      float yaw = (float) (Math.toDegrees(Math.atan2(-to.x, to.z)));
      float pitch = (float) (-Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z))));
      if (at % 2 == 0) sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), yaw, pitch);
      if (at % 30 == 10) {
        var spell = new Inscription(List.of(g("projectile"), g("damage", "amount", 8.0), g("fire")));
        com.arcane.execution.CastingService.tryCast(sp, spell, com.arcane.execution.PrecisionSnapshot.WORKBENCH);
      }
      if (h.power() != lastPower) {
        if (h.power() != 0) write(String.format("  t%d estagio %d usou o poder %d", at, stage, h.power()));
        lastPower = h.power();
      }
      if (at == ROUND - 2) {
        write(String.format("  fim do estagio %d: vida da Hidra %.0f/%.0f, jogador levou %.1f (com Resistencia IV e Regeneracao V), viva=%s",
            stage, h.getHealth(), h.getMaxHealth(), Math.max(0, hpBefore - sp.getHealth()), h.isAlive()));
      }
    });
    if (at % 5 == 0) shot(String.format("hydra_s%02d_%03d", stage, at));
  }
}
