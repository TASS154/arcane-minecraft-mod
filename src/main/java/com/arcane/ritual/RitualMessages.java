package com.arcane.ritual;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/** Traduz o resultado de um ritual em mensagens para o jogador. */
public final class RitualMessages {
  private RitualMessages() {}

  public static void send(ServerPlayer player, RitualService.Result r) {
    Component msg = switch (r.outcome()) {
      case NO_ALTAR -> Component.translatable("message.arcane.ritual.no_altar").withStyle(ChatFormatting.RED);
      case NO_SPELL -> Component.translatable("message.arcane.ritual.no_spell").withStyle(ChatFormatting.RED);
      case COOLDOWN -> Component.translatable("message.arcane.ritual.cooldown").withStyle(ChatFormatting.GRAY);
      case CIRCLE_INCOMPLETE -> Component.translatable("message.arcane.ritual.circle_incomplete", r.amount(),
          (int) Math.round(RitualSettings.minCompleteness() * 100)).withStyle(ChatFormatting.RED);
      case LEVEL_TOO_LOW -> Component.translatable("message.arcane.ritual.level_too_low",
          Component.translatable("ritual.arcane.level." + r.amount()),
          r.circle() == null ? "-" : Component.translatable("ritual.arcane.level." + Math.max(1, r.circle().level().number()))).withStyle(ChatFormatting.RED);
      case MISSING_COMPONENT -> Component.translatable("message.arcane.ritual.missing_component",
          Component.translatable(r.detail()), r.amount()).withStyle(ChatFormatting.RED);
      case NO_MANA -> Component.translatable("message.arcane.ritual.no_mana", r.amount()).withStyle(ChatFormatting.RED);
      case INVALID -> Component.translatable("message.arcane.ritual.invalid", r.detail()).withStyle(ChatFormatting.RED);
      case STORED -> Component.translatable("message.arcane.ritual.stored").withStyle(ChatFormatting.LIGHT_PURPLE);
      case SUCCESS -> Component.translatable("message.arcane.ritual.performed",
          Component.translatable("ritual.arcane.level." + r.effectiveLevel()),
          String.format(Locale.ROOT, "%.1f", r.power()), String.format(Locale.ROOT, "%.1f", r.area())).withStyle(ChatFormatting.GOLD);
    };
    player.displayClientMessage(msg, true);
    if (r.outcome() == RitualService.Outcome.SUCCESS && "legendary".equals(r.detail())) {
      player.displayClientMessage(Component.translatable("message.arcane.ritual.legendary").withStyle(ChatFormatting.GOLD), false);
    }
  }
}
