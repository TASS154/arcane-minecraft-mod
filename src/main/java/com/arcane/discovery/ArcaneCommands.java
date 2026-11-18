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
        .then(Commands.literal("rules")
            .requires(src -> src.hasPermission(2) || (src.getEntity() instanceof ServerPlayer sp && com.arcane.config.WorldRules.canEdit(sp)))
            .executes(ctx -> {
              ServerPlayer me = ctx.getSource().getPlayerOrException();
              com.arcane.network.ModNetwork.sendToPlayer(me, new com.arcane.network.packet.S2CWorldRulesPacket(
                  com.arcane.config.WorldRules.snapshot(), com.arcane.config.WorldRules.canEdit(me)));
              return 1;
            }))
        .then(Commands.literal("guest")
            .then(Commands.literal("add")
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                    .executes(ctx -> {
                      ServerPlayer me = ctx.getSource().getPlayerOrException();
                      ServerPlayer other = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                      boolean ok = com.arcane.execution.GuestLists.get(ctx.getSource().getServer()).add(me.getUUID(), other.getUUID());
                      ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "message.arcane.guest.added" : "message.arcane.guest.unchanged", other.getDisplayName()), false);
                      return ok ? 1 : 0;
                    })))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                    .executes(ctx -> {
                      ServerPlayer me = ctx.getSource().getPlayerOrException();
                      ServerPlayer other = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                      boolean ok = com.arcane.execution.GuestLists.get(ctx.getSource().getServer()).remove(me.getUUID(), other.getUUID());
                      ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "message.arcane.guest.removed" : "message.arcane.guest.unchanged", other.getDisplayName()), false);
                      return ok ? 1 : 0;
                    })))
            .then(Commands.literal("list")
                .executes(ctx -> {
                  ServerPlayer me = ctx.getSource().getPlayerOrException();
                  var set = com.arcane.execution.GuestLists.get(ctx.getSource().getServer()).of(me.getUUID());
                  var names = new java.util.ArrayList<String>();
                  for (java.util.UUID id : set) {
                    ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(id);
                    names.add(p != null ? p.getGameProfile().getName() : id.toString().substring(0, 8));
                  }
                  ctx.getSource().sendSuccess(() -> Component.translatable("message.arcane.guest.list", set.size(), String.join(", ", names)), false);
                  return set.size();
                })))
        .then(Commands.literal("pocket")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                .executes(ctx -> {
                  ServerPlayer admin = ctx.getSource().getPlayerOrException();
                  ServerPlayer owner = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                  com.arcane.pocket.ArcanePocket.openFor(admin, owner);
                  return 1;
                })
                .then(Commands.literal("list").executes(ctx -> {
                  ServerPlayer owner = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                  var c = com.arcane.pocket.ArcanePocket.container(owner);
                  int items = 0, used = 0;
                  for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) { items += c.getItem(i).getCount(); used++; }
                  final int fi = items, fu = used;
                  ctx.getSource().sendSuccess(() -> Component.translatable("message.arcane.pocket.listed", owner.getDisplayName(), fi, fu), false);
                  return fi;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                  ServerPlayer owner = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                  int n = com.arcane.pocket.ArcanePocket.clear(owner);
                  ctx.getSource().sendSuccess(() -> Component.translatable("message.arcane.pocket.cleared", owner.getDisplayName(), n), true);
                  return n;
                }))))
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
