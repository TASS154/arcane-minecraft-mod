package com.arcane.inscribing;

import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Uma inscricao escrita num bloco; `face` e a face em que o desenho foi feito (aparece desenhado ali). */
public record InscriptionAnchor(BlockPos pos, Inscription inscription, String triggerType, ResourceLocation ink, Direction face) {
  public InscriptionAnchor(BlockPos pos, Inscription inscription, String triggerType, ResourceLocation ink) {
    this(pos, inscription, triggerType, ink, Direction.UP);
  }
}
