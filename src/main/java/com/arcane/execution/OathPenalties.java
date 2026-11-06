package com.arcane.execution;

import com.arcane.ArcaneMod;
import com.arcane.inscription.VowFormula;
import com.arcane.grimoire.GrimoireAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Penalidades dos juramentos personalizados: o jogador escolhe o TIPO e a DURACAO e, quanto mais caro for o castigo,
 * maior o bonus (ver {@link VowFormula#oathBonus}). A penalidade comeca depois da magia e fica salva no jogador
 * (sobrevive a relogar; morrer a encerra). Tipos:
 * 1 Imobilidade, 2 Silencio (nenhuma outra magia), 3 Mana selada (mana zerada e sem regenerar),
 * 4 Fragilidade (+50% de dano recebido), 5 Cegueira.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID)
public final class OathPenalties {
  public static final int IMMOBILE = 1, SILENCE = 2, SEALED_MANA = 3, FRAGILE = 4, BLIND = 5;
  public static final double FRAGILE_FACTOR = 1.5;
  private static final String ROOT = "arcane_oaths";

  private OathPenalties() {}

  private static CompoundTag store(Player p, boolean create) {
    CompoundTag data = p.getPersistentData();
    CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
    if (!persisted.contains(ROOT)) {
      if (!create) return new CompoundTag();
      persisted.put(ROOT, new CompoundTag());
      data.put(Player.PERSISTED_NBT_TAG, persisted);
    }
    return persisted.getCompound(ROOT);
  }

  /** Inicia (ou prolonga ate o maior prazo) a penalidade de um tipo. */
  public static void apply(ServerPlayer player, int kind, double seconds) {
    long until = player.level().getGameTime() + Math.round(seconds * 20.0);
    CompoundTag tag = store(player, true);
    String key = Integer.toString(kind);
    tag.putLong(key, Math.max(tag.getLong(key), until));
    player.displayClientMessage(Component.translatable("message.arcane.oath.applied",
        Component.translatable("oath.arcane.kind." + kind), Math.round(seconds)).withStyle(ChatFormatting.DARK_PURPLE), true);
  }

  public static boolean active(Player player, int kind) {
    CompoundTag tag = store(player, false);
    return tag.getLong(Integer.toString(kind)) > player.level().getGameTime();
  }

  public static int remainingSeconds(Player player, int kind) {
    long left = store(player, false).getLong(Integer.toString(kind)) - player.level().getGameTime();
    return (int) Math.max(0, (left + 19) / 20);
  }

  public static void clear(Player player) {
    player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove(ROOT);
  }

  /** Reaplica os efeitos de quem esta sob penalidade (chamado todo tick; barato quando nao ha nada ativo). */
  public static void enforce(ServerPlayer player) {
    CompoundTag tag = store(player, false);
    if (tag.isEmpty()) return;
    long now = player.level().getGameTime();
    if (tag.getLong("1") > now) {
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 6, false, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.JUMP, 6, 128, false, false, false));
    }
    if (tag.getLong("5") > now) {
      player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
    }
    if (tag.getLong("3") > now) {
      GrimoireAttachments.getMana(player).ifPresent(ManaState::drain);
    }
    for (String key : tag.getAllKeys()) {
      long until = tag.getLong(key);
      if (until != 0 && until <= now) {
        tag.putLong(key, 0L);
        player.displayClientMessage(Component.translatable("message.arcane.oath.ended",
            Component.translatable("oath.arcane.kind." + key)).withStyle(ChatFormatting.GRAY), true);
      }
    }
  }

  @SubscribeEvent
  public static void onTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
    enforce(sp);
  }

  @SubscribeEvent
  public static void onHurt(LivingHurtEvent event) {
    if (event.getEntity() instanceof Player p && !p.level().isClientSide && active(p, FRAGILE)) {
      event.setAmount((float) (event.getAmount() * FRAGILE_FACTOR));
    }
  }

  @SubscribeEvent
  public static void onDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer p) clear(p);
  }
}
