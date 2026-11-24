package com.arcane.execution.handlers;

import com.arcane.api.effect.EffectHandler;
import com.arcane.execution.EffectCall;
import com.arcane.execution.ExecutionContext;
import com.arcane.execution.TargetList;
import com.arcane.execution.Targeting;
import com.arcane.execution.Timed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Glifos de vida cotidiana, apoio, percepcao e charme (rodada de 23 nov, lista aceita por Everton). Todos usam sistemas
 * que ja existem: particulas, sons, receitas, o temporizador {@link Timed} e os dados salvos de {@link com.arcane.execution.WorldMarks}.
 */
public final class LifeEffects {
  private LifeEffects() {}

  static double p(ExecutionContext c, EffectCall call, String key, double def) {
    return c.amplify(call.parameters().getOrDefault(key, def), call.magnitudeKeys(), key);
  }

  static void say(LivingEntity who, String key, Object... args) {
    if (who instanceof Player player) player.displayClientMessage(Component.translatable(key, args), true);
  }

  static List<LivingEntity> living(TargetList targets) {
    List<LivingEntity> out = new ArrayList<>();
    for (Entity e : targets.entities()) if (e instanceof LivingEntity l && l.isAlive()) out.add(l);
    return out;
  }

  /** Centros dos alvos: criaturas, blocos e pontos (para efeitos de area). */
  static List<Vec3> centres(TargetList targets) {
    List<Vec3> out = new ArrayList<>();
    for (Entity e : targets.entities()) out.add(e.position());
    for (var b : targets.blocks()) out.add(Vec3.atCenterOf(b.pos()));
    out.addAll(targets.points());
    return out;
  }

  static Vec3 firstCentre(ExecutionContext c, TargetList targets) {
    var all = centres(targets);
    return all.isEmpty() ? c.caster().position() : all.get(0);
  }

  /** Invisibilidade do glifo: o efeito vanilla (sem particulas) mais o Veu Arcano, que esconde armadura e itens. */
  public static class ArcaneInvisibility implements EffectHandler {
    private final PotionEffectHandler base = new PotionEffectHandler(MobEffects.INVISIBILITY, 2400);
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      base.apply(c, call, t);
      for (LivingEntity l : living(t)) {
        var inv = l.getEffect(MobEffects.INVISIBILITY);
        if (inv != null) l.addEffect(new MobEffectInstance(com.arcane.registry.ModEffects.ARCANE_VEIL.get(), inv.getDuration(), 0, false, false, false), c.caster());
      }
    }
  }

  // ================================================================== utilitarios
  /** Remendar: devolve 10% da durabilidade maxima do item na mao principal de cada alvo (fraco e caro de proposito). */
  public static class Mend implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      double frac = Math.min(0.5, 0.10 * p(c, call, "strength", 1.0));
      for (LivingEntity l : living(t)) {
        ItemStack held = l.getMainHandItem();
        if (!held.isDamageableItem() || held.getDamageValue() == 0) continue;
        held.setDamageValue(Math.max(0, held.getDamageValue() - (int) Math.ceil(held.getMaxDamage() * frac)));
        c.level().sendParticles(ParticleTypes.WAX_ON, l.getX(), l.getY() + 1.0, l.getZ(), 12, 0.3, 0.3, 0.3, 0.5);
        c.level().playSound(null, l.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.4f, 1.6f);
      }
    }
  }

  /** Organizar: junta pilhas iguais e ordena o bau mirado (ou o inventario de quem e alvo, menos a barra de atalhos). */
  public static class Organize implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int done = 0;
      for (var b : t.blocks()) if (c.level().getBlockEntity(b.pos()) instanceof Container box) { sort(box, 0, box.getContainerSize()); done++; }
      if (done == 0) for (LivingEntity l : living(t)) if (l instanceof Player pl) { sort(pl.getInventory(), 9, 36); done++; }
      if (done > 0) c.level().playSound(null, c.caster().blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    public static void sort(Container box, int from, int to) {
      Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
      for (int i = from; i < to; i++) {
        ItemStack s = box.getItem(i);
        if (s.isEmpty()) continue;
        groups.computeIfAbsent(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()) + "", k -> new ArrayList<>()).add(s.copy());
        box.setItem(i, ItemStack.EMPTY);
      }
      List<ItemStack> merged = new ArrayList<>();
      groups.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
        List<ItemStack> open = new ArrayList<>();
        for (ItemStack s : e.getValue()) {
          for (ItemStack o : open) {
            if (s.isEmpty()) break;
            if (ItemStack.isSameItemSameTags(o, s) && o.getCount() < o.getMaxStackSize()) {
              int move = Math.min(s.getCount(), o.getMaxStackSize() - o.getCount());
              o.grow(move);
              s.shrink(move);
            }
          }
          if (!s.isEmpty()) open.add(s);
        }
        merged.addAll(open);
      });
      int i = from;
      for (ItemStack s : merged) if (i < to) box.setItem(i++, s);
      box.setChanged();
    }
  }

  /** Recolher: puxa os itens soltos num raio grande (8 a 32, bem mais que o vanilla) ate quem conjurou. */
  public static class Collect implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      double r = Math.min(32, p(c, call, "radius", 16.0));
      Vec3 at = c.caster().position();
      int n = 0;
      for (ItemEntity it : c.level().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(r))) {
        if (it.position().distanceTo(at) > r) continue;
        c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, it.getX(), it.getY() + 0.2, it.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
        it.teleportTo(at.x, at.y + 0.3, at.z);
        it.setNoPickUpDelay();
        n++;
      }
      if (n > 0) c.level().playSound(null, c.caster().blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.7f);
      say(c.caster(), "message.arcane.collect", n);
    }
  }

  /** Marcar Ponto: guarda o ponto mirado para o mago (ate 3) e mostra so para ele uma coluna de luz por 60 s. */
  public static class MarkPoint implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof ServerPlayer player)) return;
      Vec3 at = firstCentre(c, t);
      com.arcane.execution.WorldMarks.addWaypoint(player, BlockPos.containing(at));
      int ticks = (int) Math.min(12000, p(c, call, "duration", 1200.0));
      Timed.start(c.level(), ticks, now -> {
        if (now % 10 != 0 || player.isRemoved()) return;
        for (int h = 0; h < 24; h += 2) c.level().sendParticles(player, ParticleTypes.END_ROD, true, at.x, at.y + h, at.z, 1, 0.05, 0.4, 0.05, 0.0);
      }, null);
      say(player, "message.arcane.mark_point", BlockPos.containing(at).toShortString());
    }
  }

  // ================================================================== apoio
  /** Doar Mana: passa mana sua para os jogadores alvo (chega 80%). */
  public static class DonateMana implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof Player giver)) return;
      var mine = com.arcane.grimoire.GrimoireAttachments.getMana(giver).orElse(null);
      if (mine == null) return;
      double amount = p(c, call, "amount", 20.0);
      for (LivingEntity l : living(t)) {
        if (!(l instanceof Player other) || other == giver) continue;
        var theirs = com.arcane.grimoire.GrimoireAttachments.getMana(other).orElse(null);
        if (theirs == null) continue;
        float give = (float) Math.min(amount, mine.current());
        float room = theirs.max() - theirs.current();
        give = Math.min(give, room / 0.8f);
        if (give <= 0) continue;
        mine.set(mine.current() - give);
        theirs.set(theirs.current() + give * 0.8f);
        c.level().sendParticles(ParticleTypes.ENCHANT, other.getX(), other.getY() + 1, other.getZ(), 30, 0.3, 0.6, 0.3, 0.6);
      }
    }
  }

  /** Aquecer: o inverso de Congelar. Tira o frio e a lentidao das criaturas e derrete gelo e neve perto dos alvos. */
  public static class Warm implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (LivingEntity l : living(t)) {
        l.setTicksFrozen(0);
        l.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        c.level().sendParticles(ParticleTypes.FLAME, l.getX(), l.getY() + 0.8, l.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
      }
      if (!Targeting.mayAlterWorld(c)) return;
      int r = (int) Math.max(1, Math.min(8, p(c, call, "radius", 3.0)));
      for (Vec3 v : centres(t)) {
        BlockPos o = BlockPos.containing(v);
        for (BlockPos q : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
          if (q.distSqr(o) > r * r) continue;
          BlockState s = c.level().getBlockState(q);
          if (s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE)) c.level().setBlockAndUpdate(q, Blocks.WATER.defaultBlockState());
          else if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) c.level().removeBlock(q, false);
        }
      }
    }
  }

  /** Zona de Descanso: cura lenta e fome mais lenta, so em quem nao deu nem levou dano nos ultimos 2 minutos. */
  public static class RestZone implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (LivingEntity l : living(t)) {
        if (com.arcane.execution.WorldMarks.inCombat(l)) continue;
        l.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false, true), c.caster());
        if (l instanceof Player pl) pl.getFoodData().setExhaustion(Math.max(0f, pl.getFoodData().getExhaustionLevel() - 1.0f));
        c.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, l.getX(), l.getY() + 1.2, l.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
      }
    }
  }

  /** Folego Final: uma vez por dia de jogo, a proxima morte do alvo (nos proximos 10 min) vira 1 de vida, com estrondo. */
  public static class LastBreath implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (LivingEntity l : living(t)) {
        com.arcane.execution.WorldMarks.grantLastBreath(l, c.level().getGameTime() + 12000);
        c.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, l.getX(), l.getY() + 1, l.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
      }
    }
  }

  // ================================================================== vida cotidiana
  /** Cozinhar: ate 5 comidas cruas por vez (itens soltos nos alvos ou a mao secundaria). Nao serve para metal. */
  public static class Cook implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int budget = 5;
      List<ItemStack> stacks = new ArrayList<>();
      for (Vec3 v : centres(t)) for (ItemEntity it : c.level().getEntitiesOfClass(ItemEntity.class, new AABB(v, v).inflate(2))) stacks.add(it.getItem());
      if (stacks.isEmpty()) stacks.add(c.caster().getOffhandItem());
      for (ItemStack s : stacks) {
        if (budget <= 0 || s.isEmpty()) continue;
        var recipe = c.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(s), c.level());
        if (recipe.isEmpty()) continue;
        ItemStack out = recipe.get().getResultItem(c.level().registryAccess());
        if (!out.isEdible()) continue;
        int n = Math.min(budget, s.getCount());
        budget -= n;
        s.shrink(n);
        ItemStack cooked = out.copyWithCount(n * out.getCount());
        if (c.caster() instanceof Player pl && !pl.getInventory().add(cooked)) pl.drop(cooked, false);
        else if (!(c.caster() instanceof Player)) c.caster().spawnAtLocation(cooked);
      }
      if (budget < 5) {
        c.level().playSound(null, c.caster().blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 1f, 1f);
        c.level().sendParticles(ParticleTypes.SMOKE, c.caster().getX(), c.caster().getY() + 1, c.caster().getZ(), 10, 0.3, 0.3, 0.3, 0.01);
      }
    }
  }

  /** Secar: tira a molhadura das criaturas; esponja molhada seca e lama vira terra perto dos alvos. */
  public static class Dry implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (LivingEntity l : living(t)) {
        com.arcane.execution.Wetness.dry(l);
        l.clearFire();
        c.level().sendParticles(ParticleTypes.CLOUD, l.getX(), l.getY() + 1, l.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
      }
      if (!Targeting.mayAlterWorld(c)) return;
      int r = (int) Math.max(1, Math.min(6, p(c, call, "radius", 2.0)));
      for (Vec3 v : centres(t)) {
        BlockPos o = BlockPos.containing(v);
        for (BlockPos q : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
          BlockState s = c.level().getBlockState(q);
          if (s.is(Blocks.WET_SPONGE)) c.level().setBlockAndUpdate(q, Blocks.SPONGE.defaultBlockState());
          else if (s.is(Blocks.MUD)) c.level().setBlockAndUpdate(q, Blocks.DIRT.defaultBlockState());
        }
      }
    }
  }

  /** Colheita: colhe as plantacoes maduras no raio e replanta. */
  public static class Harvest implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!Targeting.mayAlterWorld(c)) return;
      int r = (int) Math.max(1, Math.min(16, p(c, call, "radius", 5.0)));
      int n = 0;
      for (Vec3 v : centres(t)) {
        BlockPos o = BlockPos.containing(v);
        for (BlockPos q : BlockPos.betweenClosed(o.offset(-r, -2, -r), o.offset(r, 2, r))) {
          BlockState s = c.level().getBlockState(q);
          if (!(s.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(s)) continue;
          for (ItemStack drop : net.minecraft.world.level.block.Block.getDrops(s, c.level(), q, null)) {
            net.minecraft.world.level.block.Block.popResource(c.level(), q, drop);
          }
          c.level().setBlockAndUpdate(q, crop.getStateForAge(0));
          n++;
        }
      }
      if (n > 0) c.level().playSound(null, c.caster().blockPosition(), SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1f, 1f);
      say(c.caster(), "message.arcane.harvest", n);
    }
  }

  /** Pastorear: os animais alvo seguem quem conjurou por um tempo. */
  public static class Herd implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int ticks = (int) Math.min(1200, p(c, call, "duration", 400.0));
      List<Animal> herd = new ArrayList<>();
      for (LivingEntity l : living(t)) if (l instanceof Animal a) herd.add(a);
      if (herd.isEmpty()) return;
      LivingEntity shepherd = c.caster();
      Timed.start(c.level(), ticks, now -> {
        if (now % 10 != 0 || shepherd.isRemoved()) return;
        for (Animal a : herd) if (a.isAlive() && a.distanceTo(shepherd) > 3) a.getNavigation().moveTo(shepherd, 1.15);
      }, null);
    }
  }

  /** Carinho: o animal ganha coracoes e recupera um pouco de vida. */
  public static class Pet implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      for (LivingEntity l : living(t)) {
        if (!(l instanceof Animal) && !(l instanceof net.minecraft.world.entity.TamableAnimal)) continue;
        l.heal(4f);
        c.level().sendParticles(ParticleTypes.HEART, l.getX(), l.getY() + l.getBbHeight() + 0.3, l.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
      }
    }
  }

  /** Eco: um pulso mostra so para quem conjurou o contorno das cavernas em volta (pontos de luz nas paredes). */
  public static class Echo implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof ServerPlayer player)) return;
      int r = (int) Math.max(4, Math.min(32, p(c, call, "radius", 20.0)));
      BlockPos o = player.blockPosition();
      var rnd = c.level().random;
      int shown = 0;
      for (int i = 0; i < 6000 && shown < 700; i++) {
        BlockPos q = o.offset(rnd.nextInt(2 * r + 1) - r, rnd.nextInt(2 * r + 1) - r, rnd.nextInt(2 * r + 1) - r);
        if (!c.level().getBlockState(q).isAir() || c.level().canSeeSky(q)) continue;
        boolean wall = false;
        for (var d : net.minecraft.core.Direction.values()) if (c.level().getBlockState(q.relative(d)).isSolidRender(c.level(), q.relative(d))) { wall = true; break; }
        if (!wall) continue;
        c.level().sendParticles(player, ParticleTypes.GLOW, true, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 1, 0, 0, 0, 0);
        shown++;
      }
      c.level().playSound(null, o, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.5f);
    }
  }

  /** Prospeccao: dica vaga dos minerios em volta (poucos, alguns, muitos) e a direcao do maior veio. */
  public static class Prospect implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int r = (int) Math.max(4, Math.min(24, p(c, call, "radius", 12.0)));
      BlockPos o = c.caster().blockPosition();
      Map<net.minecraft.world.level.block.Block, int[]> found = new LinkedHashMap<>();
      for (BlockPos q : BlockPos.betweenClosed(o.offset(-r, -r, -r), o.offset(r, r, r))) {
        BlockState s = c.level().getBlockState(q);
        if (!s.is(net.minecraftforge.common.Tags.Blocks.ORES)) continue;
        int[] acc = found.computeIfAbsent(s.getBlock(), b -> new int[3]);
        acc[0]++;
        acc[1] += q.getX() - o.getX();
        acc[2] += q.getZ() - o.getZ();
      }
      if (found.isEmpty()) { say(c.caster(), "message.arcane.prospect.none"); return; }
      var best = found.entrySet().stream().max(Comparator.comparingInt(e -> e.getValue()[0])).get();
      int n = best.getValue()[0];
      String band = n < 6 ? "few" : n < 20 ? "some" : "many";
      BlockPos toward = o.offset(best.getValue()[1] / n, 0, best.getValue()[2] / n);
      say(c.caster(), "message.arcane.prospect", Component.translatable("message.arcane.prospect." + band), best.getKey().getName(),
          Component.translatable("direction.arcane." + com.arcane.ritual.RitualLeaks.direction(o, toward)), found.size());
    }
  }

  /** Lembrar: onde voce morreu e onde dormiu por ultimo (e os pontos marcados). */
  public static class Remember implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof ServerPlayer player)) return;
      String death = player.getLastDeathLocation().map(g -> g.pos().toShortString() + " (" + g.dimension().location().getPath() + ")").orElse("-");
      String bed = player.getRespawnPosition() == null ? "-" : player.getRespawnPosition().toShortString();
      player.displayClientMessage(Component.translatable("message.arcane.remember", death, bed,
          String.join(" | ", com.arcane.execution.WorldMarks.waypoints(player).stream().map(BlockPos::toShortString).toList())), false);
    }
  }

  /** Eco do Passado: silhuetas de quem passou por ali nos ultimos 10 min e brilho onde o terreno foi mudado por magia. */
  public static class EchoOfPast implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      if (!(c.caster() instanceof ServerPlayer player)) return;
      Vec3 at = firstCentre(c, t);
      double r = Math.min(32, p(c, call, "radius", 12.0));
      var gray = new DustParticleOptions(new Vector3f(0.75f, 0.78f, 0.85f), 1.2f);
      int shown = 0;
      for (var ghost : com.arcane.execution.WorldMarks.footprints(c.level(), at, r)) {
        for (double h = 0.1; h < 1.8; h += 0.25) {
          double w = h > 1.3 ? 0.15 : 0.25;
          c.level().sendParticles(player, gray, true, ghost.x, ghost.y + h, ghost.z, 2, w, 0.05, w, 0.0);
        }
        shown++;
      }
      var gold = new DustParticleOptions(new Vector3f(1f, 0.8f, 0.3f), 1.0f);
      for (BlockPos q : com.arcane.restore.BlockJournal.changedNear(c.level(), at, r, 400)) {
        c.level().sendParticles(player, gold, true, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
      }
      say(player, "message.arcane.echo_past", shown);
    }
  }

  // ================================================================== charme
  /** Fogos Arcanos: estouros coloridos nos alvos (so luz e som, sem dano). */
  public static class Fireworks implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      var rnd = c.level().random;
      for (Vec3 v : centres(t)) {
        Vec3 burst = v.add(0, 3 + rnd.nextDouble() * 3, 0);
        var color = new DustParticleOptions(new Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()), 1.5f);
        c.level().sendParticles(ParticleTypes.FIREWORK, burst.x, burst.y, burst.z, 60, 0.1, 0.1, 0.1, 0.25);
        c.level().sendParticles(color, burst.x, burst.y, burst.z, 40, 1.2, 1.2, 1.2, 0.0);
        c.level().sendParticles(ParticleTypes.FLASH, burst.x, burst.y, burst.z, 1, 0, 0, 0, 0);
        c.level().playSound(null, BlockPos.containing(burst), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, 2f, 1f);
        c.level().playSound(null, BlockPos.containing(burst), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.AMBIENT, 1.5f, 1f);
      }
    }
  }

  /** Efeito de charme com duracao: a cada tick roda {@code each} no alvo. */
  abstract static class Charm implements EffectHandler {
    final int defTicks;
    Charm(int defTicks) { this.defTicks = defTicks; }
    abstract void each(ServerLevel level, LivingEntity target, long tick, ExecutionContext c);
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int ticks = (int) Math.min(2400, p(c, call, "duration", defTicks));
      for (LivingEntity l : living(t)) Timed.start(c.level(), ticks, now -> { if (l.isAlive()) each(c.level(), l, now, c); }, null);
    }
  }

  public static class FlowerTrail extends Charm {
    private static final net.minecraft.world.level.block.Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET,
        Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.LILY_OF_THE_VALLEY, Blocks.PINK_TULIP};
    public FlowerTrail() { super(600); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (tick % 4 != 0 || !Targeting.mayAlterWorld(c)) return;
      BlockPos feet = l.blockPosition();
      if (level.getBlockState(feet).isAir() && level.getBlockState(feet.below()).is(BlockTags.DIRT) && level.random.nextInt(3) == 0) {
        level.setBlockAndUpdate(feet, FLOWERS[level.random.nextInt(FLOWERS.length)].defaultBlockState());
      }
    }
  }

  public static class Bubbles extends Charm {
    public Bubbles() { super(400); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (tick % 3 != 0) return;
      level.sendParticles(ParticleTypes.BUBBLE_POP, l.getX(), l.getY() + l.getBbHeight() + 0.2, l.getZ(), 3, 0.4, 0.3, 0.4, 0.02);
      if (tick % 20 == 0) level.playSound(null, l.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 0.6f, 1.4f);
    }
  }

  public static class Tickle extends Charm {
    public Tickle() { super(80); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (tick % 10 != 0) return;
      if (l.onGround()) {
        l.setDeltaMovement(l.getDeltaMovement().add((level.random.nextDouble() - 0.5) * 0.3, 0.25, (level.random.nextDouble() - 0.5) * 0.3));
        l.hurtMarked = true;
      }
      level.playSound(null, l.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 0.5f, 1.9f);
      level.sendParticles(ParticleTypes.NOTE, l.getX(), l.getY() + l.getBbHeight() + 0.3, l.getZ(), 1, 0.2, 0.1, 0.2, 1.0);
    }
  }

  public static class Twirl extends Charm {
    public Twirl() { super(20); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (l instanceof ServerPlayer sp) {
        sp.connection.teleport(0, 0, 0, 18f, 0, java.util.EnumSet.allOf(net.minecraft.world.entity.RelativeMovement.class));
      } else {
        l.setYRot(l.getYRot() + 18f);
        l.setYBodyRot(l.getYRot());
        l.setYHeadRot(l.getYRot());
      }
      if (tick % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, l.getX(), l.getY() + 0.2, l.getZ(), 2, 0.4, 0.0, 0.4, 0.02);
    }
  }

  public static class PlayfulShadow extends Charm {
    public PlayfulShadow() { super(600); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (tick % 2 != 0) return;
      double a = tick * 0.15, hop = Math.abs(Math.sin(tick * 0.3)) * 0.4;
      level.sendParticles(ParticleTypes.SQUID_INK, l.getX() + Math.cos(a) * 0.9, l.getY() + 0.1 + hop, l.getZ() + Math.sin(a) * 0.9, 1, 0.05, 0.02, 0.05, 0.0);
    }
  }

  public static class StarrySky extends Charm {
    public StarrySky() { super(600); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (tick % 5 != 0) return;
      var rnd = level.random;
      for (int i = 0; i < 6; i++) {
        double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * 16;
        level.sendParticles(ParticleTypes.END_ROD, l.getX() + Math.cos(a) * d, l.getY() + 12 + rnd.nextDouble() * 8, l.getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0.0);
      }
    }
  }

  public static class Melody extends Charm {
    private static final float[] SCALE = {0.5f, 0.56f, 0.63f, 0.75f, 0.84f, 1.0f, 1.12f, 1.26f, 1.5f, 1.68f, 2.0f};
    public Melody() { super(64); }
    @Override void each(ServerLevel level, LivingEntity l, long tick, ExecutionContext c) {
      if (tick % 4 != 0) return;
      float pitch = SCALE[(int) Math.abs((tick * 7919L + l.getId()) % SCALE.length)];
      level.playSound(null, l.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, 1f, pitch);
      level.sendParticles(ParticleTypes.NOTE, l.getX(), l.getY() + l.getBbHeight() + 0.4, l.getZ(), 1, 0.3, 0.1, 0.3, pitch / 2.0);
    }
  }

  /** Brilhar Item: os itens soltos perto dos alvos ganham contorno brilhante por um tempo (achar o que caiu). */
  public static class GlowItem implements EffectHandler {
    @Override public void apply(ExecutionContext c, EffectCall call, TargetList t) {
      int ticks = (int) Math.min(6000, p(c, call, "duration", 1200.0));
      double r = Math.min(24, p(c, call, "radius", 8.0));
      List<ItemEntity> glowing = new ArrayList<>();
      for (Vec3 v : centres(t)) for (ItemEntity it : c.level().getEntitiesOfClass(ItemEntity.class, new AABB(v, v).inflate(r))) {
        if (!it.isCurrentlyGlowing()) { it.setGlowingTag(true); glowing.add(it); }
      }
      Timed.start(c.level(), ticks, null, () -> glowing.forEach(it -> { if (!it.isRemoved()) it.setGlowingTag(false); }));
    }
  }
}
