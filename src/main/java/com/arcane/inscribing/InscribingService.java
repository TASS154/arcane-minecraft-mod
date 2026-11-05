package com.arcane.inscribing;

import com.arcane.inscription.GlyphInstance;
import com.arcane.inscription.Inscription;
import com.arcane.inscription.InscriptionMetadata;
import com.arcane.inscription.InscriptionSourceKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InscribingService {
  private InscribingService() {}

  public static boolean inscribe(ServerLevel level, Player player, BlockPos pos, List<GlyphInstance> glyphs,
                                 ResourceLocation ink, String trigger) {
    if (!PenRegistry.canWriteOnBlock(player, pos, level)) return false;
    Inscription inscription = new Inscription(new ArrayList<>(glyphs), new InscriptionMetadata(
        "", ink, new ResourceLocation("arcane", "arcane_pen"), 1.0,
        Optional.of(player.getUUID()), level.getGameTime(), InscriptionSourceKind.WORLD));
    WorldInscriptionIndex.get(level).put(new InscriptionAnchor(pos, inscription, trigger, ink));
    if (InkRegistry.isBloodInk(ink)) {
      player.hurt(player.damageSources().magic(), 2f);
    }
    level.playSound(null, pos, com.arcane.registry.ModSounds.PEN_STROKE.get(),
        net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
    level.sendParticles(com.arcane.registry.ModParticles.ARCANE_SPARK.get(),
        pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 10, 0.3, 0.02, 0.3, 0.0);
    return true;
  }
}
