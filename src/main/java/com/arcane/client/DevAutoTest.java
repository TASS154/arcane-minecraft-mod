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
    if (mc.screen != null && !(mc.screen instanceof com.arcane.client.screen.RadialScreen)) mc.setScreen(null);
    t++;
    step(mc);
  }

  /** Manda direto o visual de um glifo original (o efeito de jogo e coberto pelos GameTests). */
  private static void fx(int kind, Vec3 at, Vec3 dir, float radius, int ticks) {
    server(sp -> com.arcane.network.ModNetwork.sendShape(sp.serverLevel(), com.arcane.network.packet.S2CShapeFxPacket.ORIGINAL, at, dir, radius, 0xFFFFFF, kind, ticks));
  }

  private static HydraEntity sealedHydra;
  private static net.minecraft.world.entity.animal.Pig bait;

  private static void step(Minecraft mc) {
    var p = mc.player;
    Vec3 stage = base == null ? Vec3.ZERO : base.add(0, 0, 18);
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
        // plataforma de pedra limpa
        ServerLevel l = sp.serverLevel();
        BlockPos o = sp.blockPosition();
        for (int x = -30; x <= 30; x++) for (int z = -4; z <= 60; z++) {
          l.setBlock(o.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
          for (int y = 0; y < 22; y++) l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        for (int i = 0; i < 5; i++) {
          var pig = net.minecraft.world.entity.EntityType.PIG.create(l);
          pig.moveTo(o.getX() + (i - 2) * 3 + 0.5, o.getY() + 2 + i % 2, o.getZ() + 18.5, 0f, 0f);
          pig.addTag("arcane_autotest");
          l.addFreshEntity(pig);
        }
        write("plataforma pronta");
      });
      case 70 -> tp(base.x, base.y + 9, base.z + 2, 0f, 28f);
      // ---- Parar o Tempo de verdade (conjurado no centro): cupula, relogio e mundo cinza
      case 100 -> server(sp -> {
        sp.connection.teleport(stage.x, stage.y, stage.z - 6, 0f, 20f);
        var r = com.arcane.execution.CastingService.tryCast(sp, new Inscription(List.of(g("self"), g("time_stop", "radius", 12.0, "duration", 120.0))),
            com.arcane.execution.PrecisionSnapshot.FIELD);
        write("parar o tempo: conjurou=" + r.success() + " " + r.message());
      });
      case 102 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      case 130 -> shot("original_time_stop");
      case 106, 116, 126, 136 -> write("t" + t + " filtro cinza: nosso=" + com.arcane.client.render.OriginalFxRenderer.greyOn() + " efeito=" + mc.gameRenderer.currentEffect()
          + " efeitos ativos=" + com.arcane.client.render.OriginalFxRenderer.active());
      case 140 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); tp(base.x, base.y + 9, base.z + 2, 0f, 28f); }
      case 230 -> write("filtro cinza depois do fim: " + (mc.gameRenderer.currentEffect() != null));
      case 232 -> fx(com.arcane.network.packet.S2CShapeFxPacket.FX_REWIND, stage.add(-4, 0, 0), stage.add(6, 0, 3), 1, 30);
      case 244 -> shot("original_rewind");
      case 260 -> fx(com.arcane.network.packet.S2CShapeFxPacket.FX_SPACE_COLLAPSE, stage.add(0, 2, 0), Vec3.ZERO, 12, 44);
      case 276 -> shot("original_space_collapse");
      case 310 -> fx(com.arcane.network.packet.S2CShapeFxPacket.FX_ANNIHILATE, stage.add(0, 3, 0), Vec3.ZERO, 7, 40);
      case 312 -> shot("original_annihilate_flash");
      case 322 -> shot("original_annihilate_void");
      case 360 -> server(sp -> {
        // o Eclipse e no ceu: vai para a superficie
        int x = (int) base.x + 200, z = (int) base.z;
        int y = sp.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        int py = sp.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z - 22);
        sp.connection.teleport(x + 0.5, Math.max(y, py) + 1, z - 21.5, 0f, -32f);
        com.arcane.network.ModNetwork.sendShape(sp.serverLevel(), com.arcane.network.packet.S2CShapeFxPacket.ORIGINAL, new Vec3(x + 0.5, y, z + 0.5), Vec3.ZERO, 30, 0xFFFFFF,
            com.arcane.network.packet.S2CShapeFxPacket.FX_ECLIPSE, 140);
      });
      case 362 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
      case 466 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
      case 420 -> shot("original_eclipse");
      case 470 -> { tp(base.x, base.y + 9, base.z + 2, 0f, 28f); fx(com.arcane.network.packet.S2CShapeFxPacket.FX_GENESIS, stage, Vec3.ZERO, 10, 70); }
      case 505 -> shot("original_genesis");
      case 545 -> fx(com.arcane.network.packet.S2CShapeFxPacket.FX_GRAVITY, stage, Vec3.ZERO, 9, 100);
      case 590 -> shot("original_gravity");
      // ---- Hidra estagio 7 selada por pilares; quebra os selos; desperta; devasta
      case 650 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        for (var e : l.getEntities((net.minecraft.world.entity.Entity) null, sp.getBoundingBox().inflate(80), e -> e.getTags().contains("arcane_autotest"))) e.discard();
        BlockPos c = BlockPos.containing(stage.add(0, 0, 6));
        for (int i = 0; i < HydraEntity.PILLARS; i++) {
          BlockPos top = HydraEntity.pillarTop(i);
          for (int y = 0; y < top.getY(); y++) l.setBlock(c.offset(top.getX(), y, top.getZ()), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
          l.setBlock(c.offset(top), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        }
        HydraEntity h = ModEntities.HYDRA.get().create(l);
        h.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180f, 0f);
        h.setStage(7);
        h.homeTo(c, 12);
        h.yBodyRot = 180f;
        h.yHeadRot = 180f;
        int mask = HydraEntity.sealMaskFor(7);
        for (int i = 0; i < HydraEntity.PILLARS; i++) if ((mask & (1 << i)) != 0) l.setBlock(c.offset(HydraEntity.pillarTop(i)), com.arcane.registry.ModBlocks.HYDRA_SEAL.get().defaultBlockState(), 3);
        h.setSealMask(HydraLairs.sealMaskAt(l, c));
        h.addTag("arcane_autotest");
        l.addFreshEntity(h);
        sealedHydra = h;
        write("hidra selada: estagio 7, selos=" + Integer.bitCount(h.sealMask()) + ", sem IA=" + h.isNoAi());
        sp.connection.teleport(c.getX() + 0.5, c.getY() + 7, c.getZ() - 17.5, 0f, 18f);
      });
      case 690 -> shot("hydra_sealed");
      case 695 -> server(sp -> {
        float hp = sealedHydra.getHealth();
        sealedHydra.hurt(sp.serverLevel().damageSources().playerAttack(sp), 50f);
        write("golpe na hidra selada: vida " + hp + " -> " + sealedHydra.getHealth());
        // quebra todos os selos menos um
        BlockPos c = sealedHydra.home();
        int left = Integer.bitCount(sealedHydra.sealMask());
        for (int i = 0; i < HydraEntity.PILLARS && left > 1; i++) {
          if ((sealedHydra.sealMask() & (1 << i)) == 0) continue;
          sp.serverLevel().destroyBlock(c.offset(HydraEntity.pillarTop(i)), false, sp);
          left--;
        }
        write("selos depois de quebrar: " + Integer.bitCount(sealedHydra.sealMask()) + ", selada=" + sealedHydra.sealed());
      });
      case 702 -> shot("hydra_seal_break");
      case 730 -> server(sp -> {
        BlockPos c = sealedHydra.home();
        for (int i = 0; i < HydraEntity.PILLARS; i++) if ((sealedHydra.sealMask() & (1 << i)) != 0) sp.serverLevel().destroyBlock(c.offset(HydraEntity.pillarTop(i)), false, sp);
        write("ultimo selo quebrado: selada=" + sealedHydra.sealed() + ", sem IA=" + sealedHydra.isNoAi());
      });
      case 738 -> shot("hydra_awaken");
      case 760 -> server(sp -> {
        ServerLevel l = sp.serverLevel();
        sealedHydra.setNoAi(true);
        bait = net.minecraft.world.entity.EntityType.PIG.create(l);
        bait.moveTo(sealedHydra.getX() + 10, sealedHydra.getY(), sealedHydra.getZ() - 8, 0f, 0f);
        bait.setNoAi(true);
        bait.addTag("arcane_autotest");
        l.addFreshEntity(bait);
        sealedHydra.use(HydraEntity.DEVASTATE, bait);
        write("devastacao anunciada: golpes pendentes=" + sealedHydra.pendingStrikes());
      });
      case 775 -> shot("hydra_devastate_warning");
      case 793 -> shot("hydra_devastate_impact");
      case 840 -> {
        shot("hydra_crater");
        server(sp -> write("depois do golpe: pendentes=" + sealedHydra.pendingStrikes() + ", porco vivo=" + bait.isAlive()));
      }
      case 860 -> server(sp -> {
        for (var e : sp.serverLevel().getEntities((net.minecraft.world.entity.Entity) null, sp.getBoundingBox().inflate(80), e -> e.getTags().contains("arcane_autotest"))) e.discard();
      });
      case 870 -> {
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
