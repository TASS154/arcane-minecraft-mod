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
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Cetro (cajado de mago): guarda ate 6 magias e as conjura com 120% de forca.
 * <ul>
 *   <li>Botao direito: conjura a magia escolhida.</li>
 *   <li>Agachado + botao direito: abre a tela do cetro (ScepterScreen): gravar a magia ativa do grimorio, escolher ou
 *       apagar magias e mudar a aparencia.</li>
 *   <li>Aparencia: 10 pontas x 8 hastes (modelos) x 20 cores de gema x 8 metais (pintados pelo jogo). Gratis.</li>
 * </ul>
 */
public class ScepterItem extends Item {
  public static final int SLOTS = 6;
  public static final double POWER = 1.2;
  private static final String SPELLS = "scepter_spells", SELECTED = "scepter_selected", HEAD = "scepter_head", SHAFT = "scepter_shaft",
      GEM = "scepter_gem", METAL = "scepter_metal";

  /** Pontas (predicado "arcane:head"); mesma ordem de tools/gen_scepter_models.py. */
  public static final List<String> HEAD_NAMES = List.of("crescent", "cage", "ring", "crystals", "crook", "wings", "star", "trident", "halo", "orb");
  /** Hastes (predicado "arcane:shaft"). */
  public static final List<String> SHAFT_NAMES = List.of("white", "wood", "twisted", "bone", "obsidian", "gilded", "crystal", "branch");
  /** Cores da gema (tinta 0). */
  public static final List<String> GEM_NAMES = List.of("red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta",
      "pink", "white", "light_gray", "gray", "black", "brown", "amethyst", "diamond", "emerald", "sun");
  public static final int[] GEM_COLORS = {0xE8283C, 0xF08A2A, 0xF6D93A, 0x8BE04A, 0x3E9B3A, 0x2EC4C4, 0x6CC3F5, 0x3552D8, 0x8E3FD0, 0xD24FC8,
      0xF59AC0, 0xF4F4F8, 0xB4B4BC, 0x6A6A74, 0x2A2236, 0x8A5A34, 0xB46EF0, 0x78F0E6, 0x28D264, 0xFFF4B0};
  /** Metais (tinta 1). */
  public static final List<String> METAL_NAMES = List.of("gold", "silver", "rose", "copper", "black", "white", "celestial", "crimson");
  public static final int[] METAL_COLORS = {0xFFC845, 0xD2D7E6, 0xEBA096, 0xC87846, 0x3A3444, 0xF5F5FA, 0x78C8FF, 0xAA2232};

  public static final int PART_HEAD = 0, PART_SHAFT = 1, PART_GEM = 2, PART_METAL = 3;

  public static int count(int part) {
    return switch (part) {
      case PART_HEAD -> HEAD_NAMES.size();
      case PART_SHAFT -> SHAFT_NAMES.size();
      case PART_GEM -> GEM_NAMES.size();
      default -> METAL_NAMES.size();
    };
  }

  private static String key(int part) {
    return switch (part) {
      case PART_HEAD -> HEAD;
      case PART_SHAFT -> SHAFT;
      case PART_GEM -> GEM;
      default -> METAL;
    };
  }

  public static int style(ItemStack stack, int part) {
    int v = stack.hasTag() ? stack.getTag().getInt(key(part)) : 0;
    return Math.floorMod(v, count(part));
  }

  /** Muda uma parte da aparencia (indice com volta). */
  public static void setStyle(ItemStack stack, int part, int value) {
    if (part < 0 || part > PART_METAL) return;
    stack.getOrCreateTag().putInt(key(part), Math.floorMod(value, count(part)));
  }

  public static String styleName(int part, int value) {
    return switch (part) {
      case PART_HEAD -> "scepter.arcane.head." + HEAD_NAMES.get(value);
      case PART_SHAFT -> "scepter.arcane.shaft." + SHAFT_NAMES.get(value);
      case PART_GEM -> "scepter.arcane.gem." + GEM_NAMES.get(value);
      default -> "scepter.arcane.metal." + METAL_NAMES.get(value);
    };
  }

  /** Cor de cada tinta do modelo: 0 = gema, 1 = metal. */
  public static int tint(ItemStack stack, int index) {
    if (index == 0) return GEM_COLORS[style(stack, PART_GEM)];
    if (index == 1) return METAL_COLORS[style(stack, PART_METAL)];
    return 0xFFFFFF;
  }

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

  public static int head(ItemStack stack) { return style(stack, PART_HEAD); }

  public static int shaft(ItemStack stack) { return style(stack, PART_SHAFT); }

  public static void select(ItemStack stack, int index) {
    int n = spells(stack).size();
    if (index >= 0 && index < n) stack.getOrCreateTag().putInt(SELECTED, index);
  }

  public static void remove(ItemStack stack, int index) {
    List<Inscription> all = spells(stack);
    if (index < 0 || index >= all.size()) return;
    all.remove(index);
    setSpells(stack, all);
    stack.getOrCreateTag().putInt(SELECTED, Math.max(0, Math.min(selected(stack), all.size() - 1)));
  }

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

  private static String nameOf(Inscription i) {
    String n = i.metadata().name();
    return n == null || n.isEmpty() ? Component.translatable("gui.arcane.unnamed").getString() : n;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
    if (level.isClientSide) {
      if (player.isShiftKeyDown()) net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
          () -> com.arcane.client.ClientHooks::openScepter);
      return InteractionResultHolder.sidedSuccess(stack, true);
    }
    if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
    if (player.isShiftKeyDown()) return InteractionResultHolder.success(stack); // a tela abre no cliente
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
        Component.translatable(styleName(PART_HEAD, head(stack))), Component.translatable(styleName(PART_SHAFT, shaft(stack))),
        Component.translatable(styleName(PART_GEM, style(stack, PART_GEM))), Component.translatable(styleName(PART_METAL, style(stack, PART_METAL))))
        .withStyle(ChatFormatting.DARK_PURPLE));
    int sel = selected(stack);
    for (int i = 0; i < all.size(); i++) {
      tooltip.add(Component.literal((i == sel ? "▶ " : "  ") + (i + 1) + ". " + nameOf(all.get(i))).withStyle(i == sel ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }
    for (int i = all.size(); i < SLOTS; i++) tooltip.add(Component.literal("  " + (i + 1) + ". —").withStyle(ChatFormatting.DARK_GRAY));
    tooltip.add(Component.translatable("tooltip.arcane.scepter.help").withStyle(ChatFormatting.DARK_GRAY));
  }
}
