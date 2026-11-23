package com.arcane.item;

import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.Inscription;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Cetro: guarda ate 6 magias e as conjura com 120% de forca.
 * <ul>
 *   <li>Botao direito: conjura a magia escolhida do cetro.</li>
 *   <li>Agachado + botao direito: passa para a proxima magia guardada.</li>
 *   <li>Agachado + botao direito com o grimorio aberto numa magia (a magia ativa) e o cetro sem ela: grava a magia
 *       ativa do grimorio num espaco livre (ou no lugar da escolhida, se os 6 estiverem cheios). Ver {@link #store}.</li>
 *   <li>Aparencia: com o cetro na mao principal e uma gema/material na secundaria, agachar + botao direito troca a
 *       ponta (gemas) ou a haste (materiais). A troca gasta 1 item.</li>
 * </ul>
 */
public class ScepterItem extends Item {
  public static final int SLOTS = 6;
  public static final double POWER = 1.2;
  private static final String SPELLS = "scepter_spells", SELECTED = "scepter_selected", HEAD = "scepter_head", SHAFT = "scepter_shaft";

  /** Pontas: o indice vira o predicado "arcane:head" (0 = ametista, o padrao). */
  public static final List<Item> HEADS = List.of(Items.AMETHYST_SHARD, Items.DIAMOND, Items.EMERALD, Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.ENDER_PEARL, Items.NETHER_STAR);
  public static final List<String> HEAD_NAMES = List.of("amethyst", "diamond", "emerald", "redstone", "lapis", "quartz", "ender", "star");
  /** Hastes: o indice vira o predicado "arcane:shaft" (0 = madeira escura, o padrao). */
  public static final List<Item> SHAFTS = List.of(Items.DARK_OAK_LOG, Items.GOLD_INGOT, Items.IRON_INGOT, Items.BONE, Items.BLAZE_ROD, Items.OBSIDIAN);
  public static final List<String> SHAFT_NAMES = List.of("wood", "gold", "iron", "bone", "blaze", "obsidian");

  public ScepterItem(Properties properties) {
    super(properties.stacksTo(1));
  }

  public static List<Inscription> spells(ItemStack stack) {
    List<Inscription> out = new ArrayList<>();
    if (!stack.hasTag() || !stack.getTag().contains(SPELLS, Tag.TAG_LIST)) return out;
    for (Tag t : stack.getTag().getList(SPELLS, Tag.TAG_COMPOUND)) Inscription.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(out::add);
    return out;
  }

  private static void setSpells(ItemStack stack, List<Inscription> spells) {
    ListTag list = new ListTag();
    for (Inscription i : spells) Inscription.CODEC.encodeStart(NbtOps.INSTANCE, i).result().ifPresent(list::add);
    stack.getOrCreateTag().put(SPELLS, list);
  }

  public static int selected(ItemStack stack) {
    int n = spells(stack).size();
    return n == 0 ? 0 : Math.floorMod(stack.getOrCreateTag().getInt(SELECTED), n);
  }

  public static int head(ItemStack stack) { return stack.hasTag() ? stack.getTag().getInt(HEAD) : 0; }

  public static int shaft(ItemStack stack) { return stack.hasTag() ? stack.getTag().getInt(SHAFT) : 0; }

  /** Grava uma magia: espaco livre se houver; cheio, substitui a escolhida. Mesma magia (mesmo id) nao duplica. */
  public static int store(ItemStack stack, Inscription spell) {
    List<Inscription> all = spells(stack);
    for (int i = 0; i < all.size(); i++) {
      if (all.get(i).id().equals(spell.id())) {
        all.set(i, spell);
        setSpells(stack, all);
        stack.getOrCreateTag().putInt(SELECTED, i);
        return i;
      }
    }
    int at;
    if (all.size() < SLOTS) {
      all.add(spell);
      at = all.size() - 1;
    } else {
      at = selected(stack);
      all.set(at, spell);
    }
    setSpells(stack, all);
    stack.getOrCreateTag().putInt(SELECTED, at);
    return at;
  }

  public static void cycle(ItemStack stack) {
    int n = spells(stack).size();
    if (n > 0) stack.getOrCreateTag().putInt(SELECTED, (selected(stack) + 1) % n);
  }

  /** Troca a aparencia com o item da mao secundaria; devolve verdadeiro se trocou. */
  public static boolean restyle(ItemStack scepter, ItemStack material) {
    int h = HEADS.indexOf(material.getItem());
    int s = SHAFTS.indexOf(material.getItem());
    if (h >= 0 && h != head(scepter)) { scepter.getOrCreateTag().putInt(HEAD, h); return true; }
    if (s >= 0 && s != shaft(scepter)) { scepter.getOrCreateTag().putInt(SHAFT, s); return true; }
    return false;
  }

  private static String nameOf(Inscription i) {
    String n = i.metadata().name();
    return n == null || n.isEmpty() ? Component.translatable("gui.arcane.unnamed").getString() : n;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
    if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    if (player.isShiftKeyDown()) {
      ItemStack off = player.getOffhandItem();
      if (!off.isEmpty() && restyle(stack, off)) {
        if (!player.getAbilities().instabuild) off.shrink(1);
        sp.displayClientMessage(Component.translatable("message.arcane.scepter.restyled").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResultHolder.success(stack);
      }
      // Magia ativa do grimorio que ainda nao esta no cetro: grava. Ja esta (ou nao ha): passa para a proxima.
      var grimoire = GrimoireAttachments.get(sp);
      var active = grimoire.flatMap(g -> g.activeInscriptionId().flatMap(g::getInscription));
      if (active.isPresent() && spells(stack).stream().noneMatch(i -> i.id().equals(active.get().id()))) {
        int at = store(stack, active.get());
        sp.displayClientMessage(Component.translatable("message.arcane.scepter.stored", nameOf(active.get()), at + 1, SLOTS).withStyle(ChatFormatting.AQUA), true);
        return InteractionResultHolder.success(stack);
      }
      cycle(stack);
      List<Inscription> all = spells(stack);
      if (all.isEmpty()) sp.displayClientMessage(Component.translatable("message.arcane.scepter.empty").withStyle(ChatFormatting.GRAY), true);
      else sp.displayClientMessage(Component.translatable("message.arcane.scepter.selected", nameOf(all.get(selected(stack))), selected(stack) + 1, all.size()).withStyle(ChatFormatting.AQUA), true);
      return InteractionResultHolder.success(stack);
    }
    List<Inscription> all = spells(stack);
    if (all.isEmpty()) {
      sp.displayClientMessage(Component.translatable("message.arcane.scepter.empty").withStyle(ChatFormatting.GRAY), true);
      return InteractionResultHolder.fail(stack);
    }
    Inscription spell = all.get(selected(stack));
    var result = CastingService.tryCast(sp, spell, new PrecisionSnapshot(POWER * PrecisionSnapshot.of(spell).powerMultiplier(), 1.0, false));
    if (!result.success() && result.message() != null) sp.displayClientMessage(Component.translatable(result.message()).withStyle(ChatFormatting.RED), true);
    return InteractionResultHolder.consume(stack);
  }

  @Override
  public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
    List<Inscription> all = spells(stack);
    tooltip.add(Component.translatable("tooltip.arcane.scepter.power", (int) Math.round(POWER * 100)).withStyle(ChatFormatting.GOLD));
    tooltip.add(Component.translatable("tooltip.arcane.scepter.look",
        Component.translatable("scepter.arcane.head." + HEAD_NAMES.get(Math.min(head(stack), HEAD_NAMES.size() - 1))),
        Component.translatable("scepter.arcane.shaft." + SHAFT_NAMES.get(Math.min(shaft(stack), SHAFT_NAMES.size() - 1)))).withStyle(ChatFormatting.DARK_PURPLE));
    int sel = selected(stack);
    for (int i = 0; i < all.size(); i++) {
      tooltip.add(Component.literal((i == sel ? "▶ " : "  ") + (i + 1) + ". " + nameOf(all.get(i))).withStyle(i == sel ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }
    for (int i = all.size(); i < SLOTS; i++) tooltip.add(Component.literal("  " + (i + 1) + ". —").withStyle(ChatFormatting.DARK_GRAY));
    tooltip.add(Component.translatable("tooltip.arcane.scepter.help").withStyle(ChatFormatting.DARK_GRAY));
  }
}
