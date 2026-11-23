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
 * jogo (run/). Ao entrar num mundo, mostra e fotografa (run/screenshots/autotest_*.png) as Hidras, o Cetro, as poses de
 * voo, a telecinese e um covil, e grava em {@code arcane_autotest.log} a linha do tempo de uma magia com atrasos.
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
    if (mc.screen != null && !(mc.screen instanceof com.arcane.client.screen.RadialScreen)) mc.setScreen(null);
    t++;
    step(mc);
  }

  private static void step(Minecraft mc) {
    var p = mc.player;
    if (t > 490 && t <= 555 && t % 5 == 0) {
      int now = t - 490;
      server(sp -> write(String.format("magia longa t+%d: pos (%.1f, %.1f, %.1f)", now, sp.getX(), sp.getY(), sp.getZ())));
    }
    switch (t) {
      case 40 -> {
        cmd("gamemode creative");
        cmd("time set 6000");
        cmd("gamerule doDaylightCycle false");
        cmd("weather clear 100000");
        cmd("gamerule doMobSpawning false");
        cmd("kill @e[type=!player]");
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        base = p.position();
        write("base " + base);
      }
      case 60 -> server(sp -> {
        // plataforma de pedra limpa e tres Hidras paradas (estagios 1, 7 e 14), uma ao lado da outra
        ServerLevel l = sp.serverLevel();
        BlockPos o = sp.blockPosition();
        for (int x = -30; x <= 30; x++) for (int z = -4; z <= 60; z++) {
          l.setBlock(o.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
          for (int y = 0; y < 22; y++) l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        int[][] at = {{-8, 10, 1}, {0, 16, 7}, {14, 30, 14}};
        for (int[] a : at) {
          HydraEntity h = ModEntities.HYDRA.get().create(l);
          h.moveTo(o.getX() + a[0] + 0.5, o.getY(), o.getZ() + a[1] + 0.5, 180f, 0f);
          h.setStage(a[2]);
          h.setNoAi(true);
          h.yBodyRot = 180f;
          h.yHeadRot = 180f;
          h.addTag("arcane_autotest");
          l.addFreshEntity(h);
        }
        write("hidras criadas: estagios 1, 7, 14");
      });
      case 70 -> {
        tp(base.x, base.y + 6, base.z - 2, 0f, 12f);
      }
      case 110 -> shot("hydras_1_7_14");
      case 115 -> tp(base.x - 22, base.y + 8, base.z + 30, -90f, 8f);
      case 150 -> shot("hydra_elder_side");
      case 155 -> tp(base.x - 4, base.y + 2, base.z + 4, -30f, 5f);
      case 185 -> shot("hydra_young_close");
      case 190 -> {
        cmd("kill @e[tag=arcane_autotest]");
        tp(base.x, base.y, base.z, 0f, 0f);
      }
      case 200 -> server(sp -> {
        ItemStack sc = new ItemStack(com.arcane.registry.ModItems.SCEPTER.get());
        com.arcane.item.ScepterItem.restyle(sc, new ItemStack(net.minecraft.world.item.Items.EMERALD));
        com.arcane.item.ScepterItem.restyle(sc, new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT));
        com.arcane.item.ScepterItem.store(sc, new Inscription(List.of(g("self"), g("heal"))));
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sc);
        sp.getInventory().selected = 0;
        sp.getInventory().setItem(0, sc);
      });
      case 215 -> look(0f, 10f);
      case 230 -> shot("scepter_first_person");
      case 232 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      case 250 -> shot("scepter_third_person");
      case 252 -> server(sp -> {
        ItemStack sc = sp.getMainHandItem();
        com.arcane.item.ScepterItem.restyle(sc, new ItemStack(net.minecraft.world.item.Items.NETHER_STAR));
        com.arcane.item.ScepterItem.restyle(sc, new ItemStack(net.minecraft.world.item.Items.OBSIDIAN));
      });
      case 270 -> shot("scepter_star_obsidian");
      // ---- voo
      case 280 -> {
        server(sp -> {
          sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
          sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get(), 2400,
              com.arcane.execution.FlightService.encode(0.8, 0), false, false));
        });
        tp(base.x, base.y + 18, base.z, 0f, 35f);
      }
      case 290 -> drive(1f, 0f, false);
      case 320 -> shot("flight_dive_front");
      case 322 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      case 330 -> shot("flight_dive_back");
      case 332 -> { look(0f, -35f); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
      case 360 -> shot("flight_climb_front");
      case 362 -> { look(0f, 0f); }
      case 380 -> shot("flight_level_front");
      case 382 -> drive(0f, 1f, false);
      case 405 -> shot("flight_strafe_right_front");
      case 407 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      case 415 -> shot("flight_strafe_right_back");
      case 417 -> drive(1f, 0f, true);
      case 430 -> write("voo: velocidade com Ctrl (nivel 1) = " + String.format("%.3f", p.getDeltaMovement().length()));
      case 431 -> { p.setSprinting(true); net.minecraft.client.KeyMapping.click(mc.options.keySprint.getKey()); }
      case 450 -> write("voo: depois de 1 toque extra no Ctrl = " + String.format("%.3f", p.getDeltaMovement().length()));
      case 451 -> { p.setSprinting(true); net.minecraft.client.KeyMapping.click(mc.options.keySprint.getKey()); }
      case 470 -> { write("voo: depois de 2 toques extras = " + String.format("%.3f", p.getDeltaMovement().length())); shot("flight_boost3_back"); }
      case 472 -> {
        release();
        server(sp -> sp.removeEffect(com.arcane.registry.ModEffects.FLOAT_FLIGHT.get()));
        tp(base.x, base.y, base.z, 0f, 0f);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
      }
      // ---- a magia com atrasos (igual a mais recente do grimorio): posicao a cada 5 ticks
      case 490 -> server(sp -> {
        var spell = new Inscription(List.of(g("self"), g("recall"), g("after_seconds", "seconds", 1.0), g("push", "strength", 3.0),
            g("hook", "strength", 3.0), g("after_seconds", "seconds", 1.0), g("phase"), g("after_seconds", "seconds", 1.0), g("recall")));
        var plan = com.arcane.inscription.InscriptionCompiler.compile(spell);
        StringBuilder sb = new StringBuilder("magia longa: atrasos dos trechos (ticks) =");
        for (var seg : plan.segments()) sb.append(' ').append(seg.delayTicks());
        write(sb.toString());
        com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(sp).set(1000f);
        var r = com.arcane.execution.CastingService.tryCast(sp, spell, com.arcane.execution.PrecisionSnapshot.FIELD);
        write("magia longa: conjurou=" + r.success() + " " + r.message() + " em " + sp.position());
      });
      // ---- telecinese num bloco de pedras
      case 560 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = sp.blockPosition().offset(0, 0, 5);
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = 0; z <= 2; z++) l.setBlock(o.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 0f, 15f);
      });
      case 570 -> server(sp -> {
        var spell = new Inscription(List.of(g("touch", "range", 6.0), g("telekinesis", "sphere", 2.0)));
        com.arcane.grimoire.GrimoireAttachments.getManaOrCreate(sp).set(1000f);
        var r = com.arcane.execution.CastingService.tryCast(sp, spell, com.arcane.execution.PrecisionSnapshot.FIELD);
        write("telecinese: conjurou=" + r.success() + " " + r.message() + " segurando " + com.arcane.execution.Telekinesis.heldCount(sp.getUUID()));
      });
      case 590 -> { look(-20f, -10f); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
      case 600 -> shot("telekinesis_back");
      case 602 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
      case 612 -> shot("telekinesis_first_person");
      case 615 -> server(sp -> com.arcane.execution.Telekinesis.release(sp.getUUID()));
      // ---- crescer
      case 630 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = sp.blockPosition().offset(-6, 0, -6);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
          l.setBlock(o.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState(), 3);
          l.setBlock(o.offset(x, 0, z), Blocks.WHEAT.defaultBlockState(), 3);
        }
        l.setBlock(o.offset(2, -1, 2), Blocks.WATER.defaultBlockState(), 3);
        sp.connection.teleport(o.getX() + 2.5, o.getY() + 0.1, o.getZ() - 2.5, 0f, 40f);
      });
      case 645 -> shot("grow_before");
      case 650 -> server(sp -> {
        var spell = new Inscription(List.of(g("self"), g("grow", "radius", 5.0, "strength", 8.0)));
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ() + 3, 0f, 40f);
        var r = com.arcane.execution.CastingService.tryCast(sp, spell, com.arcane.execution.PrecisionSnapshot.FIELD);
        write("crescer: conjurou=" + r.success() + " " + r.message());
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ() - 3, 0f, 40f);
      });
      case 675 -> shot("grow_after");
      // ---- covil da Hidra: vai ate o primeiro covil, espera construir e chega na porta da camara
      case 690 -> server(sp -> {
        var data = HydraLairs.get(sp.server.overworld());
        data.placeIfNeeded(sp.server.overworld());
        var lair = data.lairs().get(0);
        write("covil 0 em " + lair.centre + " (construido=" + lair.built + ", estagio=" + lair.stage + "); revelados=" + data.revealedCount());
        sp.connection.teleport(lair.centre.getX() + HydraLairs.CHAMBER_R + 24 + 0.5, 200, lair.centre.getZ() + 0.5, 90f, 30f);
      });
      case 760 -> server(sp -> {
        var lair = HydraLairs.get(sp.server.overworld()).lairs().get(0);
        if ((!lair.built || lair.centre.getY() == 0) && ++lairWaits < 40) { write("covil ainda gerando..."); t = 740; return; }
        lairCentre = lair.centre;
        write("covil 0 depois de carregar: " + lair.centre + " construido=" + lair.built);
        int top = sp.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, lair.centre.getX() + HydraLairs.CHAMBER_R + 24, lair.centre.getZ());
        sp.connection.teleport(lair.centre.getX() + HydraLairs.CHAMBER_R + 27.5, top + 1, lair.centre.getZ() + 0.5, 90f, 15f);
      });
      case 800 -> shot("lair_entrance");
      case 805 -> server(sp -> {
        if (lairCentre == null) return;
        sp.connection.teleport(lairCentre.getX() + HydraLairs.CHAMBER_R + 6.5, lairCentre.getY() + 0.1, lairCentre.getZ() + 0.5, 90f, 5f);
      });
      case 830 -> shot("lair_corridor");
      case 835 -> server(sp -> {
        if (lairCentre == null) return;
        sp.connection.teleport(lairCentre.getX() + HydraLairs.CHAMBER_R + 1.5, lairCentre.getY() + 0.1, lairCentre.getZ() + 0.5, 90f, 0f);
      });
      case 880 -> server(sp -> {
        var data = HydraLairs.get(sp.server.overworld());
        var lair = data.lairs().get(0);
        var hydras = sp.serverLevel().getEntitiesOfClass(HydraEntity.class, new net.minecraft.world.phys.AABB(lair.centre).inflate(30));
        write("covil 0 revelado: estagio=" + lair.stage + " ordem=" + lair.revealOrder + " hidras na camara=" + hydras.size()
            + (hydras.isEmpty() ? "" : " (estagio da entidade " + hydras.get(0).stage() + ", cabecas " + hydras.get(0).heads() + ", vida efetiva " + (int) hydras.get(0).effectiveMaxHealth() + ")"));
        for (var h : hydras) h.setNoAi(true);
        sp.connection.teleport(lair.centre.getX() + 9.5, lair.centre.getY() + 4, lair.centre.getZ() + 0.5, 90f, 15f);
      });
      case 910 -> shot("lair_chamber");
      case 915 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
      case 920 -> {
        cmd("gamemode creative");
        mc.options.hideGui = false;
        mc.options.pauseOnLostFocus = oldPause;
        write("fim do roteiro");
        armed = false;
      }
      default -> {
      }
    }
  }
}
