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

  private static void step(Minecraft mc) {
    var p = mc.player;
    switch (t) {
      case 40 -> {
        cmd("gamemode creative");
        cmd("time set 6000");
        cmd("gamerule doDaylightCycle false");
        cmd("weather clear 100000");
        cmd("gamerule doMobSpawning false");
        cmd("gamerule mobGriefing true");
        cmd("kill @e[type=!player]");
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        base = p.position();
        write("base " + base);
      }
      case 60 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = sp.blockPosition();
        for (int x = -30; x <= 30; x++) for (int z = -4; z <= 60; z++) {
          l.setBlock(o.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
          for (int y = 0; y < 22; y++) l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        sp.getInventory().setItem(sp.getInventory().selected, new ItemStack(com.arcane.registry.ModItems.SCEPTER.get()));
        write("plataforma pronta, cetro na mao");
      });
      // ---- cetros: 10 visuais em terceira pessoa de frente
      case 70 -> { tp(base.x, base.y, base.z, 180f, 0f); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
      case 80, 92, 104, 116, 128, 140, 152, 164, 176, 188 -> styleHeld(STYLES[(t - 80) / 12]);
      case 88, 100, 112, 124, 136, 148, 160, 172, 184, 196 -> shot("scepter_style_" + ((t - 88) / 12));
      case 200 -> { styleHeld(STYLES[0]); mc.options.setCameraType(CameraType.FIRST_PERSON); }
      case 212 -> shot("scepter_first_person");
      case 214 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      case 226 -> shot("scepter_third_back");
      case 228 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.hideGui = false; com.arcane.client.ClientHooks.openScepter(); }
      case 245 -> shot("scepter_screen");
      case 247 -> { mc.setScreen(null); mc.options.hideGui = false; }
      case 250 -> shot("scepter_hotbar");
      case 252 -> mc.options.hideGui = true;
      // ---- telecinese: pega um bloco e gira a camera; mede no CLIENTE a distancia do bloco ate a mira
      case 260 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = sp.blockPosition();
        sp.getInventory().setItem(sp.getInventory().selected, ItemStack.EMPTY);
        l.setBlock(o.offset(0, 1, 4), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        sp.connection.teleport(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, 0f, 3f);
      });
      case 270 -> server(sp -> {
        var r = com.arcane.execution.CastingService.tryCast(sp, new Inscription(List.of(g("ray"), g("telekinesis", "distance", 4.0, "duration", 400.0))),
            com.arcane.execution.PrecisionSnapshot.FIELD);
        write("telecinese: conjurou=" + r.success() + " " + r.message() + " segurando=" + com.arcane.execution.Telekinesis.heldCount(sp.getUUID()));
      });
      case 300 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
      case 360 -> server(sp -> com.arcane.execution.Telekinesis.release(sp.getUUID()));
      // ---- explosao destrutiva: onda de choque; uma barreira protege um pilar
      case 380 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = BlockPos.containing(base);
        // pilar de pedra protegido por uma parede de barreira entre ele e a explosao, e outro pilar sem protecao
        for (int y = 0; y < 6; y++) {
          l.setBlock(o.offset(6, y, 30), Blocks.STONE_BRICKS.defaultBlockState(), 2);
          l.setBlock(o.offset(-6, y, 30), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        }
        for (int x = 4; x <= 8; x++) for (int y = 0; y < 6; y++) l.setBlock(o.offset(x, y, 28), com.arcane.registry.ModBlocks.ARCANE_BARRIER.get().defaultBlockState(), 2);
        sp.connection.teleport(o.getX() + 0.5, o.getY() + 12, o.getZ() + 8.5, 0f, 30f);
      });
      case 400 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        var at = base.add(0, 0, 26);
        com.arcane.execution.SpellImmunity.explodeAs(sp, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK, 11f,
            () -> l.explode(sp, at.x, at.y, at.z, 11f, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK));
        write("explosao: blocos na fila logo depois = " + com.arcane.execution.CraterQueue.pending(l));
      });
      case 403 -> shot("blast_wave_1");
      case 407 -> { shot("blast_wave_2"); server(sp -> write("explosao: na fila depois de 7 ticks = " + com.arcane.execution.CraterQueue.pending(sp.serverLevel()))); }
      case 412 -> shot("blast_wave_3");
      case 440 -> server(sp -> {
        BlockPos o = BlockPos.containing(base);
        var l = sp.serverLevel();
        int kept = 0, lost = 0;
        for (int y = 0; y < 6; y++) {
          if (!l.getBlockState(o.offset(6, y, 30)).isAir()) kept++;
          if (!l.getBlockState(o.offset(-6, y, 30)).isAir()) lost++;
        }
        write("depois da explosao: pilar atras da barreira " + kept + "/6 blocos, pilar sem barreira " + lost + "/6, fila " + com.arcane.execution.CraterQueue.pending(l));
      });
      case 442 -> shot("blast_after");
      case 460 -> {
        cmd("gamemode creative");
        mc.options.hideGui = false;
        mc.options.pauseOnLostFocus = oldPause;
        write("fim do roteiro");
        armed = false;
      }
      default -> {
      }
    }
    if (t >= 272 && t < 340 && t % 4 == 0) {
      int now = t;
      server(sp -> {
        var held = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, sp.getBoundingBox().inflate(12));
        write("servidor t" + now + ": segurando=" + com.arcane.execution.Telekinesis.heldCount(sp.getUUID()) + " blocos-entidade=" + held.size()
            + (held.isEmpty() ? "" : String.format(" pos=(%.1f,%.1f,%.1f) chao=%s", held.get(0).getX(), held.get(0).getY(), held.get(0).getZ(), held.get(0).onGround())));
      });
      write("cliente t" + now + ": blocos-entidade=" + mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, p.getBoundingBox().inflate(12)).size());
    }
    // telecinese: gira a camera 9 graus por tick por 2 s e mede o bloco no cliente
    if (t >= 280 && t < 340) {
      look(p.getYRot() + (t < 310 ? 9f : -9f), 25f);
      var fb = mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class, p.getBoundingBox().inflate(10));
      if (!fb.isEmpty()) {
        Vec3 anchor = p.getEyePosition().add(p.getLookAngle().scale(4.0));
        Vec3 at = fb.get(0).position().add(0, 0.49, 0);
        write(String.format("telecinese t%d: bloco a %.2f da mira (cliente)", t, at.distanceTo(anchor)));
      }
      if (t == 295) shot("telekinesis_turning");
    }
  }
}
