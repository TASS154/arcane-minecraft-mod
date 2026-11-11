package com.arcane.discovery;

import com.arcane.ArcaneMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public class ArcaneCommands {
  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("arcane")
        .then(Commands.literal("chamber")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
              ServerPlayer p = ctx.getSource().getPlayerOrException();
              var floor = p.blockPosition();
              com.arcane.world.SealedChambers.build(p.serverLevel(), floor, p.getRandom());
              p.teleportTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() - 3.5);
              ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("message.arcane.chamber.built"), true);
              return 1;
            }))
        .then(Commands.literal("mana")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("max")
                .then(Commands.argument("players", net.minecraft.commands.arguments.EntityArgument.players())
                    .then(Commands.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1f, 100000f))
                        .executes(ctx -> {
                          float value = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
                          int n = 0;
                          for (ServerPlayer p : net.minecraft.commands.arguments.EntityArgument.getPlayers(ctx, "players")) {
                            com.arcane.grimoire.GrimoireAttachments.getMana(p).ifPresent(m -> m.setBaseMax(value));
                            com.arcane.execution.ManaBonuses.refresh(p);
                            com.arcane.network.ArcaneSync.sync(p, true);
                            n++;
                          }
                          final int count = n;
                          ctx.getSource().sendSuccess(() -> Component.literal("Mana maxima base = " + value + " (" + count + " jogador(es)); os bonus de glifos e pocoes somam por cima"), true);
                          return count;
                        }))))
            .then(Commands.literal("set")
                .then(Commands.argument("players", net.minecraft.commands.arguments.EntityArgument.players())
                    .then(Commands.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0f, 100000f))
                        .executes(ctx -> {
                          float value = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
                          int n = 0;
                          for (ServerPlayer p : net.minecraft.commands.arguments.EntityArgument.getPlayers(ctx, "players")) {
                            com.arcane.grimoire.GrimoireAttachments.getMana(p).ifPresent(m -> m.set(value));
                            com.arcane.network.ArcaneSync.sync(p, true);
                            n++;
                          }
                          return n;
                        }))))
            .then(Commands.literal("get")
                .executes(ctx -> {
                  if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                  com.arcane.execution.ManaBonuses.refresh(p);
                  com.arcane.grimoire.GrimoireAttachments.getMana(p).ifPresent(m -> ctx.getSource().sendSuccess(() -> Component.literal(
                      "Mana " + Math.round(m.current()) + "/" + Math.round(m.max()) + " (base " + Math.round(m.baseMax()) + " + glifos " + Math.round(m.glyphBonus())
                      + " [" + com.arcane.execution.ManaBonuses.discoveredGlyphs(p) + " descobertos] + pocao " + Math.round(m.potionBonus()) + ", regen x" + m.regenMultiplier() + ")"), false));
                  return 1;
                })))
        .then(Commands.literal("know")
            .then(Commands.literal("all")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                  if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    com.arcane.glyph.GlyphRegistry.all().keySet().forEach(id -> DiscoveryService.learnMastered(player, id));
                    ctx.getSource().sendSuccess(() -> Component.literal("Learned " + com.arcane.glyph.GlyphRegistry.size() + " glyphs"), true);
                    return 1;
                  }
                  return 0;
                }))
            .then(Commands.argument("glyph", StringArgumentType.greedyString())
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                  if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    ResourceLocation glyph = new ResourceLocation(StringArgumentType.getString(ctx, "glyph"));
                    DiscoveryService.learnMastered(player, glyph);
                    ctx.getSource().sendSuccess(() -> Component.literal("Learned " + glyph), true);
                    return 1;
                  }
                  return 0;
                }))));
  }
}
