package com.arcane.client;

import com.arcane.ArcaneMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onKey(InputEvent.Key event) {
    if (ClientSetup.CAST_KEY != null) {
      while (ClientSetup.CAST_KEY.consumeClick()) {
        ClientSetup.tryCast(false);
      }
    }
    if (ClientSetup.GRIMOIRE_KEY != null) {
      while (ClientSetup.GRIMOIRE_KEY.consumeClick()) {
        ClientHooks.openGrimoire();
      }
    }
    if (ClientSetup.CYCLE_KEY != null) {
      while (ClientSetup.CYCLE_KEY.consumeClick()) {
        ClientSetup.cycleSpell(1);
      }
    }
  }

  @SubscribeEvent
  public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
    ClientArcaneData.reset();
  }

  /** Lore dos itens (chave item.arcane.<nome>.desc, linhas separadas por \n) e glifo das notas. */
  @SubscribeEvent
  public static void onTooltip(ItemTooltipEvent event) {
    ItemStack stack = event.getItemStack();
    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
    if (id == null || !ArcaneMod.MODID.equals(id.getNamespace())) return;

    if (stack.hasTag() && stack.getTag().contains("glyph")) {
      ResourceLocation glyph = ResourceLocation.tryParse(stack.getTag().getString("glyph"));
      if (glyph != null) {
        event.getToolTip().add(Component.translatable("tooltip.arcane.glyph_note.glyph",
            Component.translatable("glyph." + glyph.getNamespace() + "." + glyph.getPath())).withStyle(ChatFormatting.AQUA));
      }
    }

    var contents = com.arcane.item.GrimoireItem.contents(stack);
    if (contents != null) {
      event.getToolTip().add(Component.translatable("tooltip.arcane.grimoire.contents",
          contents.getList("spellbook", net.minecraft.nbt.Tag.TAG_COMPOUND).size(),
          contents.getCompound("knowledge").getAllKeys().size()).withStyle(ChatFormatting.LIGHT_PURPLE));
      event.getToolTip().add(Component.translatable("tooltip.arcane.grimoire.shift").withStyle(ChatFormatting.DARK_GRAY));
    }
    if (stack.hasTag() && stack.getTag().contains(com.arcane.item.GrimoireItem.OWNER_NAME)) {
      event.getToolTip().add(Component.translatable("tooltip.arcane.grimoire.owner",
          stack.getTag().getString(com.arcane.item.GrimoireItem.OWNER_NAME)).withStyle(ChatFormatting.DARK_PURPLE));
    }

    String key = stack.getDescriptionId() + ".desc";
    if (I18n.exists(key)) {
      for (String line : I18n.get(key).split("\n")) {
        event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
      }
    }
  }
}
