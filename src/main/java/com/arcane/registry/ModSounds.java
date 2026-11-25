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
  public static final RegistryObject<SoundEvent> CHARGE_START = register("charge_start");
  public static final RegistryObject<SoundEvent> CHARGE_RELEASE = register("charge_release");
  public static final RegistryObject<SoundEvent> DOMAIN_OPEN = register("domain_open");
  public static final RegistryObject<SoundEvent> DOMAIN_BREAK = register("domain_break");
  public static final RegistryObject<SoundEvent> FLIGHT_BOOST = register("flight_boost");
  public static final RegistryObject<SoundEvent> GOLEM_SLAM = register("golem_slam");
  public static final RegistryObject<SoundEvent> GOLEM_HUM = register("golem_hum");

  /** Sons por elemento (conjuracao e impacto, 3 variacoes cada) e das reacoes; gerados por tools/gen_sounds.py --elements. */
  public static final java.util.Map<String, RegistryObject<SoundEvent>> ELEMENT = new java.util.LinkedHashMap<>();

  static {
    for (com.arcane.execution.SpellElement e : com.arcane.execution.SpellElement.values()) {
      String id = e.name().toLowerCase(java.util.Locale.ROOT);
      ELEMENT.put("element." + id + ".cast", register("element." + id + ".cast"));
      ELEMENT.put("element." + id + ".impact", register("element." + id + ".impact"));
    }
    ELEMENT.put("reaction.steam", register("reaction.steam"));
    ELEMENT.put("reaction.shatter", register("reaction.shatter"));
  }

  private ModSounds() {}

  private static RegistryObject<SoundEvent> register(String name) {
    return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArcaneMod.MODID, name)));
  }

  public static void register(IEventBus bus) {
    SOUNDS.register(bus);
  }
}
