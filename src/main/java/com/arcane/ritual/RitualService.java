package com.arcane.ritual;

import com.arcane.ambient.AmbientFieldResolver;
import com.arcane.execution.EnvironmentalSnapshot;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.PrecisionSnapshot;
import com.arcane.execution.SpellPipeline;
import com.arcane.inscription.CompiledExecutionPlan;
import com.arcane.inscription.CompiledPlanCache;
import com.arcane.inscription.Inscription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class RitualService {
  private RitualService() {}

  public static void register(ServerLevel level, RitualSite site) {
    RitualIndex.get(level).put(site);
  }

  public static void activate(ServerLevel level, UUID ritualId, LivingEntity activator) {
    RitualIndex.get(level).get(ritualId).ifPresent(site -> {
      if (site.state() != RitualSite.State.DORMANT) return;
      Inscription inscription = site.inscription();
      CompiledExecutionPlan plan = CompiledPlanCache.getOrCompile(inscription);
      EnvironmentalSnapshot environment = AmbientFieldResolver.resolve(level, site.origin());
      ExecutionContext context = new ExecutionContext(level, activator, inscription, plan, environment,
          PrecisionSnapshot.WORKBENCH, ExecutionContext.TriggerEvent.cast());
      context.setGeometryBonus(site.geometryScore());
      context.setScaleFactor(site.scaleFactor());
      context.setRitualSiteId(ritualId);
      SpellPipeline.INSTANCE.execute(context);
      site.setState(RitualSite.State.ARMED);
    });
  }

  public static class RitualIndex extends SavedData {
    private static final String NAME = "arcane_rituals";
    private final Map<UUID, RitualSite> sites = new HashMap<>();

    public static RitualIndex get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(RitualIndex::load, RitualIndex::new, NAME);
    }

    public RitualIndex() {}

    public static RitualIndex load(CompoundTag tag) {
      RitualIndex index = new RitualIndex();
      if (tag.contains("sites", Tag.TAG_LIST)) {
        ListTag list = tag.getList("sites", Tag.TAG_COMPOUND);
        for (Tag element : list) {
          RitualSite site = RitualSite.load((CompoundTag) element);
          index.sites.put(site.id(), site);
        }
      }
      return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      sites.values().forEach(site -> list.add(site.save(new CompoundTag())));
      tag.put("sites", list);
      return tag;
    }

    public void put(RitualSite site) {
      sites.put(site.id(), site);
      setDirty();
    }

    public Optional<RitualSite> get(UUID id) {
      return Optional.ofNullable(sites.get(id));
    }
  }
}
