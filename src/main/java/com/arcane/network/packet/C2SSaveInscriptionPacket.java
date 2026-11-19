package com.arcane.network.packet;

import com.arcane.discovery.DiscoveryService;
import com.arcane.glyph.GlyphDefinition;
import com.arcane.glyph.GlyphRegistry;
import com.arcane.inscription.GlyphInstance;
import com.arcane.menu.ArcaneWorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Bancada -> servidor: salva a composicao no grimorio. O servidor valida e limita todos os valores. */
public record C2SSaveInscriptionPacket(String name, List<Entry> glyphs, String ink) {
  public C2SSaveInscriptionPacket(String name, List<Entry> glyphs) { this(name, glyphs, ""); }

  public static final int MAX_GLYPHS = 24;
  private static final int MAX_PARAMS = 8;

  public record Entry(String id, Map<String, Double> params) {}

  public static void encode(C2SSaveInscriptionPacket packet, FriendlyByteBuf buf) {
    buf.writeUtf(packet.name, 64);
    buf.writeUtf(packet.ink, 32);
    buf.writeVarInt(packet.glyphs.size());
    for (Entry entry : packet.glyphs) {
      buf.writeUtf(entry.id, 128);
      buf.writeVarInt(entry.params.size());
      entry.params.forEach((key, value) -> {
        buf.writeUtf(key, 32);
        buf.writeDouble(value);
      });
    }
  }

  public static C2SSaveInscriptionPacket decode(FriendlyByteBuf buf) {
    String name = buf.readUtf(64);
    String ink = buf.readUtf(32);
    int count = Math.min(buf.readVarInt(), MAX_GLYPHS + 1);
    List<Entry> entries = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String id = buf.readUtf(128);
      int paramCount = Math.min(buf.readVarInt(), MAX_PARAMS);
      Map<String, Double> params = new HashMap<>();
      for (int p = 0; p < paramCount; p++) {
        params.put(buf.readUtf(32), buf.readDouble());
      }
      entries.add(new Entry(id, params));
    }
    return new C2SSaveInscriptionPacket(name, entries, ink);
  }

  public static void handle(C2SSaveInscriptionPacket packet, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer player = ctx.get().getSender();
      if (player == null || !(player.containerMenu instanceof ArcaneWorkbenchMenu menu)) return;
      if (packet.glyphs.size() > MAX_GLYPHS) return;

      List<ResourceLocation> used = new ArrayList<>();
      List<GlyphInstance> instances = new ArrayList<>();
      for (Entry entry : packet.glyphs) {
        ResourceLocation id = ResourceLocation.tryParse(entry.id);
        if (id == null) return;
        used.add(id);
        instances.add(new GlyphInstance(id, sanitize(id, entry.params)));
      }
      menu.clearGlyphs();
      instances.forEach(menu::addGlyph);
      String name = packet.name.length() > 32 ? packet.name.substring(0, 32) : packet.name;
      menu.setSpellName(name.trim());

      // Valida antes de salvar: composicoes invalidas contam como experimento, mas nao entram no grimorio.
      var draft = new com.arcane.inscription.Inscription(List.copyOf(menu.building()));
      var validation = com.arcane.inscription.InscriptionValidator.validate(draft,
          com.arcane.grimoire.GrimoireAttachments.get(player));
      if (menu.penMode()) {
        penInscribe(player, menu, validation, packet.ink);
        return;
      }
      var grimoire = com.arcane.grimoire.GrimoireAttachments.get(player);
      boolean noPages = validation.valid() && grimoire.map(g -> g.freePages() <= 0).orElse(false);
      if (noPages) {
        player.displayClientMessage(Component.translatable("message.arcane.workbench.no_pages").withStyle(ChatFormatting.RED), true);
      } else if (validation.valid() && menu.saveToGrimoire(player)) {
        player.displayClientMessage(Component.translatable(menu.mode() == ArcaneWorkbenchMenu.FIELD ? "message.arcane.workbench.saved_field" : "message.arcane.workbench.saved",
            menu.spellName().isBlank() ? Component.translatable("gui.arcane.unnamed") : Component.literal(menu.spellName()))
            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        player.level().playSound(null, player.blockPosition(), com.arcane.registry.ModSounds.WORKBENCH_SAVE.get(),
            net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.0f);
      } else {
        player.displayClientMessage(Component.translatable("message.arcane.workbench.invalid", validation.message())
            .withStyle(ChatFormatting.RED), true);
      }
      DiscoveryService.onExperiment(player, used);
    });
    ctx.get().setPacketHandled(true);
  }

  /** Modos caneta: a tinta e escolhida na interface e tem de estar no inventario; gasta 1 (menos no criativo). */
  private static void penInscribe(ServerPlayer player, ArcaneWorkbenchMenu menu, com.arcane.inscription.InscriptionValidator.ValidationResult validation, String inkChoice) {
    if (!validation.valid()) {
      player.displayClientMessage(Component.translatable("message.arcane.workbench.invalid", validation.message()).withStyle(ChatFormatting.RED), true);
      return;
    }
    net.minecraft.world.item.ItemStack ink = findInk(player, inkChoice);
    if (ink == null) {
      player.displayClientMessage(Component.translatable("message.arcane.pen.pick_ink").withStyle(ChatFormatting.RED), true);
      return;
    }
    ResourceLocation inkId = new ResourceLocation("arcane", ((com.arcane.item.InkItem) ink.getItem()).inkId());
    if (menu.mode() == ArcaneWorkbenchMenu.ITEM) {
      var held = player.getOffhandItem();
      if (held.isEmpty() || held.getItem() instanceof com.arcane.item.InkItem || held.is(com.arcane.registry.ModItems.ARCANE_PEN.get())) {
        player.displayClientMessage(Component.translatable("message.arcane.pen.need_item").withStyle(ChatFormatting.RED), true);
        return;
      }
      var mainPen = player.getMainHandItem().getItem();
      boolean scroll = held.is(net.minecraft.world.item.Items.PAPER);
      if (mainPen instanceof com.arcane.item.ObjectPenItem objPen) {
        if (!objPen.canWriteOn(held)) {
          player.displayClientMessage(Component.translatable("message.arcane.pen.wrong_item").withStyle(ChatFormatting.RED), true);
          return;
        }
      } else if (!scroll && com.arcane.inscribing.ItemDrawings.kindOf(held) != com.arcane.inscribing.ItemDrawings.Kind.OTHER) {
        // Comida, arma e pocao pedem a caneta propria; a Pena Arcana escreve em papel e nos demais objetos.
        player.displayClientMessage(Component.translatable("message.arcane.pen.need_object_pen").withStyle(ChatFormatting.RED), true);
        return;
      }
      var inscription = new com.arcane.inscription.Inscription(List.copyOf(menu.building()), new com.arcane.inscription.InscriptionMetadata(
          menu.spellName(), inkId, new ResourceLocation("arcane", "arcane_pen"), com.arcane.execution.PrecisionSnapshot.FIELD_POWER,
          java.util.Optional.of(player.getUUID()), player.level().getGameTime(), com.arcane.inscription.InscriptionSourceKind.ITEM));
      var written = held.copyWithCount(1);
      if (scroll) {
        com.arcane.inscribing.ItemInscriptionHelper.write(written, inscription);
        written.setHoverName(menu.spellName().isEmpty() ? Component.translatable("gui.arcane.unnamed")
            : Component.literal(menu.spellName()).withStyle(ChatFormatting.LIGHT_PURPLE));
      } else {
        if (!com.arcane.inscribing.ItemDrawings.canAdd(written)) {
          player.displayClientMessage(Component.translatable("message.arcane.drawing.full", com.arcane.inscribing.ItemDrawings.maxCount(written)).withStyle(ChatFormatting.RED), true);
          return;
        }
        com.arcane.inscribing.ItemDrawings.add(written, inscription);
      }
      if (!player.getAbilities().instabuild) ink.shrink(1);
      held.shrink(1);
      if (held.isEmpty()) player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, written);
      else if (!player.getInventory().add(written)) player.drop(written, false);
      int excess = scroll ? 0 : com.arcane.inscribing.ItemDrawings.excess(written);
      player.displayClientMessage(Component.translatable(excess > 0 ? "message.arcane.drawing.unstable" : "message.arcane.pen.written.item").withStyle(excess > 0 ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE), true);
      player.closeContainer();
      return;
    }
    var level = (net.minecraft.server.level.ServerLevel) player.level();
    var pos = menu.targetPos();
    String trigger = level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
        ? com.arcane.inscribing.InscribingService.TRIGGER_ON_OPEN : com.arcane.inscribing.InscribingService.TRIGGER_ON_TOUCH;
    if (menu.building().stream().anyMatch(g -> g.glyphId().getPath().equals("on_signal"))) {
      trigger = com.arcane.inscribing.InscribingService.TRIGGER_ON_SIGNAL; // Ativar por Sinal troca o gatilho
    } else if (menu.building().stream().anyMatch(g -> g.glyphId().getPath().equals("on_near"))) {
      trigger = com.arcane.inscribing.InscribingService.TRIGGER_ON_NEAR; // Ativar por Proximidade
    }
    if (com.arcane.inscribing.InscribingService.inscribe(level, player, pos, menu.building(), inkId, trigger, menu.spellName(), menu.face())) {
      if (!player.getAbilities().instabuild) ink.shrink(1);
      player.displayClientMessage(Component.translatable("message.arcane.pen.written." + trigger).withStyle(ChatFormatting.LIGHT_PURPLE), true);
      player.closeContainer();
    }
  }

  /** Procura no inventario uma tinta do tipo escolhido (vazio = nenhuma escolhida = recusa). */
  public static net.minecraft.world.item.ItemStack findInk(ServerPlayer player, String inkChoice) {
    if (inkChoice == null || inkChoice.isEmpty()) return null;
    var inv = player.getInventory();
    for (int i = 0; i < inv.getContainerSize(); i++) {
      var s = inv.getItem(i);
      if (s.getItem() instanceof com.arcane.item.InkItem k && k.inkId().equals(inkChoice) && !s.isEmpty()) return s;
    }
    return null;
  }

  /** So parametros que o glifo declara, limitados ao intervalo dele; valores iguais ao padrao nem sao guardados. */
  static Map<String, Double> sanitize(ResourceLocation id, Map<String, Double> given) {
    GlyphDefinition def = GlyphRegistry.get(id).orElse(null);
    if (def == null) return Map.of();
    Map<String, Double> clean = new HashMap<>();
    for (Map.Entry<String, Double> e : given.entrySet()) {
      if (!def.parameters().containsKey(e.getKey())) continue;
      double value = def.resolveParam(e.getKey(), Map.of(e.getKey(), e.getValue()));
      if (Math.abs(value - def.parameters().get(e.getKey()).defaultValue()) > 1e-9) {
        clean.put(e.getKey(), value);
      }
    }
    return Map.copyOf(clean);
  }
}
