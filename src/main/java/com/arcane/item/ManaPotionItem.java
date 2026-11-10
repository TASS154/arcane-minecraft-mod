package com.arcane.item;

import com.arcane.registry.ModEffects;
import com.arcane.config.ArcaneConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Pocao de mana: SURGE regenera mais rapido; VESSEL aumenta o maximo. Dura o tempo da config e devolve o frasco. */
public class ManaPotionItem extends Item {
    public enum Kind { SURGE, VESSEL }

    private final Kind kind;

    public ManaPotionItem(Properties properties, Kind kind) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            int ticks = kind == Kind.SURGE ? ArcaneConfig.MANA_SURGE_TICKS.get() : ArcaneConfig.MANA_VESSEL_TICKS.get();
            var effect = kind == Kind.SURGE ? ModEffects.MANA_SURGE.get() : ModEffects.MANA_VESSEL.get();
            entity.addEffect(new MobEffectInstance(effect, ticks, 0, false, false, true));
            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8f, 1.2f);
            if (entity instanceof ServerPlayer sp) com.arcane.execution.ManaBonuses.refresh(sp);
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) return bottle;
        if (entity instanceof Player player && !player.getInventory().add(bottle)) player.drop(bottle, false);
        return stack;
    }
}
