package com.arcane.registry;

import com.arcane.ArcaneMod;
import com.arcane.entity.ArcaneProjectileEntity;
import com.arcane.entity.GrimoireEntity;
import com.arcane.entity.NPCMageEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ArcaneMod.MODID);

    public static final RegistryObject<EntityType<GrimoireEntity>> GRIMOIRE = ENTITIES.register("grimoire",
            () -> EntityType.Builder.<GrimoireEntity>of(GrimoireEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f).clientTrackingRange(8).updateInterval(2).build("grimoire"));

    public static final RegistryObject<EntityType<ArcaneProjectileEntity>> ARCANE_PROJECTILE = ENTITIES.register("arcane_projectile",
            () -> EntityType.Builder.<ArcaneProjectileEntity>of(ArcaneProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("arcane_projectile"));

    public static final RegistryObject<EntityType<NPCMageEntity>> NPC_MAGE = ENTITIES.register("npc_mage",
            () -> EntityType.Builder.<NPCMageEntity>of(NPCMageEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("npc_mage"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
