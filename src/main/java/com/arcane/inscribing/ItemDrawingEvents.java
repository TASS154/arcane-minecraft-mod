package com.arcane.inscribing;

import com.arcane.ArcaneMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Gatilhos naturais dos desenhos em objetos: comer, beber, ferir e usar. So reagem a eventos, nunca varrem nada. */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class ItemDrawingEvents {
  private ItemDrawingEvents() {}

  /** Comida e pocao: dispara ao terminar de consumir (o consumidor e quem conjura). */
  @SubscribeEvent
  public static void onConsumed(LivingEntityUseItemEvent.Finish event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    ItemStack stack = event.getItem();
    var kind = ItemDrawings.kindOf(stack);
    if (kind != ItemDrawings.Kind.FOOD && kind != ItemDrawings.Kind.POTION) return;
    ItemDrawings.fire(player, stack);
  }

  /** Arma: dispara quando a arma fere alguem (golpe direto, nao dano de magia). */
  @SubscribeEvent
  public static void onHit(LivingHurtEvent event) {
    if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
    if (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC) || com.arcane.execution.Pierce.isSpell(event.getSource())) return;
    ItemStack held = player.getMainHandItem();
    if (ItemDrawings.kindOf(held) != ItemDrawings.Kind.WEAPON) return;
    ItemDrawings.fire(player, held);
  }

  /** Proxima vez (tick do jogo) em que a armadura de cada jogador pode disparar de novo: 1 s entre disparos. */
  private static final java.util.Map<java.util.UUID, Long> ARMOR_READY = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * Armadura e escudo: disparam quando quem os usa leva dano de outra criatura ou projetil (queda, fome e afogamento
   * nao). Cada peca vestida (e o escudo levantado) dispara os seus desenhos; no maximo uma vez por segundo.
   */
  @SubscribeEvent
  public static void onWearerHurt(LivingHurtEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == null || event.getSource().getEntity() == player) return;
    long now = player.level().getGameTime();
    if (ARMOR_READY.getOrDefault(player.getUUID(), 0L) > now) return;
    int fired = 0;
    for (ItemStack piece : player.getArmorSlots()) if (ItemDrawings.count(piece) > 0) fired += ItemDrawings.fire(player, piece);
    ItemStack used = player.getUseItem();
    if (player.isBlocking() && ItemDrawings.kindOf(used) == ItemDrawings.Kind.ARMOR && ItemDrawings.count(used) > 0) fired += ItemDrawings.fire(player, used);
    if (fired > 0) ARMOR_READY.put(player.getUUID(), now + 20);
  }

  /** Criatura desenhada: dispara os desenhos dela sempre que ataca (o desenho some depois, salvo Persistencia). */
  @SubscribeEvent
  public static void onCreatureAttack(LivingHurtEvent event) {
    if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) || attacker instanceof ServerPlayer
        || !(attacker.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
    var data = attacker.getPersistentData();
    var drawings = ItemDrawings.listIn(data);
    if (drawings.isEmpty()) return;
    ItemDrawings.fireOn(attacker, level, drawings, data);
  }

  /** Po solvente na mao principal e um item na secundaria: apaga os desenhos do item (a tinta permanente pura fica). */
  @SubscribeEvent
  public static void onSolvent(PlayerInteractEvent.RightClickItem event) {
    var player = event.getEntity();
    if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().is(com.arcane.registry.ModItems.SOLVENT_POWDER.get())) return;
    ItemStack target = player.getOffhandItem();
    if (target.isEmpty() || ItemDrawings.count(target) == 0) return;
    event.setCanceled(true);
    if (event.getLevel().isClientSide) return;
    int removed = ItemDrawings.dissolve(target);
    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(removed > 0 ? "message.arcane.solvent.erased" : "message.arcane.solvent.permanent")
        .withStyle(net.minecraft.ChatFormatting.GRAY), true);
    if (removed > 0 && !player.getAbilities().instabuild) event.getItemStack().shrink(1);
  }

  /** Qualquer outro item: dispara ao usar (botao direito). Agachado nao dispara. */
  @SubscribeEvent
  public static void onUse(PlayerInteractEvent.RightClickItem event) {
    if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || event.getEntity().isShiftKeyDown()) return;
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    ItemStack stack = event.getItemStack();
    if (ItemDrawings.kindOf(stack) != ItemDrawings.Kind.OTHER || stack.getItem() instanceof com.arcane.item.InkItem
        || stack.getItem() instanceof com.arcane.item.BlockPenItem || stack.getItem() instanceof com.arcane.item.ObjectPenItem) return;
    if (ItemDrawings.fire(player, stack) > 0) event.setCanceled(true);
  }
}
