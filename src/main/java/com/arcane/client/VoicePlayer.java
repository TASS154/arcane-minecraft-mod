package com.arcane.client;

import com.arcane.ArcaneMod;
import com.arcane.fx.SpellVoice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Toca a voz da magia no cliente (Fase L): uma fila por tick (nada de Thread.sleep). Cada nota e a amostra sustentada
 * da familia do elemento, no tom da nota. Opcao de desligar e volume proprio em {@link ClientPrefs}. NAO ouvido.
 */
@Mod.EventBusSubscriber(modid = ArcaneMod.MODID, value = Dist.CLIENT)
public final class VoicePlayer {
  private record Pending(long at, String family, SpellVoice.Note note, double x, double y, double z) {}

  private static final List<Pending> QUEUE = new ArrayList<>();
  private static long tick;

  private VoicePlayer() {}

  /** Agenda a melodia na posicao dada (a partir do proximo tick). */
  public static void play(String family, List<SpellVoice.Note> notes, double x, double y, double z) {
    if (!ClientPrefs.voiceEnabled() || notes.isEmpty()) return;
    synchronized (QUEUE) {
      for (var n : notes) QUEUE.add(new Pending(tick + 1 + n.delay(), family, n, x, y, z));
    }
  }

  /** Uma nota so (pena desenhando o glifo, passar o mouse no grimorio). */
  public static void note(String family, SpellVoice.Note note) {
    var p = Minecraft.getInstance().player;
    if (p == null || !ClientPrefs.voiceEnabled()) return;
    playNow(family, note, p.getX(), p.getY(), p.getZ(), 0.6f);
  }

  static int queued() { synchronized (QUEUE) { return QUEUE.size(); } }

  @SubscribeEvent
  public static void onTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    tick++;
    List<Pending> due = new ArrayList<>();
    synchronized (QUEUE) {
      if (QUEUE.isEmpty()) return;
      for (Iterator<Pending> it = QUEUE.iterator(); it.hasNext(); ) {
        Pending p = it.next();
        if (p.at() <= tick) { due.add(p); it.remove(); }
      }
      if (QUEUE.size() > 64) QUEUE.subList(0, QUEUE.size() - 64).clear(); // nunca acumula
    }
    for (Pending p : due) playNow(p.family(), p.note(), p.x(), p.y(), p.z(), 1f);
  }

  private static void playNow(String family, SpellVoice.Note note, double x, double y, double z, float scale) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    String id = "voice." + family + (note.longTail() ? ".long" : "");
    SoundEvent ev = SoundEvent.createVariableRangeEvent(new ResourceLocation(ArcaneMod.MODID, id));
    float vol = note.volume() * scale * (float) ClientPrefs.voiceVolume();
    if (vol <= 0.01f) return;
    mc.getSoundManager().play(new SimpleSoundInstance(ev, SoundSource.PLAYERS, vol, note.pitch(), RandomSource.create(), x, y, z));
  }
}
