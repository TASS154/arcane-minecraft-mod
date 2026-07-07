package com.arcane.inscribing;

import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

public final class ItemInscriptionHelper {
  private static final String TAG = "arcane_inscription";

  private ItemInscriptionHelper() {}

  public static void write(ItemStack stack, Inscription inscription) {
    Inscription.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, inscription)
        .result().ifPresent(tag -> stack.getOrCreateTag().put(TAG, tag));
  }

  public static Inscription read(ItemStack stack) {
    if (!stack.hasTag() || !stack.getTag().contains(TAG)) return null;
    return Inscription.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, stack.getTag().get(TAG))
        .result().orElse(null);
  }

  public static boolean isScroll(ItemStack stack) {
    return stack.is(Items.PAPER) && stack.hasTag() && stack.getTag().contains(TAG);
  }
}
