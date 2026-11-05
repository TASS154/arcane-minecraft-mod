package com.arcane.registry;

import com.arcane.ArcaneMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sons do mod. Os arquivos ficam em assets/arcane/sounds e sao declarados em sounds.json. */
public final class ModSounds {
  public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ArcaneMod.MODID);

  public static final RegistryObject<SoundEvent> SPELL_CAST = register("spell_cast");
  public static final RegistryObject<SoundEvent> SPELL_HIT = register("spell_hit");
  public static final RegistryObject<SoundEvent> WORKBENCH_SAVE = register("workbench_save");
  public static final RegistryObject<SoundEvent> GLYPH_LEARNED = register("glyph_learned");
  public static final RegistryObject<SoundEvent> PEN_STROKE = register("pen_stroke");
  public static final RegistryObject<SoundEvent> RITUAL_ACTIVATE = register("ritual_activate");
  public static final RegistryObject<SoundEvent> MANA_POISON = register("mana_poison");
  public static final RegistryObject<SoundEvent> GRIMOIRE_SUMMON = register("grimoire_summon");

  private ModSounds() {}

  private static RegistryObject<SoundEvent> register(String name) {
    return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArcaneMod.MODID, name)));
  }

  public static void register(IEventBus bus) {
    SOUNDS.register(bus);
  }
}
