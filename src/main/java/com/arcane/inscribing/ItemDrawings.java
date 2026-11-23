package com.arcane.inscribing;

import com.arcane.config.ArcaneConfig;
import com.arcane.execution.CastingService;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.execution.Traces;
import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Desenhos em objetos (comida, armas, itens, pocoes). Cada item guarda uma lista de desenhos (cada um com a sua tinta
 * e a data em que foi feito, como nos blocos) e cada desenho dispara no uso natural do objeto:
 * comida e pocao ao consumir, arma ao ferir, qualquer outro item ao usar (botao direito).
 * Um item pequeno comporta 1 desenho, um grande (arma, ferramenta, armadura) comporta 2. Alem disso ele fica instavel:
 * cada desenho acima da capacidade deixa o uso imprevisivel e pode quebrar o objeto.
 */
public final class ItemDrawings {
  private static final String TAG = "arcane_drawings";
  /** Evita que a magia disparada por um desenho dispare outros desenhos em cadeia infinita. */
  private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

  public enum Kind { FOOD, WEAPON, POTION, OTHER }

  private ItemDrawings() {}

  public static Kind kindOf(ItemStack stack) {
    var item = stack.getItem();
    if (item instanceof PotionItem) return Kind.POTION;
    if (item instanceof SwordItem || item instanceof AxeItem || item instanceof DiggerItem || item instanceof TridentItem
        || item instanceof BowItem || item instanceof CrossbowItem) return Kind.WEAPON;
    if (stack.isEdible()) return Kind.FOOD;
    return Kind.OTHER;
  }

  /** Quantos desenhos o objeto comporta sem ficar instavel. */
  public static int capacity(ItemStack stack) {
    var item = stack.getItem();
    boolean large = item instanceof SwordItem || item instanceof DiggerItem || item instanceof TridentItem || item instanceof BowItem
        || item instanceof CrossbowItem || item instanceof ArmorItem || item instanceof ShieldItem;
    return large ? 2 : 1;
  }

  public static List<Inscription> list(ItemStack stack) {
    if (stack.isEmpty() || !stack.hasTag()) return new ArrayList<>();
    return listIn(stack.getTag());
  }

  /** Le a lista de desenhos de qualquer nbt (de um item ou de uma criatura). */
  public static List<Inscription> listIn(CompoundTag holder) {
    List<Inscription> out = new ArrayList<>();
    if (holder == null || !holder.contains(TAG, Tag.TAG_LIST)) return out;
    for (Tag t : holder.getList(TAG, Tag.TAG_COMPOUND)) {
      Inscription.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(out::add);
    }
    return out;
  }

  public static void storeIn(CompoundTag holder, List<Inscription> all) {
    if (all.isEmpty()) {
      holder.remove(TAG);
      return;
    }
    ListTag list = new ListTag();
    for (Inscription i : all) Inscription.CODEC.encodeStart(NbtOps.INSTANCE, i).result().ifPresent(list::add);
    holder.put(TAG, list);
  }

  private static void store(ItemStack stack, List<Inscription> all) {
    if (all.isEmpty()) {
      if (stack.hasTag()) stack.getTag().remove(TAG);
      return;
    }
    storeIn(stack.getOrCreateTag(), all);
  }

  public static int count(ItemStack stack) {
    return list(stack).size();
  }

  /** Quantos desenhos a mais ele aceita acima da capacidade (config); 0 = nao aceita passar do limite. */
  public static int maxCount(ItemStack stack) {
    return capacity(stack) + ArcaneConfig.ITEM_EXTRA_DRAWINGS.get();
  }

  public static int excess(ItemStack stack) {
    return Math.max(0, count(stack) - capacity(stack));
  }

  public static boolean canAdd(ItemStack stack) {
    return count(stack) < maxCount(stack);
  }

  public static void add(ItemStack stack, Inscription drawing) {
    List<Inscription> all = list(stack);
    all.add(drawing);
    store(stack, all);
  }

  /** Po solvente: apaga todos os desenhos do objeto, menos os de tinta permanente pura. Devolve quantos apagou. */
  public static int dissolve(ItemStack stack) {
    List<Inscription> all = list(stack);
    int before = all.size();
    all.removeIf(i -> InkRegistry.isErasable(i.metadata().ink()));
    store(stack, all);
    return before - all.size();
  }

  /** A tinta de cada desenho tambem se desgasta com os dias; apaga os vencidos. */
  private static List<Inscription> fresh(ItemStack stack, long now) {
    List<Inscription> all = list(stack);
    if (all.removeIf(i -> InkRegistry.expired(i.metadata().ink(), i.metadata().created(), now))) store(stack, all);
    return all;
  }

  /**
   * Dispara os desenhos do objeto em nome de {@code player}. Devolve quantos dispararam.
   * Cada desenho se desfaz depois de disparar, salvo os com Persistencia. Acima da capacidade o objeto fica instavel:
   * a potencia varia e cada uso pode quebra-lo.
   */
  public static int fire(ServerPlayer player, ItemStack stack) {
    if (DEPTH.get() > 0 || stack.isEmpty()) return 0;
    ServerLevel level = player.serverLevel();
    List<Inscription> drawings = fresh(stack, level.getGameTime());
    if (drawings.isEmpty()) return 0;
    int excess = Math.max(0, drawings.size() - capacity(stack));
    int fired = 0;
    DEPTH.set(DEPTH.get() + 1);
    try {
      List<Inscription> keep = new ArrayList<>();
      for (Inscription d : drawings) {
        double power = 1.0;
        if (excess > 0) power = 0.4 + level.random.nextDouble() * 1.2; // imprevisivel: de 40% a 160%
        CastingService.tryCast(player, d, new PrecisionSnapshot(power * PrecisionSnapshot.of(d).powerMultiplier(), 1.0, false));
        fired++;
        if (Traces.has(d, "persistence")) keep.add(d);
      }
      store(stack, keep);
    } finally {
      DEPTH.set(DEPTH.get() - 1);
    }
    if (excess > 0 && level.random.nextDouble() < Math.min(0.9, excess * 0.25)) {
      level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
      player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.arcane.drawing.broke").withStyle(net.minecraft.ChatFormatting.RED), true);
      stack.shrink(1);
    }
    return fired;
  }

  /** Dispara os desenhos de uma criatura (guardados no nbt dela): tinta vencida some, Persistencia fica, o resto some ao disparar. */
  public static int fireOn(net.minecraft.world.entity.LivingEntity creature, ServerLevel level, List<Inscription> drawings, CompoundTag holder) {
    if (DEPTH.get() > 0) return 0;
    drawings.removeIf(i -> InkRegistry.expired(i.metadata().ink(), i.metadata().created(), level.getGameTime()));
    int fired = 0;
    DEPTH.set(DEPTH.get() + 1);
    try {
      List<Inscription> keep = new ArrayList<>();
      for (Inscription d : drawings) {
        CastingService.tryCast(creature, d, level);
        fired++;
        if (Traces.has(d, "persistence")) keep.add(d);
      }
      storeIn(holder, keep);
    } finally {
      DEPTH.set(DEPTH.get() - 1);
    }
    return fired;
  }
}
