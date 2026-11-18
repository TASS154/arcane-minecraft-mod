package com.arcane.client;

import com.arcane.ArcaneMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tremor de camera por "trauma" (0 a 1): impactos, o fim de um Dominio e a carga de uma magia empurram a camera de leve.
 * O deslocamento cresce com o quadrado do trauma (pequeno some quase, grande sacode) e o trauma decai sozinho.
 * Desligado com a opcao "reduzir clarões" (ClientPrefs.reduceFlashes). So visual, nada vai ao servidor.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class CameraShake {
  private static float trauma;
  private static float time;

  private CameraShake() {}

  /** Soma trauma (limitado a 1). {@code distance} em blocos atenua: some a partir de 28. */
  public static void add(float amount, double distance) {
    if (ClientPrefs.reduceFlashes()) return;
    float falloff = (float) Mth.clamp(1.0 - distance / 28.0, 0.0, 1.0);
    trauma = Math.min(1f, trauma + amount * falloff);
  }

  /** Mantem um piso de trauma (carga em andamento): nao acumula, so sustenta. */
  public static void sustain(float level) {
    if (ClientPrefs.reduceFlashes()) return;
    trauma = Math.max(trauma, Math.min(0.35f, level));
  }

  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent e) {
    if (e.phase != TickEvent.Phase.END) return;
    trauma = Math.max(0f, trauma - 0.025f);
    time += 1f;
  }

  @SubscribeEvent
  public static void angles(ViewportEvent.ComputeCameraAngles event) {
    if (trauma <= 0.001f || Minecraft.getInstance().isPaused()) return;
    float t = (time + (float) event.getPartialTick()) * 0.9f;
    float s = trauma * trauma;
    event.setPitch(event.getPitch() + s * 3.2f * Mth.sin(t * 1.7f));
    event.setYaw(event.getYaw() + s * 3.2f * Mth.sin(t * 2.3f + 1f));
    event.setRoll(event.getRoll() + s * 4.5f * Mth.sin(t * 1.3f + 2f));
  }
}
