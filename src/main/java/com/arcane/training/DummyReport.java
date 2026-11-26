package com.arcane.training;

import com.arcane.entity.ArcaneDummyEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** {@code /arcane dummy report}: numeros exatos (HP, nao coracoes) do Boneco mais proximo, e as reacoes disparadas. */
public final class DummyReport {
  private DummyReport() {}

  public static ArcaneDummyEntity nearest(ServerLevel level, Vec3 at, double radius) {
    return level.getEntitiesOfClass(ArcaneDummyEntity.class, new AABB(at, at).inflate(radius), e -> e.isAlive())
        .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(at))).orElse(null);
  }

  static String hp(float v) { return String.format(Locale.ROOT, "%.2f", v); }

  public static List<Component> lines(ArcaneDummyEntity d) {
    List<Component> out = new ArrayList<>();
    DamageMeter m = d.meter();
    long now = d.level().getGameTime();
    String profile = d.affinityProfile().isEmpty() ? "neutral" : d.affinityProfile();
    out.add(Component.translatable("message.arcane.dummy.report", hp(d.getHealth()), hp(d.getMaxHealth()), m.count(), hp(m.total()),
        hp(m.lastHit()), hp(m.perSecond(now)), m.lastManaCost() < 0 ? "-" : Integer.toString(m.lastManaCost()),
        ArcaneDummyEntity.ARMOR_NAMES[Math.floorMod(d.armorTier(), ArcaneDummyEntity.ARMOR_NAMES.length)], profile));
    StringBuilder r = new StringBuilder();
    m.reactions().forEach((k, v) -> r.append(r.length() == 0 ? "" : ", ").append(k).append(" x").append(v));
    out.add(Component.translatable("message.arcane.dummy.reactions", r.length() == 0 ? "-" : r.toString()));
    for (DamageMeter.Hit h : m.recent(8)) out.add(Component.translatable("message.arcane.dummy.hit", now - h.tick(), hp(h.hp()), h.source()));
    return out;
  }
}
