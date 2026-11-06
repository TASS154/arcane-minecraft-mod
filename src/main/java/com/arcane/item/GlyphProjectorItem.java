package com.arcane.item;

import com.arcane.execution.SpellElement;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.Inscription;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CShapeFxPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Projetor de Glifos: projeta no ar um holograma da magia ativa (um anel por glifo, na ordem, cada um com um numero
 * diferente de lados) e lista a sequencia na tela. Serve para conferir e mostrar uma magia sem conjura-la.
 */
public class GlyphProjectorItem extends Item {
  public GlyphProjectorItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (level.isClientSide || !(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) {
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    if (sp.isShiftKeyDown()) { // agachado: analisa o residuo magico mais proximo
      com.arcane.execution.WorldScars.analyze(sp);
      sp.getCooldowns().addCooldown(this, 20);
      return InteractionResultHolder.sidedSuccess(stack, false);
    }
    var grimoire = GrimoireAttachments.get(sp);
    Inscription spell = grimoire.flatMap(g -> g.activeInscriptionId().map(id -> g.spellbook().get(id))).orElse(null);
    if (spell == null || spell.glyphs().isEmpty()) {
      sp.displayClientMessage(Component.translatable("message.arcane.projector.none").withStyle(ChatFormatting.GRAY), true);
      return InteractionResultHolder.fail(stack);
    }
    var plan = com.arcane.inscription.CompiledPlanCache.getOrCompile(spell);
    SpellElement element = plan == null ? SpellElement.ARCANE : SpellElement.of(plan);
    Vec3 at = sp.getEyePosition().add(sp.getLookAngle().scale(2.6)).add(0, -0.4, 0);
    ModNetwork.sendShape(server, S2CShapeFxPacket.HOLOGRAM, at, Vec3.ZERO, spell.glyphs().size(), element.color(), element.ordinal(), 110);
    var names = Component.empty();
    for (int i = 0; i < spell.glyphs().size(); i++) {
      var id = spell.glyphs().get(i).glyphId();
      if (i > 0) names.append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY));
      names.append(Component.translatable("glyph." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.AQUA));
    }
    sp.displayClientMessage(names, true);
    sp.getCooldowns().addCooldown(this, 40);
    return InteractionResultHolder.sidedSuccess(stack, false);
  }
}
