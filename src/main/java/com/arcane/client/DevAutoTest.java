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
      // 1) a pena escreve na bancada; o livro fica la ate o dono voltar
      case 60 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        BlockPos o = BlockPos.containing(base);
        for (int x = -24; x <= 24; x++) for (int z = -4; z <= 40; z++) {
          l.setBlock(o.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
          for (int y = 0; y < 8; y++) l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        BlockPos bench = o.offset(0, 0, 3);
        l.setBlock(bench, com.arcane.registry.ModBlocks.ARCANE_WORKBENCH.get().defaultBlockState(), 3);
        com.arcane.grimoire.GrimoireLifecycleHandler.spawnGrimoire(sp);
        com.arcane.grimoire.GrimoireDesk.startWriting(sp, bench, 6);
        write("escrevendo: bloqueio=" + com.arcane.grimoire.GrimoireDesk.blockedReason(sp));
      });
      case 62 -> tp(base.x + 0.5, base.y, base.z + 1.2, 0f, 35f);
      case 80 -> shot("desk_writing_1");
      case 95 -> shot("desk_writing_2");
      case 140 -> tp(base.x + 0.5, base.y, base.z + 14, 180f, 10f);
      case 160 -> { shot("desk_left_behind"); server(sp -> write("longe: bloqueio=" + com.arcane.grimoire.GrimoireDesk.blockedReason(sp))); }
      case 165 -> tp(base.x + 0.5, base.y, base.z + 1.0, 0f, 20f);
      case 175 -> server(sp -> write("perto de novo: bloqueio=" + com.arcane.grimoire.GrimoireDesk.blockedReason(sp)));
      // 2) invisibilidade: parado quase some; andando, a distorcao aparece
      case 180 -> server(sp -> {
        var l = sp.serverLevel();
        var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(l);
        z.moveTo(base.x + 0.5, base.y, base.z + 6, 180f, 0);
        z.setNoAi(true);
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        z.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 2000, 0, false, false, true));
        z.setPersistenceRequired();
        l.addFreshEntity(z);
        var visible = net.minecraft.world.entity.EntityType.ZOMBIE.create(l);
        visible.moveTo(base.x + 3.5, base.y, base.z + 6, 180f, 0);
        visible.setNoAi(true);
        visible.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
        l.addFreshEntity(visible);
      });
      case 182 -> tp(base.x + 1.5, base.y, base.z + 1.5, 0f, 5f);
      case 200 -> shot("invisible_still");
      case 230 -> shot("invisible_moving");
      // 3) Ilusao: 3 clones com a skin do jogador
      case 250 -> {
        cmd("kill @e[type=minecraft:zombie]");
        server(sp -> {
          sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
          sp.getInventory().add(new ItemStack(net.minecraft.world.item.Items.IRON_AXE));
          sp.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BOW));
          var r = com.arcane.execution.CastingService.tryCast(sp, new Inscription(List.of(g("self"), g("illusion", "duration", 600))),
              com.arcane.execution.PrecisionSnapshot.WORKBENCH);
          write("ilusao: " + r.success() + " " + r.message() + " clones=" + sp.serverLevel().getEntitiesOfClass(com.arcane.entity.IllusionEntity.class, sp.getBoundingBox().inflate(8)).size());
        });
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      }
      case 275 -> shot("illusion_front");
      case 277 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      case 290 -> shot("illusion_back");
      case 292 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
      // 4) mana vazando de um circulo de raio 16
      case 300 -> server(sp -> {
        var l = sp.serverLevel();
        BlockPos altar = BlockPos.containing(base).offset(0, 0, 22);
        l.setBlock(altar, com.arcane.registry.ModBlocks.ARCANE_ALTAR.get().defaultBlockState(), 3);
        com.arcane.ritual.RitualService.drawPerfectCircle(l, altar, 16);
        write("circulo: raio que vaza=" + com.arcane.ritual.RitualLeaks.leakingRadius(l, altar) + " registrado=" + com.arcane.ritual.RitualLeaks.registered(l, altar));
      });
      case 302 -> tp(base.x + 0.5, base.y + 6, base.z - 2, 0f, 15f);
      case 360 -> shot("ritual_leak");
      case 380 -> {
        cmd("gamemode creative");
        mc.options.hideGui = false;
        mc.options.pauseOnLostFocus = oldPause;
        write("fim do roteiro");
        armed = false;
      }
      default -> {
      }
    }
    // a criatura invisivel anda de um lado para o outro (distorcao em movimento)
    if (t >= 205 && t < 232) {
      int now = t;
      server(sp -> {
        for (var z : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, sp.getBoundingBox().inflate(12))) {
          if (z.isInvisible()) z.teleportTo(base.x + 0.5 + Math.sin(now * 0.35) * 1.5, base.y, base.z + 6);
        }
      });
    }
  }
}
