package com.arcane.item;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.grimoire.GrimoireState;
import com.arcane.grimoire.GrimoireTransfer;
import com.arcane.network.ArcaneSync;
import com.arcane.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Item do Grimorio. Botao direito abre a tela (mesma da tecla G).
 *
 * O que cai quando o dono morre leva o CONTEUDO dele (magias, conhecimento, diario) e o UUID do dono:
 *  - o dono o recupera pegando-o do chao (ou com Shift + botao direito): tudo volta;
 *  - outro jogador e so PORTADOR: botao direito conjura as magias dele com 60% (Shift troca de magia) e, pousado numa
 *    bancada, ele estuda um glifo desconhecido por dia de jogo (ver GrimoireDesk).
 */
public class GrimoireItem extends Item {
  public static final String OWNER = "owner";
  public static final String OWNER_NAME = "ownerName";
  public static final String CONTENTS = "contents";

  public GrimoireItem(Properties properties) {
    super(properties);
  }

  public static ItemStack createFor(Player owner) {
    return createFor(owner, null);
  }

  public static ItemStack createFor(Player owner, @Nullable CompoundTag contents) {
    ItemStack stack = new ItemStack(ModItems.GRIMOIRE.get());
    stack.getOrCreateTag().putUUID(OWNER, owner.getUUID());
    stack.getOrCreateTag().putString(OWNER_NAME, owner.getGameProfile().getName());
    if (contents != null) stack.getOrCreateTag().put(CONTENTS, contents);
    return stack;
  }

  public static boolean isOwnedBy(ItemStack stack, UUID player) {
    return stack.is(ModItems.GRIMOIRE.get()) && stack.hasTag() && stack.getTag().hasUUID(OWNER)
        && stack.getTag().getUUID(OWNER).equals(player);
  }

  @Nullable
  public static CompoundTag contents(ItemStack stack) {
    return stack.is(ModItems.GRIMOIRE.get()) && stack.hasTag() && stack.getTag().contains(CONTENTS)
        ? stack.getTag().getCompound(CONTENTS) : null;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (contents(stack) != null && !isOwnedBy(stack, player.getUUID())) {
      // Grimorio alheio: o portador conjura com 60% (agachado troca de magia). Estudar: pousa-lo numa bancada.
      if (!level.isClientSide && player instanceof ServerPlayer sp) com.arcane.grimoire.GrimoireDesk.useForeign(sp, stack, player.isShiftKeyDown());
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    boolean handlesContents = player.isShiftKeyDown() && contents(stack) != null;
    boolean toggles = player.isShiftKeyDown() && contents(stack) == null;
    if (toggles) {
      if (!level.isClientSide && player instanceof ServerPlayer sp) com.arcane.grimoire.GrimoireLifecycleHandler.toggleFloating(sp);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    if (level.isClientSide) {
      if (!handlesContents) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.arcane.client.ClientHooks::openGrimoire);
      }
    } else if (handlesContents && player instanceof ServerPlayer serverPlayer) {
      useContents(serverPlayer, stack);
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  private static void useContents(ServerPlayer player, ItemStack stack) {
    CompoundTag contents = contents(stack);
    if (contents == null) return;
    var grimoire = GrimoireAttachments.get(player);
    if (grimoire.isEmpty()) return;
    if (isOwnedBy(stack, player.getUUID())) {
      GrimoireTransfer.restore(grimoire.get(), contents);
      grimoire.get().setGrimoirePresent(true);
      grimoire.get().setGrimoireDestroyedUntilTick(0L);
      grimoire.get().setVisualStage(GrimoireState.stageFor(grimoire.get().pages()));
      stack.shrink(1);
      // O livro flutuante volta junto com o conteudo (antes so aparecia depois de relogar).
      com.arcane.grimoire.GrimoireLifecycleHandler.spawnGrimoire(player);
      player.displayClientMessage(Component.translatable("message.arcane.grimoire.restored").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    } else {
      GrimoireState source = new GrimoireState();
      source.load(contents);
      GrimoireTransfer.StudyResult r = GrimoireTransfer.study(grimoire.get(), source);
      player.displayClientMessage(Component.translatable("message.arcane.grimoire.studied", r.added(), r.suspected(),
          r.skippedNoPages()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }
    GrimoireAttachments.updateManaMultiplier(player);
    ArcaneSync.sync(player, true);
  }
}
