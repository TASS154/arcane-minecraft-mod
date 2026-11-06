package com.arcane.block;

import com.arcane.grimoire.GrimoireAttachments;
import com.arcane.item.ArcaneCompassItem;
import com.arcane.registry.ModItems;
import com.arcane.ritual.ComponentSource;
import com.arcane.ritual.RitualMessages;
import com.arcane.ritual.RitualService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Altar Arcano: o centro do ritual.
 *  - Botao direito: conjura a magia ATIVA do grimorio com o poder do circulo desenhado em volta.
 *  - Shift + botao direito: armazena a magia no altar; qualquer magia com o glifo Ativar a dispara (ate 48 blocos).
 * O Compasso Arcano e o Giz Arcano tem uso proprio (o altar nao interfere).
 */
public class ArcaneAltarBlock extends Block implements EntityBlock {
  private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 13, 15);

  public ArcaneAltarBlock(Properties properties) {
    super(properties);
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new AltarBlockEntity(pos, state);
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    ItemStack held = player.getItemInHand(hand);
    if (held.getItem() instanceof ArcaneCompassItem || held.is(ModItems.ARCANE_CHALK.get())) return InteractionResult.PASS;
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

    var spell = GrimoireAttachments.get(sp).flatMap(g -> g.activeInscriptionId().flatMap(g::getInscription));
    if (spell.isEmpty()) {
      RitualMessages.send(sp, new RitualService.Result(RitualService.Outcome.NO_SPELL, null, 0, "", 0, null));
      return InteractionResult.CONSUME;
    }
    ComponentSource source = ComponentSource.of(sp);
    RitualService.Result result = sp.isShiftKeyDown()
        ? RitualService.store(serverLevel, pos, sp, spell.get(), source, true)
        : RitualService.perform(serverLevel, pos, sp, spell.get(), source, true);
    RitualMessages.send(sp, result);
    return InteractionResult.CONSUME;
  }
}
