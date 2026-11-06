package com.arcane.item;

import com.arcane.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;

/**
 * Item do Grimorio. Botao direito abre a tela do grimorio (mesma da tecla G).
 * O item que cai quando o dono morre leva o UUID dele: so o dono o recupera de volta.
 */
public class GrimoireItem extends Item {
  public static final String OWNER = "owner";
  public static final String OWNER_NAME = "ownerName";

  public GrimoireItem(Properties properties) {
    super(properties);
  }

  public static ItemStack createFor(Player owner) {
    ItemStack stack = new ItemStack(ModItems.GRIMOIRE.get());
    stack.getOrCreateTag().putUUID(OWNER, owner.getUUID());
    stack.getOrCreateTag().putString(OWNER_NAME, owner.getGameProfile().getName());
    return stack;
  }

  public static boolean isOwnedBy(ItemStack stack, UUID player) {
    return stack.is(ModItems.GRIMOIRE.get()) && stack.hasTag() && stack.getTag().hasUUID(OWNER)
        && stack.getTag().getUUID(OWNER).equals(player);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (level.isClientSide) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.arcane.client.ClientHooks::openGrimoire);
    }
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
  }
}
