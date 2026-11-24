package com.arcane.entity;

import com.arcane.config.ArcaneConfig;
import com.arcane.network.ModNetwork;
import com.arcane.network.packet.S2CShapeFxPacket;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hidra: guardia de um dos 7 covis. Tem 14 estagios de vida, de recem-nascida (1) a anciã (14); o estagio define o
 * tamanho, quantas cabecas tem (1 a 9), a vida, o dano e quais poderes usa. Vida, dano e alcance crescem de forma
 * EXPONENCIAL com o estagio: a recem-nascida e uma luta justa, a anciã e um cataclismo. A forca tambem se ajusta a
 * quantos jogadores lutam contra ela (os por perto pesam inteiro, os outros online pesam pouco).
 *
 * <p>Nasce SELADA: dorme presa pelos selos do ritual no alto dos pilares da camara (de 2 a 8, pelo estagio), sem se
 * mexer e sem poder ser ferida. Quebrar o ultimo selo a desperta. Do estagio 5 em diante ela devasta a regiao: abre
 * crateras e poe fogo, e se nao for derrotada a tempo cava ate a superficie e sai destruindo tudo em volta.
 * Ao morrer, quem lutou aprende o glifo original do covil (ver HydraLairs).
 */
public class HydraEntity extends Monster {
  public static final int MAX_STAGE = 14;
  private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Poder sendo usado agora (0 = nenhum), para o modelo abrir as bocas / erguer as cabecas. */
  private static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Selos ainda inteiros: um bit por pilar da camara (0 = desperta). */
  private static final EntityDataAccessor<Integer> SEALS = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Centro da camara (para desenhar as correntes ate os selos). */
  private static final EntityDataAccessor<BlockPos> HOME = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.BLOCK_POS);
  private static final UUID HEALTH_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a01");
  private static final UUID DAMAGE_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a02");

  public static final int BREATH = 1, SWEEP = 2, ROAR = 3, STORM = 4, METEOR = 5, CATACLYSM = 6, DEVASTATE = 7;
  /** A partir deste estagio a Hidra destroi o terreno. */
  public static final int DEVASTATION_STAGE = 5;
  /** Pilares da camara (os selos ficam no alto deles): 8, a {@link #PILLAR_RADIUS} do centro, topo a 6 blocos do chao. */
  public static final int PILLARS = 8, PILLAR_RADIUS = 11, PILLAR_TOP = 6;
  /** Tempo entre o aviso (circulo vermelho) e a queda de um golpe de devastacao. */
  public static final int STRIKE_WARNING = 30;

  private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.arcane.hydra"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
  private final Set<UUID> fighters = new HashSet<>();
  private int lairIndex = -1;
  private int powerCooldown = 60;
  private int powerTicks;
  private double playerFactor = 1.0;
  /** Grimorios que a Hidra devorou (itens inteiros, devolvidos ao morrer); as magias deles viram poderes dela. */
  private final java.util.List<net.minecraft.world.item.ItemStack> devoured = new java.util.ArrayList<>();
  private static final UUID SPEED_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a03");
  public static final int DEVOURED_SPELL = 8;

  // ------------------------------------------------------------------ cabecas independentes
  /** Alvo de cada cabeca (id da entidade no cliente, -1 = nenhum): cada cabeca olha e morde o seu. */
  @SuppressWarnings("unchecked")
  private static final EntityDataAccessor<Integer>[] HEAD_TARGET = new EntityDataAccessor[9];
  /** Cabecas mordendo agora (bit i = cabeca i), para a animacao do bote. */
  private static final EntityDataAccessor<Integer> BITING = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  static {
    for (int i = 0; i < 9; i++) HEAD_TARGET[i] = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  }
  private final int[] headCooldown = new int[9];
  private final int[] biteTicks = new int[9];
  /**
   * A vida do Minecraft tem teto de 1024. Acima disso a Hidra fica "mais dura": o dano recebido e dividido por este
   * fator, entao a vida efetiva e vida x dureza (a barra de chefe mostra a fracao, que e a mesma).
   */
  private double toughness = 1.0;
  /** Dentro do covil (presa ao raio da camara) ou ja solta na superficie. */
  private boolean inLair;
  private int awakeTicks, idleTicks, breakoutTicks = -1, rampageCooldown = 100;

  /** Golpe de devastacao anunciado (circulo vermelho no chao) esperando a hora de cair. */
  private static final class Strike {
    final Vec3 at;
    final float radius;
    int ticks;

    Strike(Vec3 at, float radius, int ticks) {
      this.at = at;
      this.radius = radius;
      this.ticks = ticks;
    }
  }

  private final List<Strike> strikes = new ArrayList<>();

  public HydraEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    this.xpReward = 500;
    setPersistenceRequired();
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 80.0)
        .add(Attributes.ATTACK_DAMAGE, 4.0)
        .add(Attributes.ARMOR, 2.0)
        .add(Attributes.MOVEMENT_SPEED, 0.24)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
        .add(Attributes.FOLLOW_RANGE, 48.0);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(STAGE, 1);
    entityData.define(POWER, 0);
    for (var a : HEAD_TARGET) entityData.define(a, -1);
    entityData.define(BITING, 0);
    entityData.define(SEALS, 0);
    entityData.define(HOME, BlockPos.ZERO);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24f));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
  }

  // ------------------------------------------------------------------ estagio e curvas
  public int stage() { return entityData.get(STAGE); }

  public int power() { return entityData.get(POWER); }

  public int lairIndex() { return lairIndex; }

  public void setLairIndex(int i) { this.lairIndex = i; }

  private static int clampStage(int s) { return Math.max(1, Math.min(MAX_STAGE, s)); }

  /** Cabecas visiveis: 1 no estagio 1, 9 no 14. */
  public static int headsFor(int stage) { return 1 + Math.round((clampStage(stage) - 1) * 8f / 13f); }

  public int heads() { return headsFor(stage()); }

  /** Escala do corpo: 1 no estagio 1, 4,25 no 14. */
  /** Tamanho: 1,8x na recem-nascida ate 7x na ancia (antes 1x a 4,25x: pequena demais, decisao de Everton). */
  public static float scaleFor(int stage) { return 1.8f + (clampStage(stage) - 1) * 0.4f; }

  /** Vida: 80 na recem-nascida, x1,55 a cada estagio (~525 no 5, ~4,6 mil no 10, ~24 mil na anciã). */
  public static double baseHealth(int stage) { return 80.0 * Math.pow(1.55, clampStage(stage) - 1); }

  /** Dano: 4 na recem-nascida, x1,28 a cada estagio (~11 no 5, ~37 no 10, ~100 na anciã). */
  public static double baseDamage(int stage) { return 4.0 * Math.pow(1.28, clampStage(stage) - 1); }

  /** Alcance dos poderes: x1,17 a cada estagio (1 na recem-nascida, ~1,9 no 5, ~7,7 na anciã). */
  public static double reach(int stage) { return Math.pow(1.17, clampStage(stage) - 1); }

  public static boolean devastates(int stage) { return stage >= DEVASTATION_STAGE; }

  /** Raio de cada cratera: ~6,5 no estagio 5, ~14 no 10, ate 16 (as maiores caem em varias de uma vez). */
  public static float devastationRadius(int stage) {
    return devastates(stage) ? (float) Math.min(16.0, 3.5 * reach(stage)) : 0f;
  }

  /** Crateras por golpe: 1 no estagio 5, 4 na anciã. */
  public static int cratersPerStrike(int stage) { return devastates(stage) ? 1 + (clampStage(stage) - DEVASTATION_STAGE) / 3 : 0; }

  /** Intervalo (ticks) entre as devastacoes de quem anda solta: ~10 s no estagio 5, 3 s na anciã. */
  public static int rampagePeriod(int stage) { return Math.max(60, 300 - 18 * clampStage(stage)); }

  /** Quantos selos prendem a Hidra: 2 na recem-nascida, 8 na anciã. */
  public static int sealsFor(int stage) { return 2 + Math.round((clampStage(stage) - 1) * 6f / 13f); }

  /** Mascara dos pilares que recebem selo (espalhados pela camara). */
  public static int sealMaskFor(int stage) {
    int n = sealsFor(stage), mask = 0;
    for (int k = 0; k < n; k++) mask |= 1 << (Math.round(k * (float) PILLARS / n) % PILLARS);
    return mask;
  }

  /** Deslocamento do alto do pilar {@code i} (onde fica o selo) em relacao ao centro da camara. */
  public static BlockPos pillarTop(int i) {
    double a = i * Math.PI * 2 / PILLARS;
    return new BlockPos((int) Math.round(Math.cos(a) * PILLAR_RADIUS), PILLAR_TOP, (int) Math.round(Math.sin(a) * PILLAR_RADIUS));
  }

  public void setStage(int stage) {
    int s = clampStage(stage);
    entityData.set(STAGE, s);
    applyHealth(baseHealth(s));
    getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage(s));
    getAttribute(Attributes.ARMOR).setBaseValue(Math.min(30, 2.0 * s));
    getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22 + 0.006 * s);
    setHealth(getMaxHealth());
    refreshDimensions();
    updateBossName();
  }

  private void updateBossName() {
    int s = stage();
    Component name = Component.translatable("entity.arcane.hydra.stage", Component.translatable("hydra.arcane.stage." + s), s);
    if (sealed()) name = Component.translatable("entity.arcane.hydra.sealed", name, Integer.bitCount(sealMask()));
    bossBar.setName(name);
    bossBar.setColor(sealed() ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);
    if (STAGE.equals(key)) refreshDimensions();
  }

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    return super.getDimensions(pose).scale(scaleFor(stage()));
  }

  // ------------------------------------------------------------------ selo
  public int sealMask() { return entityData.get(SEALS); }

  public boolean sealed() { return sealMask() != 0; }

  public BlockPos home() { return entityData.get(HOME); }

  public boolean inLair() { return inLair; }

  public int awakeTicks() { return awakeTicks; }

  /** Atualiza os selos que sobram; zerar desperta a Hidra. */
  public void setSealMask(int mask) {
    boolean was = sealed();
    entityData.set(SEALS, mask);
    setNoAi(mask != 0);
    updateBossName();
    if (was && mask == 0) awaken();
  }

  private void awaken() {
    awakeTicks = 0;
    idleTicks = 0;
    powerCooldown = 30;
    if (!(level() instanceof ServerLevel sl)) return;
    sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 5.0f, 0.5f + 0.04f * (MAX_STAGE - stage()));
    sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0f, 0.5f);
    ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, position(), Vec3.ZERO, getBbWidth() * 2f + 6f, 0xFF3040, S2CShapeFxPacket.FX_AWAKEN, 50);
    for (ServerPlayer p : sl.players()) {
      if (p.distanceToSqr(this) < 96 * 96) {
        p.displayClientMessage(Component.translatable("message.arcane.hydra.unsealed", Component.translatable("hydra.arcane.stage." + stage()), stage())
            .withStyle(ChatFormatting.RED), false);
      }
    }
    use(ROAR, null);
  }

  // ------------------------------------------------------------------ jogadores
  /**
   * Peso dos jogadores: quem esta a ate 64 blocos conta 1, o resto online conta 0,3. Vida e dano sobem com o peso
   * (a vida guarda a mesma fracao ao mudar). 1 jogador sozinho = forca base.
   */
  public static double playerWeight(int near, int farOnline) {
    return Math.max(1.0, near + 0.3 * farOnline);
  }

  private void updatePlayerFactor() {
    if (!(level() instanceof ServerLevel sl)) return;
    int near = 0, far = 0;
    for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
      if (p.isSpectator() || p.isCreative()) continue;
      if (p.level() == level() && p.distanceToSqr(this) < 64 * 64) near++;
      else far++;
    }
    double f = playerWeight(near, far);
    if (Math.abs(f - playerFactor) < 1.0e-3) return;
    playerFactor = f;
    float frac = getHealth() / getMaxHealth();
    applyHealth(baseHealth(stage()) * (1.0 + 0.75 * (f - 1)));
    setModifier(getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, 0.35 * (f - 1));
    setModifier(getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, Math.min(0.6, 0.12 * (f - 1)));
    setHealth(frac * getMaxHealth());
  }

  private static void setModifier(AttributeInstance attr, UUID id, double amount) {
    if (attr == null) return;
    attr.removeModifier(id);
    if (amount > 0) attr.addTransientModifier(new AttributeModifier(id, "arcane hydra players", amount, AttributeModifier.Operation.MULTIPLY_BASE));
  }

  public double playerFactor() { return playerFactor; }

  /** Ataca mais rapido com mais jogadores: a espera entre poderes cai (ate a metade). */
  public static double cooldownFactor(double players) { return 1.0 / Math.min(2.0, 1.0 + 0.25 * (players - 1)); }

  /** Variacao: cada jogador a mais libera os poderes como se a Hidra fosse 1 estagio acima (dano e vida nao mudam). */
  public static int varietyStage(int stage, double players) { return clampStage(stage + (int) Math.floor(players - 1)); }

  /**
   * Inteligencia: com 2 ou mais jogadores ela troca de alvo para o mais vulneravel (menor fracao de vida) e, com 3 ou
   * mais, prefere quem acabou de conjurar (o mago e a ameaca). Sozinho o jogador enfrenta o comportamento base.
   */
  private void thinkAboutTarget() {
    if (playerFactor < 2.0 || !(level() instanceof ServerLevel sl)) return;
    LivingEntity best = getTarget();
    double bestScore = best == null ? Double.MAX_VALUE : score(best);
    for (ServerPlayer p : sl.players()) {
      if (p.isSpectator() || p.isCreative() || !p.isAlive() || p.distanceToSqr(this) > 48 * 48) continue;
      double sc = score(p);
      if (sc < bestScore - 0.1) { best = p; bestScore = sc; }
    }
    if (best != null && best != getTarget()) setTarget(best);
  }

  private double score(LivingEntity e) {
    double sc = e.getHealth() / Math.max(1f, e.getMaxHealth());
    if (playerFactor >= 3.0 && e instanceof ServerPlayer p && com.arcane.execution.CastChannels.isActive(p.getUUID())) sc -= 0.5;
    return sc;
  }

  /** Id da entidade que a cabeca i esta mirando (-1 = nenhuma). */
  public int headTarget(int i) { return i < 0 || i >= 9 ? -1 : entityData.get(HEAD_TARGET[i]); }

  public boolean headBiting(int i) { return (entityData.get(BITING) & (1 << i)) != 0; }

  /** Alcance do pescoco: ate onde uma cabeca morde sem o corpo andar. */
  public double neckReach() { return 2.5 + getBbWidth() * 1.2; }

  /**
   * Cabecas independentes: a cada segundo, distribui os alvos por perto (jogadores e criaturas que a atacaram ou que
   * ela caca) entre as cabecas, uma por alvo, revezando quando ha mais cabecas que alvos. Cada cabeca morde o seu alvo
   * quando ele esta ao alcance do pescoco, com a sua propria espera; o dano de cada mordida e uma fracao do golpe.
   */
  private void tickHeads() {
    if (!(level() instanceof ServerLevel sl)) return;
    int n = heads();
    if (tickCount % 20 == 3) {
      java.util.List<LivingEntity> foes = new java.util.ArrayList<>(enemiesWithin(28 + getBbWidth()));
      foes.removeIf(e -> e instanceof Player p && (p.isCreative() || p.isSpectator()));
      foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(this)));
      if (getTarget() != null && getTarget().isAlive()) {
        foes.remove(getTarget());
        foes.add(0, getTarget()); // a cabeca do meio fica com o alvo principal
      }
      for (int i = 0; i < 9; i++) {
        int id = -1;
        if (i < n && !foes.isEmpty()) {
          // cabeca do meio primeiro, depois as vizinhas alternando lados
          int order = Math.abs(i - (n - 1) / 2) * 2 - (i < (n - 1) / 2 ? 1 : 0);
          id = foes.get(Math.max(0, order) % foes.size()).getId();
        }
        entityData.set(HEAD_TARGET[i], id);
      }
    }
    int biting = 0;
    double reach = neckReach();
    float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35);
    for (int i = 0; i < n; i++) {
      if (biteTicks[i] > 0) { biteTicks[i]--; biting |= 1 << i; }
      if (headCooldown[i] > 0) { headCooldown[i]--; continue; }
      var e = sl.getEntity(entityData.get(HEAD_TARGET[i]));
      if (!(e instanceof LivingEntity t) || !t.isAlive() || t.distanceTo(this) > reach + t.getBbWidth()) continue;
      headCooldown[i] = 25 + random.nextInt(30);
      biteTicks[i] = 8;
      biting |= 1 << i;
      t.hurt(damageSources().mobAttack(this), dmg);
      sl.playSound(null, t.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.0f, 0.6f + random.nextFloat() * 0.3f);
    }
    entityData.set(BITING, biting);
  }

  // ------------------------------------------------------------------ grimorios devorados
  public int devouredCount() { return devoured.size(); }

  /** Donos dos grimorios que ela devorou (Rastrear Posse aponta para ela). */
  public java.util.Set<UUID> devouredOwners() {
    java.util.Set<UUID> out = new java.util.HashSet<>();
    for (var b : devoured) if (b.hasTag() && b.getTag().hasUUID(com.arcane.item.GrimoireItem.OWNER)) out.add(b.getTag().getUUID(com.arcane.item.GrimoireItem.OWNER));
    return out;
  }

  /** Magias de todos os grimorios devorados. */
  public java.util.List<com.arcane.inscription.Inscription> devouredSpells() {
    java.util.List<com.arcane.inscription.Inscription> out = new java.util.ArrayList<>();
    for (var book : devoured) {
      var contents = com.arcane.item.GrimoireItem.contents(book);
      if (contents == null) continue;
      var st = new com.arcane.grimoire.GrimoireState();
      st.load(contents);
      out.addAll(st.spellbook().values());
    }
    return out;
  }

  /** Engole os grimorios caidos ao alcance (some o item; volta inteiro quando a Hidra morre). */
  public int devourNearby() {
    if (!(level() instanceof ServerLevel sl)) return 0;
    double r = 4.0 + getBbWidth();
    int n = 0;
    for (var item : sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(r),
        it -> com.arcane.item.GrimoireItem.contents(it.getItem()) != null)) {
      devoured.add(item.getItem().copy());
      var tag = item.getItem().getTag();
      if (tag != null && tag.hasUUID(com.arcane.item.GrimoireItem.OWNER)) {
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(tag.getUUID(com.arcane.item.GrimoireItem.OWNER));
        if (owner != null) owner.displayClientMessage(Component.translatable("message.arcane.hydra.devoured").withStyle(net.minecraft.ChatFormatting.DARK_RED), false);
      }
      item.discard();
      n++;
      sl.sendParticles(ParticleTypes.ENCHANT, item.getX(), item.getY() + 0.5, item.getZ(), 40, 0.5, 0.5, 0.5, 1.0);
      sl.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 1.3f);
    }
    return n;
  }

  /** Lanca uma das magias devoradas no alvo (com o seu proprio poder de criatura). */
  private void castDevoured(LivingEntity target) {
    var spells = devouredSpells();
    if (spells.isEmpty() || !(level() instanceof ServerLevel sl)) return;
    var spell = spells.get(random.nextInt(spells.size()));
    getLookControl().setLookAt(target, 180f, 90f);
    lookAt(target, 180f, 90f);
    com.arcane.execution.CastingService.tryCast(this, spell, sl);
    sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight(), getZ(), 30, getBbWidth() * 0.4, 0.5, getBbWidth() * 0.4, 0.1);
  }

  /** Vida alvo: o que cabe no atributo (ate 1024) vira vida; o resto vira dureza. */
  private void applyHealth(double wanted) {
    double cap = 1024.0;
    getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(cap, wanted));
    toughness = Math.max(1.0, wanted / cap);
  }

  public double effectiveMaxHealth() { return getMaxHealth() * toughness; }

  // ------------------------------------------------------------------ tick
  @Override
  public void aiStep() {
    super.aiStep();
    if (level().isClientSide) return;
    if (tickCount % 100 == 1) updatePlayerFactor();
    if (tickCount % 20 == 7) devourNearby();
    if (tickCount % 100 == 51) thinkAboutTarget();
    bossBar.setProgress(getHealth() / getMaxHealth());
    if (sealed()) {
      // dormindo: so respira fumaca; nada de poderes
      if (tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight() * 0.8, getZ(), 2, getBbWidth() * 0.3, 0.2, getBbWidth() * 0.3, 0.01);
      }
      return;
    }
    awakeTicks++;
    tickHeads();
    tickStrikes();
    tickRampage();
    if (powerTicks > 0) {
      powerTicks--;
      if (power() == BREATH) breathTick();
      if (powerTicks == 0) entityData.set(POWER, 0);
      return;
    }
    LivingEntity target = getTarget();
    if (target == null || !target.isAlive()) return;
    if (--powerCooldown > 0) return;
    int s = varietyStage(stage(), playerFactor);
    powerCooldown = (int) ((Math.max(24, (int) (120 / Math.sqrt(reach(stage())))) + random.nextInt(40)) * cooldownFactor(playerFactor));
    if (!devoured.isEmpty() && random.nextInt(100) < 25) {
      entityData.set(POWER, DEVOURED_SPELL);
      powerTicks = 20; // a pose de conjurar dura 1 s e volta ao normal
      castDevoured(target);
      return;
    }
    double d = distanceTo(target);
    int choice;
    int roll = random.nextInt(100);
    if (s >= 13 && roll < 12) choice = CATACLYSM;
    else if (devastates(s) && roll < 30) choice = DEVASTATE;
    else if (s >= 10 && roll < 45) choice = METEOR;
    else if (s >= 7 && roll < 58) choice = STORM;
    else if (s >= 4 && roll < 66) choice = ROAR;
    else if (s >= 3 && d < sweepRadius(s) + 1) choice = SWEEP;
    else choice = BREATH;
    use(choice, target);
  }

  private static double sweepRadius(int s) { return Math.min(20.0, 4.0 * Math.pow(reach(s), 0.8)); }

  // ------------------------------------------------------------------ poderes
  /** Usa um poder. {@code target} pode ser nulo (o rugido de quando desperta). */
  public void use(int choice, LivingEntity target) {
    if (!(level() instanceof ServerLevel sl)) return;
    entityData.set(POWER, choice);
    int s = stage();
    double reach = reach(s);
    double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE);
    switch (choice) {
      case BREATH -> {
        powerTicks = 30;
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 1.6f - s * 0.06f);
      }
      case SWEEP -> {
        powerTicks = 10;
        double r = sweepRadius(s);
        for (LivingEntity e : enemiesWithin(r)) {
          e.hurt(damageSources().mobAttack(this), (float) (dmg * 0.9));
          Vec3 push = e.position().subtract(position()).normalize().scale(1.2 + reach * 0.3);
          e.push(push.x, 0.5 + reach * 0.1, push.z);
          e.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.5, getZ(), 12, r * 0.4, 0.2, r * 0.4, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.6f);
      }
      case ROAR -> {
        powerTicks = 30;
        double r = Math.min(64, 20 * Math.sqrt(reach));
        for (LivingEntity e : enemiesWithin(r)) {
          e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1 + s / 6), this);
          e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, s / 5), this);
          e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), this);
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, position(), Vec3.ZERO, (float) r, 0xB040FF, S2CShapeFxPacket.FX_ROAR, 24);
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.5f);
      }
      case STORM -> {
        powerTicks = 20;
        int bolts = 1 + (int) (reach * 1.2);
        List<LivingEntity> foes = enemiesWithin(40 * Math.sqrt(reach));
        for (int i = 0; i < bolts; i++) {
          Vec3 at = !foes.isEmpty() && random.nextInt(3) > 0 ? foes.get(random.nextInt(foes.size())).position()
              : position().add(random.nextGaussian() * 10 * reach, 0, random.nextGaussian() * 10 * reach);
          var bolt = EntityType.LIGHTNING_BOLT.create(sl);
          if (bolt == null) continue;
          bolt.moveTo(at.x, at.y, at.z);
          bolt.setVisualOnly(false);
          sl.addFreshEntity(bolt);
        }
      }
      case METEOR -> {
        powerTicks = 20;
        int rocks = 2 + (int) reach;
        int power = griefing(sl) ? 1 + (s - 1) / 4 : 0;
        Vec3 aim = target != null ? target.position() : position();
        for (int i = 0; i < rocks; i++) {
          Vec3 at = aim.add(random.nextGaussian() * 6 * Math.sqrt(reach), 0, random.nextGaussian() * 6 * Math.sqrt(reach));
          LargeFireball ball = new LargeFireball(sl, this, 0, -1.6, 0, power);
          ball.setPos(at.x, at.y + 22 + random.nextInt(8), at.z);
          sl.addFreshEntity(ball);
        }
        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0f, 0.4f);
      }
      case DEVASTATE -> {
        // aviso: um circulo vermelho em volta de cada alvo; 1,5 s depois o chao explode e some
        powerTicks = 16;
        Vec3 aim = target != null ? target.position() : position();
        int n = cratersPerStrike(s);
        float r = devastationRadius(s);
        for (int i = 0; i < n; i++) {
          Vec3 at = i == 0 ? aim : aim.add(random.nextGaussian() * r * 1.4, 0, random.nextGaussian() * r * 1.4);
          announceStrike(sl, at, r, STRIKE_WARNING + i * 6);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0f, 0.5f);
      }
      case CATACLYSM -> {
        // O fim do mundo em miniatura: onda de choque enorme, cura com o dano causado e um anel de fogo.
        powerTicks = 40;
        double r = Math.min(48, 20 * Math.sqrt(reach));
        float healed = 0;
        for (LivingEntity e : enemiesWithin(r)) {
          float before = e.getHealth();
          e.hurt(damageSources().indirectMagic(this, this), (float) (dmg * 1.6));
          healed += Math.max(0, before - e.getHealth());
          Vec3 push = e.position().subtract(position()).normalize().scale(2.5);
          e.push(push.x, 1.2, push.z);
          e.hurtMarked = true;
          e.setSecondsOnFire(8);
        }
        heal(healed * 0.5f);
        for (int i = 0; i < 72; i++) {
          double a = Math.PI * 2 * i / 72;
          sl.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * 10, getY() + 0.3, getZ() + Math.sin(a) * 10, 2, 0.2, 0.1, 0.2, 0.05);
          sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * 18, getY() + 0.3, getZ() + Math.sin(a) * 18, 1, 0.2, 0.1, 0.2, 0.05);
        }
        ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, position(), Vec3.ZERO, (float) r, 0xFF5020, S2CShapeFxPacket.FX_DEVASTATE, 40);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 3, 3, 1, 3, 0);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.6f);
      }
      default -> entityData.set(POWER, 0);
    }
  }

  /** Sopro: um cone de fogo (estagio 8+ vira sopro do dragao, que tambem seca) de cada cabeca na direcao do olhar. */
  private void breathTick() {
    if (!(level() instanceof ServerLevel sl)) return;
    int s = stage();
    LivingEntity target = getTarget();
    if (target != null) getLookControl().setLookAt(target, 30f, 30f);
    Vec3 look = target != null ? target.getEyePosition().subtract(getEyePosition()).normalize() : getLookAngle();
    double range = Math.min(32, 6 * Math.pow(reach(s), 0.7) + s * 0.4);
    Vec3 mouth = getEyePosition().add(look.scale(getBbWidth() * 0.6));
    var particle = s >= 8 ? ParticleTypes.DRAGON_BREATH : ParticleTypes.FLAME;
    for (int i = 0; i < 4 + heads(); i++) {
      double f = 0.6 + random.nextDouble() * 0.6;
      Vec3 v = look.add(random.nextGaussian() * 0.12, random.nextGaussian() * 0.12, random.nextGaussian() * 0.12).scale(f);
      sl.sendParticles(particle, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
    }
    if (powerTicks % 5 != 0) return;
    double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35 * Math.max(1, heads() / 2.0);
    for (LivingEntity e : enemiesWithin(range)) {
      Vec3 to = e.getEyePosition().subtract(mouth);
      if (to.length() > range || to.normalize().dot(look) < 0.82) continue;
      e.hurt(damageSources().indirectMagic(this, this), (float) dmg);
      e.setSecondsOnFire(4);
      if (s >= 8) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, s >= 12 ? 1 : 0), this);
    }
  }

  private List<LivingEntity> enemiesWithin(double r) {
    return enemiesNear(position(), r);
  }

  private List<LivingEntity> enemiesNear(Vec3 c, double r) {
    return level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
        e -> e != this && e.isAlive() && !(e instanceof HydraEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
            && e.distanceToSqr(c) <= r * r);
  }

  // ------------------------------------------------------------------ devastacao
  /** A Hidra pode destruir o terreno? (config + regra mobGriefing). */
  public static boolean griefing(ServerLevel sl) {
    return ArcaneConfig.HYDRA_DEVASTATION.get() && sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
  }

  private void announceStrike(ServerLevel sl, Vec3 at, float radius, int ticks) {
    // o golpe cai no chao: desce ate o primeiro bloco solido (ate 24 abaixo)
    BlockPos p = BlockPos.containing(at);
    for (int i = 0; i < 24 && sl.isEmptyBlock(p.below()) && p.getY() > sl.getMinBuildHeight(); i++) p = p.below();
    Vec3 ground = new Vec3(at.x, p.getY(), at.z);
    strikes.add(new Strike(ground, radius, ticks));
    ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, ground, Vec3.ZERO, radius, 0xFF2020, S2CShapeFxPacket.FX_WARN, ticks);
  }

  private void tickStrikes() {
    if (strikes.isEmpty() || !(level() instanceof ServerLevel sl)) return;
    for (var it = strikes.iterator(); it.hasNext(); ) {
      Strike st = it.next();
      if (--st.ticks > 0) continue;
      it.remove();
      devastate(sl, st.at, st.radius);
    }
  }

  public int pendingStrikes() { return strikes.size(); }

  /**
   * Uma cratera de raio {@code radius} em {@code at}: fere e queima quem esta dentro, apaga os blocos (pela fila de
   * crateras, sem travar o servidor; o Restaurar desfaz) e poe fogo na borda. Rocha-mae, baus e selos ficam.
   * So mexe em chunks ja carregados.
   */
  public void devastate(ServerLevel sl, Vec3 at, float radius) {
    double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2;
    for (LivingEntity e : enemiesNear(at, radius * 1.3)) {
      e.hurt(damageSources().explosion(this, this), (float) dmg);
      Vec3 push = e.position().subtract(at).normalize().scale(1.5);
      e.push(push.x, 1.0, push.z);
      e.hurtMarked = true;
      e.setSecondsOnFire(6);
    }
    ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, at, Vec3.ZERO, radius, 0xFF5020, S2CShapeFxPacket.FX_DEVASTATE, 36);
    sl.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 6.0f, 0.5f);
    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1, at.z, 2, radius * 0.3, 1, radius * 0.3, 0);
    if (!griefing(sl)) return;
    int n = (int) Math.ceil(radius);
    BlockPos c = BlockPos.containing(at);
    if (!sl.hasChunksAt(c.offset(-n - 2, 0, -n - 2), c.offset(n + 2, 0, n + 2))) return;
    LongArrayList list = new LongArrayList();
    var seal = com.arcane.registry.ModBlocks.HYDRA_SEAL.get();
    for (int dx = -n; dx <= n; dx++) for (int dy = -n; dy <= n; dy++) for (int dz = -n; dz <= n; dz++) {
      // borda irregular: o raio varia um pouco com a direcao
      double wobble = 0.85 + 0.15 * Math.sin(dx * 1.7 + dz * 0.9) * Math.cos(dy * 1.3 - dx * 0.5);
      if (dx * dx + dy * dy * 1.4 + dz * dz > radius * radius * wobble * wobble) continue;
      BlockPos p = c.offset(dx, dy, dz);
      var st = sl.getBlockState(p);
      if (st.isAir() || st.getDestroySpeed(sl, p) < 0 || st.hasBlockEntity() || st.is(seal)) continue;
      list.add(p.asLong());
    }
    com.arcane.execution.CraterQueue.submit(sl, list, at);
    // fogo e brasa em volta
    int fires = 8 + n * 2;
    for (int i = 0; i < fires; i++) {
      double a = random.nextDouble() * Math.PI * 2, d = radius * (0.9 + random.nextDouble() * 0.6);
      int x = (int) Math.floor(at.x + Math.cos(a) * d), z = (int) Math.floor(at.z + Math.sin(a) * d);
      BlockPos top = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
      if (Math.abs(top.getY() - at.y) > radius + 4) continue;
      if (sl.isEmptyBlock(top) && sl.getBlockState(top.below()).isFaceSturdy(sl, top.below(), net.minecraft.core.Direction.UP)) {
        sl.setBlock(top, Blocks.FIRE.defaultBlockState(), 3);
      }
    }
  }

  /**
   * Hidra desperta de estagio 5+: no covil, se ninguem a enfrenta ou se passa do tempo, cava ate a superficie; solta,
   * abre crateras em volta de tempos em tempos enquanto vagueia.
   */
  private void tickRampage() {
    if (!devastates(stage()) || !(level() instanceof ServerLevel sl)) return;
    LivingEntity t = getTarget();
    if (t == null || !t.isAlive()) idleTicks++;
    else idleTicks = 0;
    if (breakoutTicks > 0) {
      if (--breakoutTicks == 0) finishBreakout(sl);
      return;
    }
    if (inLair) {
      int limit = ArcaneConfig.HYDRA_BREAKOUT_SECONDS.get() * 20;
      if (awakeTicks > limit || idleTicks > limit / 3) startBreakout(sl);
      return;
    }
    if (--rampageCooldown > 0) return;
    rampageCooldown = rampagePeriod(stage()) + random.nextInt(40);
    float r = devastationRadius(stage());
    int n = cratersPerStrike(stage());
    for (int i = 0; i < n; i++) {
      double a = random.nextDouble() * Math.PI * 2, d = 10 + random.nextDouble() * 18 * Math.sqrt(reach(stage()));
      announceStrike(sl, position().add(Math.cos(a) * d, 0, Math.sin(a) * d), r * (0.7f + 0.3f * random.nextFloat()), STRIKE_WARNING + i * 8);
    }
  }

  /** Comeca a cavar: um poco do tamanho do corpo ate a superficie (pela fila de crateras). */
  public void startBreakout(ServerLevel sl) {
    if (!inLair || breakoutTicks > 0) return;
    breakoutTicks = 50;
    int surface = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
    if (griefing(sl)) {
      int w = Math.min(7, (int) Math.ceil(getBbWidth() / 2) + 2);
      LongArrayList list = new LongArrayList();
      BlockPos base = blockPosition();
      for (int y = base.getY(); y <= surface + 2; y++) for (int dx = -w; dx <= w; dx++) for (int dz = -w; dz <= w; dz++) {
        if (dx * dx + dz * dz > w * w) continue;
        BlockPos p = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
        var st = sl.getBlockState(p);
        if (st.isAir() || st.getDestroySpeed(sl, p) < 0 || st.hasBlockEntity()) continue;
        list.add(p.asLong());
      }
      com.arcane.execution.CraterQueue.submit(sl, list, position());
    }
    ModNetwork.sendShape(sl, S2CShapeFxPacket.ORIGINAL, position(), new Vec3(0, surface - getY(), 0), getBbWidth() + 2, 0xFF3020, S2CShapeFxPacket.FX_BREAKOUT, 60);
    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 6.0f, 0.5f);
  }

  private void finishBreakout(ServerLevel sl) {
    inLair = false;
    clearRestriction();
    int surface = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
    teleportTo(getX(), surface + 1, getZ());
    devastate(sl, position(), Math.max(4f, devastationRadius(stage()) * 0.8f));
    sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10.0f, 0.4f);
    for (ServerPlayer p : sl.players()) {
      p.displayClientMessage(Component.translatable("message.arcane.hydra.breakout", Component.translatable("hydra.arcane.stage." + stage()),
          getBlockX(), getBlockZ()).withStyle(ChatFormatting.DARK_RED), false);
    }
  }

  // ------------------------------------------------------------------ luta, chefe e morte
  @Override
  public boolean hurt(DamageSource source, float amount) {
    // gigante: nao sufoca dentro dos blocos (nem nas crateras que ela mesma abre) nem se espreme
    if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) || source.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING)) return false;
    if (sealed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
      // o selo absorve: so um brilho e um som de cristal
      if (level() instanceof ServerLevel sl && tickCount % 4 == 0) {
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + getBbHeight() * 0.5, getZ(), 12, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.2);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.5f, 0.6f);
      }
      return false;
    }
    if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
    return super.hurt(source, (float) (com.arcane.execution.Pierce.is(source) ? amount : amount / toughness));
  }

  @Override
  public boolean isPushable() { return !sealed() && super.isPushable(); }

  public Set<UUID> fighters() { return fighters; }

  @Override
  public void die(DamageSource source) {
    super.die(source);
    // os grimorios devorados voltam inteiros (e protegidos, como quando o dono morre)
    if (level() instanceof ServerLevel sl0) {
      for (var book : devoured) {
        var it = new net.minecraft.world.entity.item.ItemEntity(sl0, getX(), getY() + 1, getZ(), book.copy());
        it.setUnlimitedLifetime();
        it.setInvulnerable(true);
        it.setGlowingTag(true);
        sl0.addFreshEntity(it);
      }
      devoured.clear();
    }
    if (level() instanceof ServerLevel sl) com.arcane.world.HydraLairs.onHydraDeath(sl, this);
  }

  @Override
  public void startSeenByPlayer(ServerPlayer player) {
    super.startSeenByPlayer(player);
    bossBar.addPlayer(player);
  }

  @Override
  public void stopSeenByPlayer(ServerPlayer player) {
    super.stopSeenByPlayer(player);
    bossBar.removePlayer(player);
  }

  @Override
  public boolean removeWhenFarAway(double d) { return false; }

  @Override
  public boolean fireImmune() { return stage() >= 6; }

  @Override
  public boolean causeFallDamage(float distance, float mult, DamageSource source) { return false; }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("HydraStage", stage());
    tag.putInt("HydraLair", lairIndex);
    tag.putDouble("HydraTough", toughness);
    tag.putInt("HydraSeals", sealMask());
    tag.putLong("HydraHome", home().asLong());
    tag.putBoolean("HydraInLair", inLair);
    tag.putInt("HydraAwake", awakeTicks);
    var books = new net.minecraft.nbt.ListTag();
    for (var b : devoured) books.add(b.save(new CompoundTag()));
    tag.put("HydraDevoured", books);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.contains("HydraStage")) {
      float hp = getHealth();
      setStage(tag.getInt("HydraStage"));
      if (tag.contains("Health")) setHealth(hp);
    }
    lairIndex = tag.contains("HydraLair") ? tag.getInt("HydraLair") : -1;
    entityData.set(SEALS, tag.getInt("HydraSeals"));
    entityData.set(HOME, BlockPos.of(tag.getLong("HydraHome")));
    inLair = tag.getBoolean("HydraInLair");
    awakeTicks = tag.getInt("HydraAwake");
    devoured.clear();
    for (var t : tag.getList("HydraDevoured", net.minecraft.nbt.Tag.TAG_COMPOUND)) devoured.add(net.minecraft.world.item.ItemStack.of((CompoundTag) t));
    if (inLair) restrictTo(home(), com.arcane.world.HydraLairs.CHAMBER_R - 2);
    updateBossName();
  }

  /** Prende a Hidra a camara do covil (ate ela cavar para fora). */
  public void homeTo(BlockPos centre, int radius) {
    restrictTo(centre, radius);
    entityData.set(HOME, centre);
    inLair = true;
  }
}
