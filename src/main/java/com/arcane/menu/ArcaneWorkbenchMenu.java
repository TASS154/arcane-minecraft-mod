package com.arcane.menu;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import com.arcane.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArcaneWorkbenchMenu extends AbstractContainerMenu {
  private final BlockPos workbenchPos;
  private final ContainerLevelAccess access;
  private final List<GlyphInstance> building = new ArrayList<>();
  private String spellName = "";

  /** Modos: 0 bancada (100%), 1 caneta num bloco, 2 escrever no campo (grimorio, 30%), 3 caneta num item da mao secundaria. */
  public static final int BENCH = 0, BLOCK = 1, FIELD = 2, ITEM = 3;
  /** Escreve numa criatura: a posicao alvo guarda o id da entidade em x. */
  public static final int CREATURE = 4;
  private final int mode;
  private net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;

  public ArcaneWorkbenchMenu(int id, Inventory inv, FriendlyByteBuf buf) {
    this(id, inv, buf.readBlockPos(), buf.readVarInt());
    this.face = net.minecraft.core.Direction.from3DDataValue(buf.readVarInt());
  }

  public ArcaneWorkbenchMenu(int id, Inventory inv, BlockPos pos) {
    this(id, inv, pos, BENCH);
  }

  public ArcaneWorkbenchMenu(int id, Inventory inv, BlockPos pos, int mode) {
    super(ModMenuTypes.ARCANE_WORKBENCH.get(), id);
    this.mode = mode;
    this.workbenchPos = pos;
    this.access = ContainerLevelAccess.create(inv.player.level(), pos);
  }

  public int mode() { return mode; }
  public net.minecraft.core.Direction face() { return face; }
  public ArcaneWorkbenchMenu withFace(net.minecraft.core.Direction face) { this.face = face; return this; }
  /** Escreve fora do grimorio (num bloco ou num item) e por isso exige escolher uma tinta do inventario. */
  public boolean penMode() { return mode == BLOCK || mode == ITEM || mode == CREATURE; }
  public boolean needsInk() { return penMode(); }
  public BlockPos targetPos() { return workbenchPos; }
  public List<GlyphInstance> building() { return building; }
  public String spellName() { return spellName; }
  public void setSpellName(String name) { this.spellName = name; }

  public void addGlyph(GlyphInstance glyph) {
    building.add(glyph);
  }

  public void clearGlyphs() {
    building.clear();
  }

  /**
   * Grava uma magia NOVA no grimorio e a torna ativa (editar nunca altera a anterior). Na bancada a potencia
   * e 100%; escrevendo no campo, 30%. Nome repetido ganha um numero.
   */
  public boolean saveToGrimoire(Player player) {
    if (building.isEmpty()) return false;
    boolean field = mode == FIELD;
    return GrimoireAttachments.get(player).map(grimoire -> {
      String name = uniqueName(grimoire, spellName);
      Inscription inscription = new Inscription(List.copyOf(building), new InscriptionMetadata(
          name,
          new ResourceLocation("arcane", "common"),
          new ResourceLocation("arcane", "arcane_pen"),
          field ? com.arcane.execution.PrecisionSnapshot.FIELD_POWER : 1.0,
          Optional.of(player.getUUID()),
          player.level().getGameTime(),
          field ? InscriptionSourceKind.SCROLL : InscriptionSourceKind.WORKBENCH
      ));
      grimoire.addInscription(inscription);
      grimoire.setActiveInscription(inscription.id());
      this.spellName = name;
      return true;
    }).orElse(false);
  }

  static String uniqueName(com.arcane.grimoire.GrimoireState grimoire, String wanted) {
    if (wanted == null || wanted.isBlank()) return wanted == null ? "" : wanted;
    String base = wanted.trim();
    String candidate = base;
    for (int n = 2; grimoire.findByName(candidate).isPresent() && n < 100; n++) candidate = base + " " + n;
    return candidate.length() > 32 ? candidate.substring(0, 32) : candidate;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player player) {
    if (mode == FIELD) return true;
    if (penMode()) {
      var pen = player.getMainHandItem();
      boolean anyPen = pen.is(com.arcane.registry.ModItems.ARCANE_PEN.get()) || pen.getItem() instanceof com.arcane.item.BlockPenItem
          || pen.getItem() instanceof com.arcane.item.ObjectPenItem || pen.getItem() instanceof com.arcane.item.AirPenItem
          || pen.getItem() instanceof com.arcane.item.CreaturePenItem;
      if (!anyPen) return false;
      if (mode == CREATURE) {
        var target = player.level().getEntity(workbenchPos.getX());
        return target != null && target.isAlive() && player.distanceToSqr(target) < 144.0;
      }
      return (mode == ITEM ? !player.getOffhandItem().isEmpty() : true)
          && player.distanceToSqr(workbenchPos.getX() + 0.5, workbenchPos.getY() + 0.5, workbenchPos.getZ() + 0.5) < 100.0;
    }
    return stillValid(access, player, com.arcane.registry.ModBlocks.ARCANE_WORKBENCH.get());
  }
}
