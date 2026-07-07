package com.arcane.discovery;

import com.arcane.grimoire.GrimoireAttachments;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ResearchService {
  private static final Map<UUID, Map<String, Float>> PROGRESS = new HashMap<>();

  private ResearchService() {}

  public static void advance(ServerPlayer player, String topicId, float amount) {
    PROGRESS.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
        .merge(topicId, amount, Float::sum);
    float progress = PROGRESS.get(player.getUUID()).getOrDefault(topicId, 0f);
    if (progress >= 1.0f) {
      GrimoireAttachments.get(player).ifPresent(g -> g.logDiscovery("research_complete:" + topicId));
    }
  }
}
