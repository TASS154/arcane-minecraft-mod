package com.arcane.inscribing;

import com.arcane.inscription.Inscription;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record InscriptionAnchor(BlockPos pos, Inscription inscription, String triggerType, ResourceLocation ink) {
}
