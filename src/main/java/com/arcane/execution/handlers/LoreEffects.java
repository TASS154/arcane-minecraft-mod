package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.CreatureLevels;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Timed;
import com.arcane.execution.WorldMarks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Glifos do ciclo do grimorio e do combate (rodada de 23 nov): selos, residuos, posse, portal, contramagia, trancas. */
public final class LoreEffects {
  private LoreEffects() {}

  private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.2f), 1.3f);

  // ------------------------------------------------------------------ Selamento de criatura
  /**
   * Selamento: correntes douradas prendem a criatura por 2 s e depois ela e banida para dentro de um item (Criatura
   * Selada), que solta a criatura de novo ao usar num bloco. Respeita o nivel da criatura (com mana pouca as correntes
   * se partem); chefes resistem; jogadores so ficam presos pelas correntes (nao viram item).
   */
  public static class SealCreature implements EffectHandler {
    public static final int CHAIN_TICKS = 40;
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (Entity e : t.entities()) {
        if (!(e instanceof LivingEntity l) || l == c.caster() || !l.isAlive()) continue;
        boolean full = CreatureLevels.fraction(c, l) >= 1.0;
        l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHAIN_TICKS + 10, 6, false, false), c.caster());
        if (l instanceof Mob mob) mob.setNoAi(true);
        ServerLevel level = c.level();
        level.playSound(null, l.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.5f, 0.6f);
        Timed.start(level, CHAIN_TICKS, now -> chains(level, l, now), () -> {
          if (!l.isAlive()) return;
          if (l instanceof Mob mob) mob.setNoAi(false);
          if (!full || l instanceof Player) {
            level.playSound(null, l.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);
            if (!(l instanceof Player)) LifeEffects.say(c.caster(), "message.arcane.seal_creature.broke", l.getDisplayName());
            return;
          }
          banish(level, l);
        });
      }
    }

    private static void chains(ServerLevel level, LivingEntity l, long now) {
      if (now % 2 != 0) return;
      for (int i = 0; i < 4; i++) {
        double a = i * Math.PI / 2 + now * 0.05, r = l.getBbWidth() * 0.9 + 0.6;
        Vec3 ground = new Vec3(l.getX() + Math.cos(a) * r, l.getY(), l.getZ() + Math.sin(a) * r);
        Vec3 body = l.position().add(0, l.getBbHeight() * 0.6, 0);
        for (int k = 0; k <= 6; k++) {
          Vec3 p = ground.lerp(body, k / 6.0);
          level.sendParticles(GOLD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
      }
    }

    /** Guarda a criatura inteira (com nome, vida e equipamento) num item no chao e a tira do mundo. */
    public static ItemStack banish(ServerLevel level, LivingEntity l) {
      CompoundTag data = new CompoundTag();
      if (!l.save(data)) return ItemStack.EMPTY;
      ItemStack item = new ItemStack(com.arcane.registry.ModItems.SEALED_CREATURE.get());
      item.getOrCreateTag().put("entity", data);
      item.setHoverName(Component.translatable("item.arcane.sealed_creature.named", l.getDisplayName()).withStyle(ChatFormatting.GOLD));
      ItemEntity drop = new ItemEntity(level, l.getX(), l.getY() + 0.5, l.getZ(), item);
      drop.setUnlimitedLifetime();
      level.addFreshEntity(drop);
      level.sendParticles(ParticleTypes.FLASH, l.getX(), l.getY() + 1, l.getZ(), 1, 0, 0, 0, 0);
      level.sendParticles(GOLD, l.getX(), l.getY() + 1, l.getZ(), 60, 0.5, 0.8, 0.5, 0.0);
      level.playSound(null, l.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.5f, 0.6f);
      l.discard();
      return item;
    }
  }

  // ------------------------------------------------------------------ Ler Residuo
  /** Ler Residuo: no residuo mais perto do ponto mirado, diz quantos glifos a magia tinha e em que ordem (como ???). */
  public static class ReadResidue implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      Vec3 at = LifeEffects.firstCentre(c, t);
      com.arcane.execution.WorldScars.Scar best = null;
      for (var s : com.arcane.execution.WorldScars.all(c.level())) {
        if (s.pos().distanceTo(at) > 8) continue;
        if (best == null || s.pos().distanceTo(at) < best.pos().distanceTo(at)) best = s;
      }
      if (best == null) { LifeEffects.say(c.caster(), "message.arcane.read_residue.none"); return; }
      var mine = c.caster() instanceof Player p ? com.arcane.grimoire.GrimoireAttachments.get(p).orElse(null) : null;
      List<String> parts = new ArrayList<>();
      for (var id : best.glyphs()) {
        boolean known = mine != null && mine.knowsGlyph(id, com.arcane.grimoire.GlyphKnowledge.IDENTIFIED);
        parts.add(known ? Component.translatable(com.arcane.discovery.DiscoveryService.glyphKey(id)).getString() : "???");
      }
      if (c.caster() instanceof Player p) {
        p.displayClientMessage(Component.translatable("message.arcane.read_residue", best.glyphs().size(), String.join(" > ", parts))
            .withStyle(ChatFormatting.LIGHT_PURPLE), false);
      }
      c.level().sendParticles(ParticleTypes.ENCHANT, best.pos().x, best.pos().y + 0.5, best.pos().z, 30, 0.4, 0.4, 0.4, 0.6);
    }
  }

  // ------------------------------------------------------------------ Rastrear Posse
  /** Rastrear Posse: sente para que lado esta o SEU grimorio (ate 500 blocos), com erro de ate 30 graus que cai perto. */
  public static class TrackPossession implements EffectHandler {
    public static final double RANGE = 500;
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof ServerPlayer player)) return;
      Vec3 where = locate(player);
      if (where == null) { LifeEffects.say(player, "message.arcane.track.unknown"); return; }
      double d = where.distanceTo(player.position());
      if (d > RANGE) { LifeEffects.say(player, "message.arcane.track.far"); return; }
      double noise = Math.toRadians(30) * Math.min(1, d / 150.0) * (c.level().random.nextDouble() * 2 - 1);
      Vec3 dir = where.subtract(player.position());
      double ang = Math.atan2(dir.z, dir.x) + noise;
      BlockPos fake = BlockPos.containing(player.getX() + Math.cos(ang) * 10, player.getY(), player.getZ() + Math.sin(ang) * 10);
      String band = d < 16 ? "here" : d < 64 ? "near" : d < 200 ? "mid" : "far_band";
      player.displayClientMessage(Component.translatable("message.arcane.track.found",
          Component.translatable("direction.arcane." + com.arcane.ritual.RitualLeaks.direction(player.blockPosition(), fake)),
          Component.translatable("message.arcane.track." + band)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** Onde esta o grimorio do jogador: no chao, no inventario de alguem, numa bancada de estudo ou dentro de uma Hidra. */
    public static Vec3 locate(ServerPlayer owner) {
      UUID me = owner.getUUID();
      for (ServerLevel level : owner.server.getAllLevels()) {
        if (level != owner.level()) continue; // so na mesma dimensao
        for (Entity e : level.getAllEntities()) {
          if (e instanceof ItemEntity it && com.arcane.item.GrimoireItem.isOwnedBy(it.getItem(), me)) return it.position();
          if (e instanceof Player p && p != owner) {
            for (ItemStack s : p.getInventory().items) if (com.arcane.item.GrimoireItem.isOwnedBy(s, me)) return p.position();
          }
          if (e instanceof com.arcane.entity.HydraEntity h && h.devouredOwners().contains(me)) return h.position();
        }
        Vec3 desk = com.arcane.grimoire.GrimoireDesk.studyOf(level, me);
        if (desk != null) return desk;
      }
      return null;
    }
  }

  // ------------------------------------------------------------------ Portal Pareado
  /** Portal Pareado: liga onde voce esta ao ponto mirado por alguns segundos; quem entra num sai no outro. */
  public static class PairedPortal implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      Vec3 a = c.caster().position();
      Vec3 b = LifeEffects.firstCentre(c, t);
      if (b.distanceTo(a) < 3) return;
      int ticks = (int) Math.min(600, LifeEffects.p(c, call, "duration", 200.0));
      ServerLevel level = c.level();
      Map<UUID, Long> cooldown = new HashMap<>();
      Timed.start(level, ticks, now -> {
        ring(level, a, now);
        ring(level, b, now);
        hop(level, a, b, now, cooldown);
        hop(level, b, a, now, cooldown);
      }, null);
      level.playSound(null, BlockPos.containing(a), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    private static void ring(ServerLevel level, Vec3 at, long now) {
      if (now % 2 != 0) return;
      for (int i = 0; i < 8; i++) {
        double ang = now * 0.2 + i * Math.PI / 4;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x + Math.cos(ang) * 0.9, at.y + 1 + Math.sin(ang) * 0.9, at.z, 1, 0, 0, 0, 0);
      }
    }

    private static void hop(ServerLevel level, Vec3 from, Vec3 to, long now, Map<UUID, Long> cooldown) {
      for (Entity e : level.getEntities((Entity) null, new AABB(from, from).inflate(0.9, 1.5, 0.9), x -> !x.isSpectator())) {
        if (cooldown.getOrDefault(e.getUUID(), 0L) > now) continue;
        cooldown.put(e.getUUID(), now + 30);
        e.teleportTo(to.x, to.y, to.z);
        level.playSound(null, BlockPos.containing(to), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
      }
    }
  }

  // ------------------------------------------------------------------ Contramagia
  /**
   * Contramagia: interrompe magias. Num trecho "Ao Ver Conjuracao", cancela a magia que voce viu ser conjurada se a
   * forca desta (mana com amplificadores) for pelo menos o custo daquela. Fora disso, corta o que os alvos estao
   * canalizando (carregar, segurar, alternar).
   */
  public static class Counterspell implements EffectHandler {
    private static final ThreadLocal<double[]> WATCH = new ThreadLocal<>();

    /** Chamado pela conjuracao antes de acontecer: avisa os vigias e diz se a magia foi anulada. */
    public static boolean intercept(LivingEntity caster, double incomingCost) {
      double[] best = {0};
      WATCH.set(best);
      try {
        com.arcane.execution.ArmedSpells.seeCast(caster);
      } finally {
        WATCH.remove();
      }
      if (best[0] <= 0 || best[0] < incomingCost) return false;
      if (caster.level() instanceof ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, caster.getX(), caster.getY() + 1.2, caster.getZ(), 30, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, caster.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.6f);
      }
      if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.arcane.counterspell.countered").withStyle(ChatFormatting.RED), true);
      return true;
    }

    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      double power = CreatureLevels.spellMana(c) * Math.max(0.1, call.parameters().getOrDefault("strength", 1.0));
      double[] watch = WATCH.get();
      if (watch != null && "see_cast".equals(c.trigger().type())) {
        watch[0] = Math.max(watch[0], power);
        return;
      }
      for (Entity e : t.entities()) {
        if (e instanceof ServerPlayer p && p != c.caster() && com.arcane.execution.CastChannels.isActive(p.getUUID())) {
          com.arcane.execution.CastChannels.stop(p, true);
          c.level().sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
        }
      }
    }
  }

  // ------------------------------------------------------------------ Ilusao
  /** Ilusao: 3 clones seus (so visuais) que imitam a sua intencao por ate 15 s e confundem as criaturas. */
  public static class Illusion implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof Player owner)) return;
      int ticks = (int) Math.min(1200, LifeEffects.p(c, call, "duration", 300.0));
      int count = (int) Math.max(1, Math.min(5, Math.round(call.parameters().getOrDefault("count", 3.0))));
      for (var old : c.level().getEntitiesOfClass(com.arcane.entity.IllusionEntity.class, owner.getBoundingBox().inflate(64),
          e -> e.owner().map(owner.getUUID()::equals).orElse(false))) old.discard();
      for (int i = 0; i < count; i++) {
        var clone = com.arcane.registry.ModEntities.ILLUSION.get().create(c.level());
        if (clone == null) continue;
        clone.setup(owner, i, ticks);
        double yaw = Math.toRadians(owner.getYRot() + 90 + (i - 1) * 70.0);
        clone.moveTo(owner.getX() + Math.cos(yaw) * 2.6, owner.getY(), owner.getZ() + Math.sin(yaw) * 2.6, owner.getYRot(), 0);
        c.level().addFreshEntity(clone);
        c.level().sendParticles(ParticleTypes.POOF, clone.getX(), clone.getY() + 1, clone.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
      }
      c.level().playSound(null, owner.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1f, 1f);
    }

    /** O dono conjurou: os clones fingem conjurar outra coisa. */
    public static void ownerCast(Player owner) {
      for (var clone : owner.level().getEntitiesOfClass(com.arcane.entity.IllusionEntity.class, owner.getBoundingBox().inflate(32),
          e -> e.owner().map(owner.getUUID()::equals).orElse(false))) clone.mimicCast();
    }
  }

  // ------------------------------------------------------------------ Trancar, Selar Bancada, Batizar
  /** Trancar: porta, alcapao, portao ou bau mirado so abre para voce (e seus convidados); fica muito duro de quebrar. */
  public static class Lock implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int n = 0;
      for (var b : t.blocks()) {
        var s = c.level().getBlockState(b.pos());
        boolean lockable = s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.FENCE_GATES)
            || c.level().getBlockEntity(b.pos()) instanceof Container;
        if (!lockable) continue;
        WorldMarks.lock(c.level(), b.pos(), c.caster().getUUID());
        c.level().sendParticles(LoreEffects.GOLD, b.pos().getX() + 0.5, b.pos().getY() + 0.5, b.pos().getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.0);
        n++;
      }
      if (n > 0) c.level().playSound(null, c.caster().blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.7f);
      LifeEffects.say(c.caster(), "message.arcane.lock.done", n);
    }
  }

  /** Selar Grimorio/Bancada: a bancada mirada so obedece ao dono (ninguem mais abre, pega o grimorio ou estuda nela). */
  public static class SealBench implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (var b : t.blocks()) {
        if (!c.level().getBlockState(b.pos()).is(com.arcane.registry.ModBlocks.ARCANE_WORKBENCH.get())) continue;
        WorldMarks.sealBench(c.level(), b.pos(), c.caster().getUUID());
        c.level().sendParticles(LoreEffects.GOLD, b.pos().getX() + 0.5, b.pos().getY() + 1.2, b.pos().getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.0);
        c.level().playSound(null, b.pos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 0.6f);
        LifeEffects.say(c.caster(), "message.arcane.seal_bench.done");
      }
    }
  }

  /** Batizar: da nome a area em volta do ponto mirado (o nome e o nome desta magia); quem entra ve o titulo. */
  public static class Baptize implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      String name = c.inscription().metadata().name();
      if (name == null || name.isBlank()) { LifeEffects.say(c.caster(), "message.arcane.baptize.unnamed"); return; }
      double r = Math.max(4, Math.min(128, LifeEffects.p(c, call, "radius", 24.0)));
      WorldMarks.baptize(c.level(), LifeEffects.firstCentre(c, t), r, name, c.caster().getUUID());
      LifeEffects.say(c.caster(), "message.arcane.baptize.done", name);
    }
  }
}
