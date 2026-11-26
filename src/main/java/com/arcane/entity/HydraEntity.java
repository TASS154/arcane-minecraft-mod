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
  static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Poder sendo usado agora (0 = nenhum), para o modelo abrir as bocas / erguer as cabecas. */
  static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Selos ainda inteiros: um bit por pilar da camara (0 = desperta). */
  static final EntityDataAccessor<Integer> SEALS = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Centro da camara (para desenhar as correntes ate os selos). */
  static final EntityDataAccessor<BlockPos> HOME = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.BLOCK_POS);
  static final UUID HEALTH_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a01");
  static final UUID DAMAGE_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a02");

  public static final int BREATH = 1, SWEEP = 2, ROAR = 3, STORM = 4, METEOR = 5, CATACLYSM = 6, DEVASTATE = 7;
  /** A partir deste estagio a Hidra destroi o terreno. */
  public static final int DEVASTATION_STAGE = 5;
  /** Pilares da camara (os selos ficam no alto deles): 8, a {@link #PILLAR_RADIUS} do centro, topo a 6 blocos do chao. */
  public static final int PILLARS = 8, PILLAR_RADIUS = 11, PILLAR_TOP = 6;
  /** Tempo entre o aviso (circulo vermelho) e a queda de um golpe de devastacao. */
  public static final int STRIKE_WARNING = 30;

  private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.arcane.hydra"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
  final Set<UUID> fighters = new HashSet<>();
  private int lairIndex = -1;
  int powerCooldown = 60;
  int powerTicks;
  private double playerFactor = 1.0;
  /** Grimorios que a Hidra devorou (itens inteiros, devolvidos ao morrer); as magias deles viram poderes dela. */
  final java.util.List<net.minecraft.world.item.ItemStack> devoured = new java.util.ArrayList<>();
  static final UUID SPEED_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a03");
  public static final int DEVOURED_SPELL = 8;

  // ------------------------------------------------------------------ cabecas independentes
  /** Alvo de cada cabeca (id da entidade no cliente, -1 = nenhum): cada cabeca olha e morde o seu. */
  @SuppressWarnings("unchecked")
  static final EntityDataAccessor<Integer>[] HEAD_TARGET = new EntityDataAccessor[9];
  /** Cabecas mordendo agora (bit i = cabeca i), para a animacao do bote. */
  static final EntityDataAccessor<Integer> BITING = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  static {
    for (int i = 0; i < 9; i++) HEAD_TARGET[i] = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  }
  private final int[] headCooldown = new int[9];
  private final int[] biteTicks = new int[9];
  private final int[] spellCooldown = new int[9];

  /** Cabecas vivas (bit i): cada limiar de vida arranca uma cabeca ao acaso (fases da luta). */
  static final EntityDataAccessor<Integer> ALIVE = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Estados para a animacao: furia, fraqueza, despertando, morrendo, debatendo-se (selada). */
  static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Tick em que comecou a animacao longa atual (despertar). */
  static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Glifo original da Hidra (0..6, a ordem de HydraLairs.ORIGINALS); muda a cor, detalhes do modelo e a morte. */
  static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  /** Gesto ocioso atual (rosnar, rugir, bater a cauda...) e quando comecou: so para a animacao e os sons. */
  static final EntityDataAccessor<Integer> IDLE = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  static final EntityDataAccessor<Integer> IDLE_START = SynchedEntityData.defineId(HydraEntity.class, EntityDataSerializers.INT);
  public static final int IDLE_GROWL = 1, IDLE_ROAR = 2, IDLE_TAIL = 3, IDLE_SHAKE = 4, IDLE_SNIFF = 5, IDLE_YAWN = 6, IDLE_SNORE = 7, IDLE_TICKS = 40;
  public static final int F_BERSERK = 1, F_WEAK = 2, F_AWAKEN = 4, F_DYING = 8, F_STRAIN = 16;
  public static final int HEAL = 9, ORIGINAL = 10, AWAKEN_TICKS = 70, DEATH_TICKS = 110;
  public static final int ORIGINAL_COOLDOWN = 20 * 180;
  static final UUID ENRAGE_ID = UUID.fromString("6b1c33a1-6f0a-4c39-9c51-1f2d8d4e6a04");
  int weakTicks, originalCooldown = 600, strainTicks;
  final java.util.ArrayDeque<Float> healthHistory = new java.util.ArrayDeque<>();
  /** Fotos dos jogadores durante a luta (so a Hidra do Retroceder usa): posicao, vida, fome e equipamento. */
  record Snapshot(Vec3 pos, float health, int food, java.util.List<net.minecraft.world.item.ItemStack> gear) {}
  final java.util.Map<UUID, java.util.List<Snapshot>> snapshots = new java.util.HashMap<>();
  /**
   * A vida do Minecraft tem teto de 1024. Acima disso a Hidra fica "mais dura": o dano recebido e dividido por este
   * fator, entao a vida efetiva e vida x dureza (a barra de chefe mostra a fracao, que e a mesma).
   */
  double toughness = 1.0;
  /** Dentro do covil (presa ao raio da camara) ou ja solta na superficie. */
  private boolean inLair;
  private int awakeTicks, idleTicks, breakoutTicks = -1, rampageCooldown = 100;

  /** Golpe de devastacao anunciado (circulo vermelho no chao) esperando a hora de cair. */
  static final class Strike {
    final Vec3 at;
    final float radius;
    int ticks;

    Strike(Vec3 at, float radius, int ticks) {
      this.at = at;
      this.radius = radius;
      this.ticks = ticks;
    }
  }

  final List<Strike> strikes = new ArrayList<>();

  /** Usa um poder agora (testes e comandos): ver {@link HydraPowers#use}. */
  public void use(int choice, LivingEntity target) { HydraPowers.use(this, choice, target); }

  /** Sorteio da entidade, para as classes de poderes, fases e morte (random e protegido no Entity). */
  net.minecraft.util.RandomSource rng() { return random; }

  /** Dados sincronizados (entityData e protegido no Entity). */
  net.minecraft.network.syncher.SynchedEntityData data() { return entityData; }

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
    entityData.define(ALIVE, 1);
    entityData.define(FLAGS, 0);
    entityData.define(ANIM_START, 0);
    entityData.define(VARIANT, -1);
    entityData.define(IDLE, 0);
    entityData.define(IDLE_START, 0);
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

  public void setLairIndex(int i) { this.lairIndex = i; if (i >= 0) setVariant(i % 7); }

  private static int clampStage(int s) { return Math.max(1, Math.min(MAX_STAGE, s)); }

  /** Cabecas visiveis: 1 no estagio 1, 9 no 14. */
  public static int headsFor(int stage) { return 1 + Math.round((clampStage(stage) - 1) * 8f / 13f); }

  public int heads() { return headsFor(stage()); }

  /** Escala do corpo: 1 no estagio 1, 4,25 no 14. */
  /** Tamanho: 1,8x na recem-nascida ate 7x na ancia (antes 1x a 4,25x: pequena demais, decisao de Everton). */
  public static float scaleFor(int stage) { return 1.8f + (clampStage(stage) - 1) * (14.2f / 13f); }

  /** A caixa de colisao para de crescer em 8x (o corpo visivel continua crescendo: a anciã e uma montanha). */
  public static float hitScaleFor(int stage) { return Math.min(8f, scaleFor(stage)); }

  /** Rapida quando filhote, lenta e pesada quando anciã. */
  public static double speedFor(int stage) { return 0.34 - 0.017 * (clampStage(stage) - 1); }

  /** Vida: 80 na recem-nascida, x1,55 a cada estagio (~525 no 5, ~4,6 mil no 10, ~24 mil na anciã). */
  public static double baseHealth(int stage) { return 80.0 * Math.pow(1.55, clampStage(stage) - 1); }

  /** Dano: 4 na recem-nascida, x1,28 a cada estagio (~11 no 5, ~37 no 10, ~100 na anciã). */
  public static double baseDamage(int stage) { return 4.0 * Math.pow(1.28, clampStage(stage) - 1); }

  /** Alcance dos poderes: x1,17 a cada estagio (1 na recem-nascida, ~1,9 no 5, ~7,7 na anciã). */
  public static double reach(int stage) { return Math.pow(1.21, clampStage(stage) - 1); }

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
  public static BlockPos pillarTop(int i) { return pillarTop(i, 1); }

  /** Topo do pilar i numa camara do estagio dado: a camara cresce com a Hidra, e os pilares se afastam e sobem junto. */
  public static BlockPos pillarTop(int i, int stage) {
    int r = com.arcane.world.HydraLairs.chamberRadius(stage);
    int radius = (int) Math.round(r * 0.78), height = PILLAR_TOP + (int) Math.round((r - com.arcane.world.HydraLairs.CHAMBER_R) * 0.4);
    double a = i * Math.PI * 2 / PILLARS;
    return new BlockPos((int) Math.round(Math.cos(a) * radius), height, (int) Math.round(Math.sin(a) * radius));
  }

  public void setStage(int stage) {
    int s = clampStage(stage);
    entityData.set(STAGE, s);
    applyHealth(baseHealth(s));
    getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage(s));
    getAttribute(Attributes.ARMOR).setBaseValue(Math.min(30, 2.0 * s));
    getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speedFor(s));
    entityData.set(ALIVE, (1 << headsFor(s)) - 1);
    setHealth(getMaxHealth());
    refreshDimensions();
    setMaxUpStep(Math.max(1.0f, hitScaleFor(s) * 0.7f)); // anda por cima de morros e buracos (o corpo e suavizado no cliente)
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
    return super.getDimensions(pose).scale(hitScaleFor(stage()));
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
    setFlag(F_AWAKEN, true);
    entityData.set(ANIM_START, tickCount);
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
    HydraPowers.use(this, ROAR, null);
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

  static void setModifier(AttributeInstance attr, UUID id, double amount) {
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

  public boolean headAlive(int i) { return (entityData.get(ALIVE) & (1 << i)) != 0; }

  public int aliveHeads() { return Integer.bitCount(entityData.get(ALIVE) & ((1 << heads()) - 1)); }

  public boolean flag(int f) { return (entityData.get(FLAGS) & f) != 0; }

  void setFlag(int f, boolean on) {
    int v = entityData.get(FLAGS);
    entityData.set(FLAGS, on ? v | f : v & ~f);
  }

  public int animStart() { return entityData.get(ANIM_START); }

  public int idleAction() { return entityData.get(IDLE); }

  public int idleStart() { return entityData.get(IDLE_START); }

  /**
   * Gestos ociosos para parecer viva: de vez em quando (sem poder em curso), rosna, ruge com uma cabeca, bate a cauda
   * no chao, sacode as cabecas, fareja o chao ou boceja. Selada, ronca. So som, particulas e animacao.
   */
  private void tickIdle(ServerLevel sl, boolean sleeping) {
    if (idleAction() != 0 && tickCount - idleStart() > IDLE_TICKS) entityData.set(IDLE, 0);
    if (idleAction() != 0 || tickCount % 20 != 0) return;
    if (!sleeping && (power() != 0 || flag(F_AWAKEN))) return;
    if (random.nextInt(sleeping ? 6 : 4) != 0) return;
    int a = sleeping ? IDLE_SNORE : 1 + random.nextInt(6);
    entityData.set(IDLE, a);
    entityData.set(IDLE_START, tickCount);
    float low = 0.4f + 0.6f / (1 + stage());
    switch (a) {
      case IDLE_GROWL -> sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.5f, low * 0.8f);
      case IDLE_ROAR -> sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, low + 0.2f);
      case IDLE_TAIL -> {
        double yaw = Math.toRadians(yBodyRot);
        double back = getBbWidth() * 1.4;
        double tx = getX() + Math.sin(yaw) * back, tz = getZ() - Math.cos(yaw) * back;
        sl.playSound(null, BlockPos.containing(tx, getY(), tz), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, sl.getBlockState(BlockPos.containing(tx, getY() - 1, tz))),
            tx, getY() + 0.2, tz, 40, getBbWidth() * 0.3, 0.2, getBbWidth() * 0.3, 0.2);
      }
      case IDLE_SHAKE -> sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2f, low);
      case IDLE_SNIFF -> sl.playSound(null, blockPosition(), SoundEvents.SNIFFER_SNIFFING, SoundSource.HOSTILE, 3f, low * 0.7f);
      case IDLE_YAWN -> sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_AMBIENT, SoundSource.HOSTILE, 2.5f, low * 0.6f);
      case IDLE_SNORE -> {
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.35f);
        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.4, getZ(), 15, getBbWidth() * 0.4, 0.2, getBbWidth() * 0.4, 0.02);
      }
      default -> { }
    }
  }

  public int variant() { return entityData.get(VARIANT); }

  public void setVariant(int v) { entityData.set(VARIANT, v); }

  /** O corpo visivel e maior que a caixa de colisao: o cliente nao pode deixar de desenha-la so porque a caixa saiu da tela. */
  @Override
  public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
    float extra = scaleFor(stage()) / hitScaleFor(stage());
    return getBoundingBox().inflate(getBbWidth() * extra * 1.5, getBbHeight() * extra * 2.5, getBbWidth() * extra * 1.5);
  }

  @Override
  public boolean shouldRenderAtSqrDistance(double d) { return d < 512 * 512; }

  /** Id da entidade que a cabeca i esta mirando (-1 = nenhuma). */
  public int headTarget(int i) { return i < 0 || i >= 9 ? -1 : entityData.get(HEAD_TARGET[i]); }

  public boolean headBiting(int i) { return (entityData.get(BITING) & (1 << i)) != 0; }

  /** Alcance do pescoco: ate onde uma cabeca morde sem o corpo andar. */
  public double neckReach() { return 2.0 + scaleFor(stage()) * 3.4; } // pescoco longo (15 gomos)

  /**
   * Cabecas independentes: a cada segundo, distribui os alvos por perto (jogadores e criaturas que a atacaram ou que
   * ela caca) entre as cabecas, uma por alvo, revezando quando ha mais cabecas que alvos. Cada cabeca morde o seu alvo
   * quando ele esta ao alcance do pescoco, com a sua propria espera; o dano de cada mordida e uma fracao do golpe.
   */
  private void tickHeads() {
    if (!(level() instanceof ServerLevel sl)) return;
    int n = heads();
    boolean berserk = flag(F_BERSERK);
    if (tickCount % 20 == 3) {
      // foco por ameaca: cada alvo ganha cabecas em proporcao a sua mana maxima (criaturas contam 20)
      java.util.List<LivingEntity> foes = new java.util.ArrayList<>(HydraPowers.enemiesWithin(this, 28 + scaleFor(stage()) * 3));
      foes.removeIf(e -> e instanceof Player p && (p.isCreative() || p.isSpectator()));
      foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(this)));
      if (getTarget() != null && getTarget().isAlive()) { foes.remove(getTarget()); foes.add(0, getTarget()); }
      java.util.List<Integer> alive = new java.util.ArrayList<>();
      for (int i = 0; i < n; i++) if (headAlive(i)) alive.add(i);
      // a ordem das cabecas: do meio para as pontas (o meio pega o alvo de maior ameaca)
      alive.sort(java.util.Comparator.comparingDouble(i -> Math.abs(i - (n - 1) / 2.0)));
      java.util.List<Integer> plan = new java.util.ArrayList<>();
      if (!foes.isEmpty()) {
        double total = 0;
        double[] w = new double[foes.size()];
        for (int k = 0; k < foes.size(); k++) { w[k] = threat(foes.get(k)); total += w[k]; }
        int[] share = new int[foes.size()];
        int given = 0;
        for (int k = 0; k < foes.size() && given < alive.size(); k++) { share[k] = Math.max(1, (int) Math.floor(alive.size() * w[k] / total)); given += share[k]; }
        for (int k = 0; plan.size() < alive.size(); k = (k + 1) % foes.size()) {
          if (share[k] > 0) { plan.add(foes.get(k).getId()); share[k]--; }
          else if (k == foes.size() - 1 && java.util.Arrays.stream(share).allMatch(v -> v <= 0)) plan.add(foes.get(0).getId());
        }
      }
      for (int i = 0; i < 9; i++) entityData.set(HEAD_TARGET[i], -1);
      for (int k = 0; k < alive.size() && k < plan.size(); k++) {
        int id = plan.get(k);
        if (berserk && !foes.isEmpty() && random.nextInt(3) == 0) id = foes.get(random.nextInt(foes.size())).getId(); // furia: mira sem criterio
        entityData.set(HEAD_TARGET[alive.get(k)], id);
      }
    }
    int biting = 0;
    double reach = neckReach();
    boolean weak = flag(F_WEAK) || flag(F_AWAKEN);
    // mordida: praticamente fatal (a punicao por chegar perto); na furia, ainda mais forte
    float bite = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * (berserk ? 1.8 : 1.2));
    for (int i = 0; i < n; i++) {
      if (!headAlive(i)) continue;
      if (biteTicks[i] > 0) { biteTicks[i]--; biting |= 1 << i; }
      if (headCooldown[i] > 0) headCooldown[i]--;
      if (spellCooldown[i] > 0) spellCooldown[i]--;
      if (weak) continue;
      var e = sl.getEntity(entityData.get(HEAD_TARGET[i]));
      if (!(e instanceof LivingEntity t) || !t.isAlive()) continue;
      double d = t.distanceTo(this);
      if (d <= reach + t.getBbWidth()) {
        if (headCooldown[i] > 0) continue;
        headCooldown[i] = 30 + random.nextInt(30);
        biteTicks[i] = 8;
        biting |= 1 << i;
        if (!berserk || random.nextFloat() < 0.75f) t.hurt(damageSources().mobAttack(this), bite);
        sl.playSound(null, t.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.2f, 0.5f + random.nextFloat() * 0.3f);
      } else if (spellCooldown[i] <= 0 && d < 40 + scaleFor(stage()) * 2) {
        headSpell(sl, i, t, berserk);
      }
    }
    entityData.set(BITING, biting);
  }

  /** Ameaca de um alvo: a mana maxima (jogadores) ou 20 (criaturas). */
  private static double threat(LivingEntity e) {
    if (e instanceof Player p) return com.arcane.grimoire.GrimoireAttachments.getMana(p).map(m -> (double) Math.max(20f, m.max())).orElse(20.0);
    return 20.0;
  }

  /** Onde fica (mais ou menos) a boca da cabeca i, para as magias sairem dela. */
  public Vec3 headPos(int i, Vec3 toward) {
    int n = heads();
    float spread = n == 1 ? 0f : (i / (float) (n - 1)) * 2f - 1f;
    double sc = scaleFor(stage());
    double yaw = Math.toRadians(yBodyRot);
    Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    Vec3 base = position().add(0, sc * 3.6, 0).add(fwd.scale(sc * 2.5)).add(side.scale(spread * sc * 0.9));
    Vec3 dir = toward.subtract(base);
    double len = Math.min(dir.length(), sc * 1.2);
    return dir.lengthSqr() < 1e-4 ? base : base.add(dir.normalize().scale(len));
  }

  /**
   * Cada cabeca tem 1 a 3 elementos e 1 a 3 formas (sorteados pela identidade da Hidra, fixos para sempre): de longe,
   * cospe um projetil ou um raio pela boca. Na furia a mira treme muito.
   */
  private void headSpell(ServerLevel sl, int i, LivingEntity t, boolean berserk) {
    java.util.Random r = new java.util.Random(getUUID().getLeastSignificantBits() * 31 + i);
    String[] elements = {"fire", "slow", "poison", "wither", "blind", "electrify"};
    String[] forms = {"projectile", "ray", "projectile"};
    int ne = 1 + r.nextInt(3), nf = 1 + r.nextInt(3);
    String element = elements[(r.nextInt(elements.length) + random.nextInt(ne)) % elements.length];
    String form = forms[(r.nextInt(forms.length) + random.nextInt(nf)) % forms.length];
    double dmg = Math.min(20, getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.3);
    java.util.List<com.arcane.inscription.GlyphInstance> glyphs = new java.util.ArrayList<>();
    glyphs.add(form.equals("ray") ? new com.arcane.inscription.GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "ray"), java.util.Map.of("range", 40.0))
        : new com.arcane.inscription.GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "projectile")));
    glyphs.add(new com.arcane.inscription.GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", "damage"), java.util.Map.of("amount", dmg)));
    glyphs.add(new com.arcane.inscription.GlyphInstance(new net.minecraft.resources.ResourceLocation("arcane", element)));
    Vec3 aim = t.getEyePosition();
    if (berserk) aim = aim.add(random.nextGaussian() * 4, random.nextGaussian() * 2, random.nextGaussian() * 4);
    Vec3 mouth = headPos(i, aim);
    Vec3 look = aim.subtract(mouth).normalize();
    com.arcane.execution.CastingService.castFrom(this, new com.arcane.inscription.Inscription(glyphs), sl, mouth, look);
    spellCooldown[i] = (berserk ? 50 : 80) + random.nextInt(60);
    biteTicks[i] = 6;
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
  void castDevoured(LivingEntity target) {
    var spells = devouredSpells();
    if (spells.isEmpty() || !(level() instanceof ServerLevel sl)) return;
    var spell = spells.get(random.nextInt(spells.size()));
    getLookControl().setLookAt(target, 180f, 90f);
    lookAt(target, 180f, 90f);
    com.arcane.execution.CastingService.tryCast(this, spell, sl);
    sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight(), getZ(), 30, getBbWidth() * 0.4, 0.5, getBbWidth() * 0.4, 0.1);
  }

  /** Vida alvo: o que cabe no atributo (ate 1024) vira vida; o resto vira dureza. */
  void applyHealth(double wanted) {
    double cap = 1024.0;
    getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(cap, wanted));
    toughness = Math.max(1.0, wanted / cap);
  }

  public double effectiveMaxHealth() { return getMaxHealth() * toughness; }

  // ------------------------------------------------------------------ tick
  @Override
  public void aiStep() {
    super.aiStep();
    if (level().isClientSide || isDeadOrDying()) return; // morrendo: so a morte em runas (tickDeath)
    if (tickCount % 100 == 1) updatePlayerFactor();
    if (tickCount % 20 == 7) devourNearby();
    if (tickCount % 100 == 51) thinkAboutTarget();
    bossBar.setProgress(getHealth() / getMaxHealth());
    if (sealed()) {
      // dormindo: so respira fumaca; nada de poderes. Se alguem chega muito perto, ela tenta se mexer, mas as
      // correntes a seguram com rigor (so a animacao e o som mudam).
      if (tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight() * 0.8, getZ(), 2, getBbWidth() * 0.3, 0.2, getBbWidth() * 0.3, 0.01);
        Player near = sl.getNearestPlayer(this, getBbWidth() + 6);
        if (near != null && !near.isSpectator()) {
          strainTicks = 40;
          if (random.nextInt(2) == 0) sl.playSound(null, blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 2f, 0.4f);
          sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.5f, 0.4f);
        }
      }
      if (strainTicks > 0) strainTicks--;
      setFlag(F_STRAIN, strainTicks > 0);
      if (level() instanceof ServerLevel sl2) tickIdle(sl2, true);
      return;
    }
    setFlag(F_STRAIN, false);
    awakeTicks++;
    if (flag(F_AWAKEN)) {
      // despertar: as cabecas se erguem uma a uma, cada uma rugindo para os alvos (sem atacar ainda)
      int t = tickCount - animStart();
      if (level() instanceof ServerLevel sl && t % 8 == 0 && t / 8 < heads()) {
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 0.5f + 0.06f * (t / 8));
      }
      if (t >= AWAKEN_TICKS) setFlag(F_AWAKEN, false);
      return;
    }
    if (weakTicks > 0 && --weakTicks == 0) setFlag(F_WEAK, false);
    if (originalCooldown > 0) originalCooldown--;
    if (tickCount % 20 == 0) {
      healthHistory.addLast(getHealth());
      while (healthHistory.size() > 30) healthHistory.pollFirst();
    }
    heavySteps();
    huntPlayers();
    if (level() instanceof ServerLevel sl3) tickIdle(sl3, false);
    if (variant() == 1 && tickCount % 100 == 0) recordSnapshots();
    tickHeads();
    tickStrikes();
    tickRampage();
    if (powerTicks > 0) {
      powerTicks--;
      if (power() == BREATH) HydraPowers.breathTick(this);
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
    if (variant() >= 0 && originalCooldown <= 0 && roll < 18) choice = ORIGINAL;
    else if (getHealth() < getMaxHealth() * 0.6f && roll < 26) choice = HEAL;
    else if (s >= 13 && roll < 12) choice = CATACLYSM;
    else if (devastates(s) && roll < 30) choice = DEVASTATE;
    else if (s >= 10 && roll < 45) choice = METEOR;
    else if (s >= 7 && roll < 58) choice = STORM;
    else if (s >= 4 && roll < 66) choice = ROAR;
    else if (s >= 3 && d < sweepRadius(s) + 1) choice = SWEEP;
    else choice = BREATH;
    HydraPowers.use(this, choice, target);
    // magias da Hidra inteira (metade das cabecas para o ceu): ela fica fraca enquanto conjura
    if (choice == METEOR || choice == STORM || choice == CATACLYSM || choice == DEVASTATE || choice == HEAL) markWeak(powerTicks + 20);
  }

  void markWeak(int ticks) {
    weakTicks = Math.max(weakTicks, ticks);
    setFlag(F_WEAK, true);
  }

  static double sweepRadius(int s) { return Math.min(20.0, 4.0 * Math.pow(reach(s), 0.8)); }

  // ------------------------------------------------------------------ poderes




  // ------------------------------------------------------------------ devastacao
  /** A Hidra pode destruir o terreno? (config + regra mobGriefing). */
  public static boolean griefing(ServerLevel sl) {
    return ArcaneConfig.HYDRA_DEVASTATION.get() && sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
  }

  void announceStrike(ServerLevel sl, Vec3 at, float radius, int ticks) {
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
    for (LivingEntity e : HydraPowers.enemiesNear(this, at, radius * 1.3)) {
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
  /** Casca (o selo; so o /kill atravessa) e janela de fraqueza (x2), pelo portao generico (Fase N, mesmo comportamento). */
  static final PhaseGate<HydraEntity> GATE = new PhaseGate<>(HydraEntity::sealed,
      s -> s.is(DamageTypeTags.BYPASSES_INVULNERABILITY), h -> h.flag(F_WEAK), 2f);

  @Override
  public boolean hurt(DamageSource source, float amount) {
    // gigante: nao sufoca dentro dos blocos (nem nas crateras que ela mesma abre) nem se espreme
    if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) || source.is(net.minecraft.world.damagesource.DamageTypes.CRAMMING)) return false;
    if (GATE.blocks(this, source)) {
      // o selo absorve: so um brilho e um som de cristal
      if (level() instanceof ServerLevel sl && tickCount % 4 == 0) {
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + getBbHeight() * 0.5, getZ(), 12, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.2);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.5f, 0.6f);
      }
      return false;
    }
    if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
    if (flag(F_DYING)) return false;
    amount = GATE.scale(this, amount); // momento de fraqueza (x2): depois de uma magia grande ou da magia original
    boolean hurt = super.hurt(source, (float) (com.arcane.execution.Pierce.is(source) ? amount : amount / toughness));
    if (hurt && !level().isClientSide && isAlive()) HydraPhases.checkPhases(this);
    return hurt;
  }


  @Override
  public boolean isPushable() { return !sealed() && super.isPushable(); }

  public Set<UUID> fighters() { return fighters; }

  @Override
  public void die(DamageSource source) {
    super.die(source);
    setFlag(F_DYING, true);
    // a barra de chefe some na hora da morte (antes ficava os 5,5 s da morte em runas: com outra Hidra por perto, duas barras)
    bossBar.setProgress(0f);
    bossBar.removeAllPlayers();
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
    if (!isDeadOrDying()) bossBar.addPlayer(player); // morrendo: sem barra
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
    tag.putInt("HydraAlive", entityData.get(ALIVE));
    tag.putInt("HydraVariant", variant());
    tag.putInt("HydraOriginalCd", originalCooldown);
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
    if (tag.contains("HydraAlive")) entityData.set(ALIVE, tag.getInt("HydraAlive"));
    if (tag.contains("HydraVariant")) setVariant(tag.getInt("HydraVariant"));
    originalCooldown = tag.getInt("HydraOriginalCd");
    devoured.clear();
    for (var t : tag.getList("HydraDevoured", net.minecraft.nbt.Tag.TAG_COMPOUND)) devoured.add(net.minecraft.world.item.ItemStack.of((CompoundTag) t));
    if (inLair) restrictTo(home(), com.arcane.world.HydraLairs.chamberRadius(stage()) - 2);
    updateBossName();
  }

  /** Prende a Hidra a camara do covil (ate ela cavar para fora). */
  public void homeTo(BlockPos centre, int radius) {
    restrictTo(centre, radius);
    entityData.set(HOME, centre);
    inLair = true;
  }

  // ------------------------------------------------------------------ magia original, passos, caca, morte

  /** Passos pesados (estagio 8+): andando, esmaga plantas, neve e folhas e aplaina saliencias de 1 bloco sob os pes. */
  private void heavySteps() {
    if (stage() < 8 || tickCount % 10 != 0 || getDeltaMovement().horizontalDistanceSqr() < 1e-4 || !(level() instanceof ServerLevel sl)) return;
    int r = (int) Math.ceil(getBbWidth() / 2);
    BlockPos feet = blockPosition();
    boolean grief = griefing(sl);
    for (BlockPos q : BlockPos.betweenClosed(feet.offset(-r, 0, -r), feet.offset(r, 1, r))) {
      var st = sl.getBlockState(q);
      if (st.isAir()) continue;
      if (st.canBeReplaced() || st.is(net.minecraft.tags.BlockTags.LEAVES) || st.is(net.minecraft.tags.BlockTags.FLOWERS) || st.is(net.minecraft.world.level.block.Blocks.SNOW)) {
        sl.destroyBlock(q, false);
      } else if (grief && q.getY() == feet.getY() && sl.getBlockState(q.above()).isAir() && random.nextInt(3) == 0) {
        com.arcane.restore.BlockJournal.destroy(sl, q.immutable(), this); // aplaina o terreno
      }
    }
    sl.playSound(null, feet, SoundEvents.WARDEN_STEP, SoundSource.HOSTILE, 3f, 0.4f);
    sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, sl.getBlockState(feet.below())), getX(), getY() + 0.2, getZ(), 30, getBbWidth() * 0.4, 0.1, getBbWidth() * 0.4, 0.1);
  }

  /** Fora do covil ela caca jogadores: escolhe o mais proximo num raio grande e vai atras. */
  private void huntPlayers() {
    if (inLair || tickCount % 40 != 0 || (getTarget() != null && getTarget().isAlive()) || !(level() instanceof ServerLevel sl)) return;
    Player prey = sl.getNearestPlayer(getX(), getY(), getZ(), 160, p -> !((Player) p).isCreative() && !p.isSpectator());
    if (prey != null) setTarget(prey);
  }

  /** Hidra do Retroceder: guarda fotos dos jogadores da luta (a cada 5 s) para a morte devolve-los a uma delas. */
  private void recordSnapshots() {
    if (!(level() instanceof ServerLevel sl)) return;
    for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(64))) {
      java.util.List<net.minecraft.world.item.ItemStack> gear = new java.util.ArrayList<>();
      for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) gear.add(p.getItemBySlot(slot).copy());
      var list = snapshots.computeIfAbsent(p.getUUID(), k -> new java.util.ArrayList<>());
      list.add(new Snapshot(p.position(), p.getHealth(), p.getFoodData().getFoodLevel(), gear));
      if (list.size() > 120) list.remove(0);
    }
  }

  /** Morte: o corpo se desfaz em runas, devagar e depois cada vez mais rapido; no fim, o efeito do seu glifo. */
  @Override
  protected void tickDeath() {
    ++deathTime;
    if (level() instanceof ServerLevel sl) {
      float f = deathTime / (float) DEATH_TICKS;
      int n = (int) (2 + 60 * f * f);
      sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + getBbHeight() * 0.6, getZ(), n, getBbWidth() * 0.5, getBbHeight() * 0.5, getBbWidth() * 0.5, 0.6 + f * 2);
      sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.6, getZ(), n / 3, getBbWidth() * 0.4, getBbHeight() * 0.4, getBbWidth() * 0.4, 0.05 + f * 0.3);
      if (deathTime % Math.max(2, 20 - deathTime / 6) == 0) sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 3f, 0.5f + f);
      if (deathTime == DEATH_TICKS - 10) HydraDeath.deathEffect(this, sl);
    }
    if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
      level().broadcastEntityEvent(this, (byte) 60);
      remove(RemovalReason.KILLED);
    }
  }


}
