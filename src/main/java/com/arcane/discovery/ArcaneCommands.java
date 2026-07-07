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
        .then(Commands.literal("know")
            .then(Commands.argument("glyph", StringArgumentType.greedyString())
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
