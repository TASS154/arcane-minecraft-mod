package com.arcane.pocket;

import com.arcane.config.ArcaneConfig;
import com.arcane.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bolso Arcano: armazenamento por jogador, como um Bau do Fim (so o dono abre o seu), que permanece na morte.
 * O conteudo vive em {@code PlayerPersisted.arcane_pocket} (o Forge preserva essa sub-etiqueta na morte e
 * {@code PocketEvents} copia de novo no clone por garantia). Todo acesso e feito no servidor e cada alteracao grava na hora.
 */
public final class ArcanePocket {
  public static final String KEY = "arcane_pocket";
  public static final int MENU_SLOTS = 18;
  private static final int COMBAT_TICKS = 60;
  private static final Map<UUID, PocketContainer> LIVE = new HashMap<>();

  private ArcanePocket() {}

  public static int slots() { return Math.min(MENU_SLOTS, ArcaneConfig.POCKET_SLOTS.get()); }

  static CompoundTag persisted(Player player) {
    CompoundTag data = player.getPersistentData();
    if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
    return data.getCompound(Player.PERSISTED_NBT_TAG);
  }

  /** Itens que nao podem entrar: shulkers, o grimorio, paginas (guardam inscricoes) e a lista da config. */
  public static boolean blocked(ItemStack stack) {
    if (stack.isEmpty()) return false;
    if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) return true;
    if (stack.is(ModItems.GRIMOIRE.get()) || stack.is(ModItems.ARCANE_PAGE.get())) return true;
    var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
    return key != null && ArcaneConfig.POCKET_BLOCKED_ITEMS.get().contains(key.toString());
  }

  public static boolean inCombat(Player player) {
    int now = player.tickCount;
    return (player.getLastHurtByMob() != null && now - player.getLastHurtByMobTimestamp() < COMBAT_TICKS)
        || (player.getLastHurtMob() != null && now - player.getLastHurtMobTimestamp() < COMBAT_TICKS);
  }

  /** O contêiner vivo do jogador (um so por jogador, para dono e admin enxergarem o mesmo). */
  public static PocketContainer container(ServerPlayer owner) {
    PocketContainer c = LIVE.get(owner.getUUID());
    if (c == null || c.owner != owner) {
      c = new PocketContainer(owner);
      c.load(persisted(owner).getList(KEY, Tag.TAG_COMPOUND));
      LIVE.put(owner.getUUID(), c);
    }
    return c;
  }

  public static void forget(UUID id) { LIVE.remove(id); }
  public static void clearCache() { LIVE.clear(); }

  /** Abre o bolso do proprio jogador, respeitando a config e o bloqueio em combate. Devolve se abriu. */
  public static boolean open(ServerPlayer player) {
    if (!ArcaneConfig.POCKET_ENABLED.get()) { player.displayClientMessage(Component.translatable("message.arcane.pocket.disabled"), true); return false; }
    if (inCombat(player)) { player.displayClientMessage(Component.translatable("message.arcane.pocket.combat"), true); return false; }
    openFor(player, player);
    return true;
  }

  /** Abre o bolso de {@code owner} para {@code viewer} (admin). */
  public static void openFor(ServerPlayer viewer, ServerPlayer owner) {
    PocketContainer c = container(owner);
    viewer.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x2, id, inv, c, 2),
        Component.translatable(viewer == owner ? "container.arcane.pocket" : "container.arcane.pocket.other", owner.getDisplayName())));
  }

  /** Esvazia o bolso e devolve quantos itens havia. */
  public static int clear(ServerPlayer owner) {
    PocketContainer c = container(owner);
    int n = 0;
    for (int i = 0; i < c.getContainerSize(); i++) n += c.getItem(i).getCount();
    c.clearContent();
    c.setChanged();
    return n;
  }

  public static final class PocketContainer extends SimpleContainer {
    private final ServerPlayer owner;
    private boolean loading;

    PocketContainer(ServerPlayer owner) {
      super(MENU_SLOTS);
      this.owner = owner;
    }

    /** Le as pilhas mantendo o espaco de cada uma ({@code fromTag} do vanilla as compacta e perde a posicao). */
    void load(ListTag tag) {
      loading = true;
      try {
        for (int i = 0; i < getContainerSize(); i++) setItem(i, ItemStack.EMPTY);
        for (int i = 0; i < tag.size(); i++) {
          CompoundTag entry = tag.getCompound(i);
          int slot = entry.getByte("Slot") & 255;
          if (slot < getContainerSize()) setItem(slot, ItemStack.of(entry));
        }
      } finally { loading = false; }
    }

    ListTag save() {
      ListTag out = new ListTag();
      for (int i = 0; i < getContainerSize(); i++) {
        ItemStack st = getItem(i);
        if (st.isEmpty()) continue;
        CompoundTag entry = new CompoundTag();
        entry.putByte("Slot", (byte) i);
        st.save(entry);
        out.add(entry);
      }
      return out;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
      return slot < slots() && !blocked(stack);
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void setChanged() {
      super.setChanged();
      if (!loading) persisted(owner).put(KEY, save());
    }
  }
}
