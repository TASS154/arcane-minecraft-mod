package com.arcane.menu;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GrimoireState;
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

  public ArcaneWorkbenchMenu(int id, Inventory inv, FriendlyByteBuf buf) {
    this(id, inv, buf.readBlockPos());
  }

  public ArcaneWorkbenchMenu(int id, Inventory inv, BlockPos pos) {
    super(ModMenuTypes.ARCANE_WORKBENCH.get(), id);
    this.workbenchPos = pos;
    this.access = ContainerLevelAccess.create(inv.player.level(), pos);
  }

  public List<GlyphInstance> building() { return building; }
  public String spellName() { return spellName; }
  public void setSpellName(String name) { this.spellName = name; }

  public void addGlyph(GlyphInstance glyph) {
    building.add(glyph);
  }

  public void clearGlyphs() {
    building.clear();
  }

  public boolean saveToGrimoire(Player player) {
    if (building.isEmpty()) return false;
    Inscription inscription = new Inscription(List.copyOf(building), new InscriptionMetadata(
        spellName,
        new ResourceLocation("arcane", "common"),
        new ResourceLocation("arcane", "arcane_pen"),
        1.0,
        Optional.of(player.getUUID()),
        player.level().getGameTime(),
        InscriptionSourceKind.WORKBENCH
    ));
    return GrimoireAttachments.get(player).map(grimoire -> {
      grimoire.addInscription(inscription);
      grimoire.setActiveInscription(inscription.id());
      return true;
    }).orElse(false);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player player) {
    return stillValid(access, player, com.arcane.registry.ModBlocks.ARCANE_WORKBENCH.get());
  }
}
