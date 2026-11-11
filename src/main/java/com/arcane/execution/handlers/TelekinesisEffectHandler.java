package com.arcane.execution.handlers;

import com.arcane.execution.EffectCall;
import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.Targeting;
import com.arcane.execution.TargetList;
import com.arcane.execution.Telekinesis;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Telecinese: segura as criaturas alvo e/ou o grupo de blocos do bloco mirado (um, esfera ou mesmo tipo conectado),
 * levando-os pairando diante da mira por alguns segundos. Os blocos so se mexem quando as configs permitem alterar o mundo.
 */
public class TelekinesisEffectHandler implements EffectHandler {
  @Override
  public void apply(ExecutionContext context, EffectCall call, TargetList targets) {
    double duration = context.amplify(call.parameters().getOrDefault("duration", 100.0), call.magnitudeKeys(), "duration");
    int ticks = (int) Math.max(20, Math.min(400, duration));
    double distance = Math.max(2.0, Math.min(16.0, call.parameters().getOrDefault("distance", 6.0)));
    int sphere = (int) Math.max(0, Math.min(6, Math.round(call.parameters().getOrDefault("sphere", 0.0))));
    boolean sameType = call.parameters().getOrDefault("same_type", 0.0) >= 0.5;

    List<BlockPos> blocks = new ArrayList<>();
    if (!targets.blocks().isEmpty() && Targeting.mayAlterWorld(context)) {
      BlockPos seed = targets.blocks().get(0).pos();
      blocks = Telekinesis.collect(context.level(), seed, sphere, sameType, Telekinesis.MAX_BLOCKS);
    }
    int count = Telekinesis.grab(context, new ArrayList<>(targets.entities()), blocks, distance, ticks);
    if (context.caster() instanceof ServerPlayer player) {
      player.displayClientMessage(Component.translatable(count > 0 ? "message.arcane.telekinesis.holding" : "message.arcane.telekinesis.nothing", count), true);
    }
  }
}
