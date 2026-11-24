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

  // ------------------------------------------------------------------ vitrine: gestos ociosos, andar no terreno, masmorra do covil
  private static java.util.List<BlockPos> rooms = new java.util.ArrayList<>();
  private static Vec3 walkFrom;

  private static void step(Minecraft mc) {
    var p = mc.player;
    if (t == 40) {
      cmd("gamemode creative");
      cmd("time set 6000");
      cmd("gamerule doDaylightCycle false");
      cmd("weather clear 100000");
      cmd("gamerule doMobSpawning false");
      cmd("gamerule mobGriefing false");
      cmd("kill @e[type=!player]");
      mc.options.hideGui = true;
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      base = p.position();
      server(sp -> com.arcane.grimoire.GrimoireLifecycleHandler.toggleFloating(sp));
      write("base " + base);
      return;
    }
    // 1) gestos ociosos: estagio 6 acordada, sem alvo (jogador em criativo)
    if (t == 60) server(sp -> {
      var l = sp.serverLevel();
      HydraEntity h = ModEntities.HYDRA.get().create(l);
      h.setStage(5);
      h.setVariant(-1);
      var hm = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
      int hy = l.getHeight(hm, (int) base.x, (int) base.z + 30);
      h.moveTo(base.x, hy, base.z + 30, 90f, 0); // de lado para a camera: o comprimento aparece
      l.addFreshEntity(h);
      h.setSealMask(0);
      h.setNoAi(true);
      h.setYBodyRot(90f);
      h.setYHeadRot(90f);
      h.yBodyRotO = 90f;
      int cy = Math.max(hy, l.getHeight(hm, (int) base.x, (int) base.z - 16));
      sp.getAbilities().flying = true;
      sp.onUpdateAbilities();
      sp.connection.teleport(base.x, cy + 16, base.z - 30, 0f, 4f);
    });
    if (t == 240) server(sp -> { sp.getAbilities().flying = true; sp.onUpdateAbilities(); });
    if (t == 330) server(sp -> sp.serverLevel().getEntitiesOfClass(HydraEntity.class, sp.getBoundingBox().inflate(150)).forEach(h ->
        sp.connection.teleport(h.getX() + 4, h.getY() + 5, h.getZ() - 30, 0f, 4f))); // perto, de lado: patas
    if (t == 242) server(sp -> sp.serverLevel().getEntitiesOfClass(HydraEntity.class, sp.getBoundingBox().inflate(120)).forEach(h ->
        sp.connection.teleport(h.getX() - 52, h.getY() + 30, h.getZ() - 46, -48f, 16f))); // tres quartos, de frente
    if (t > 60 && t < 420 && t % 10 == 0) server(sp -> sp.serverLevel().getEntitiesOfClass(HydraEntity.class, sp.getBoundingBox().inflate(80)).forEach(h -> { h.setYBodyRot(90f); h.setYRot(90f); }));
    if (t > 60 && t < 420) {
      if (t % 5 == 0) shot(String.format("idle_%03d", t - 60));
      if (t % 20 == 0) server(sp -> sp.serverLevel().getEntitiesOfClass(HydraEntity.class, sp.getBoundingBox().inflate(80))
          .forEach(h -> write("  t" + t + " gesto=" + h.idleAction() + " poder=" + h.power())));
    }
    // 2) andar no terreno natural: estagio 9 caminhando por morros longe da area aplainada
    if (t == 425) { write("fim do roteiro"); mc.options.hideGui = false; mc.options.pauseOnLostFocus = oldPause; armed = false; return; }
    if (t == 430) {
      cmd("kill @e[type=arcane:hydra]");
      server(sp -> {
        var l = sp.serverLevel();
        int x = (int) base.x, z = (int) base.z - 120;
        int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        walkFrom = new Vec3(x + 0.5, y, z + 0.5);
        HydraEntity h = ModEntities.HYDRA.get().create(l);
        h.setStage(9);
        h.setVariant(0);
        h.moveTo(walkFrom.x, walkFrom.y, walkFrom.z, 0f, 0);
        l.addFreshEntity(h);
        h.setSealMask(0);
        sp.connection.teleport(walkFrom.x + 28, walkFrom.y + 10, walkFrom.z + 10, 110f, 18f);
        write("andar: comeca em " + walkFrom);
      });
    }
    if (t > 520 && t < 760) {
      server(sp -> sp.serverLevel().getEntitiesOfClass(HydraEntity.class, sp.getBoundingBox().inflate(120)).forEach(h -> {
        if (t % 40 == 0) {
          h.getNavigation().moveTo(walkFrom.x + (t % 80 == 0 ? 0 : -6), walkFrom.y, walkFrom.z + 36, 1.0);
          write(String.format("  t%d hidra em (%.1f, %.1f, %.1f)", t, h.getX(), h.getY(), h.getZ()));
        }
        // a camera acompanha de lado
        if (t % 10 == 0) sp.connection.teleport(h.getX() + 30, h.getY() + 12, h.getZ() - 4, 100f, 18f);
      }));
      if (t % 5 == 0) shot(String.format("walk_%03d", t - 520));
    }
    // 3) masmorra do covil: constroi um covil num lugar novo e entra nas salas
    if (t == 770) {
      cmd("kill @e[type=arcane:hydra]");
      server(sp -> {
        var l = sp.serverLevel();
        BlockPos c = BlockPos.containing(base).offset(0, -30, 260);
        int surface = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, c.getX(), c.getZ());
        com.arcane.world.HydraLairs.buildAt(l, c, surface);
        var map = com.arcane.world.LairDungeon.build(l, c, c.offset(38, 0, 0));
        rooms = new java.util.ArrayList<>(map.keySet());
        write("masmorra: " + map);
      });
    }
    if (t >= 790 && t < 1030) {
      int k = (t - 790) / 40;
      if ((t - 790) % 40 == 0 && k < rooms.size()) {
        BlockPos r = rooms.get(k);
        tp(r.getX() + 0.5 - 3.5, r.getY() + 1, r.getZ() + 0.5 - 3.5, -45f, 20f);
      }
      if ((t - 790) % 40 == 20 && k < rooms.size()) shot("room_" + k);
    }
    if (t == 1040) {
      cmd("gamemode creative");
      mc.options.hideGui = false;
      mc.options.pauseOnLostFocus = oldPause;
      write("fim do roteiro");
      armed = false;
    }
  }
}
