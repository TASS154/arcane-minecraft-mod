package com.arcane.glyph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class GlyphRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, GlyphDefinition> GLYPHS = new HashMap<>();

    private GlyphRegistry() {}

    public static PreparableReloadListener reloadListener() {
        return new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                    ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                    Executor backgroundExecutor, Executor gameExecutor) {
                return CompletableFuture.supplyAsync(() -> load(manager), backgroundExecutor)
                        .thenCompose(barrier::wait)
                        .thenAcceptAsync(map -> {
                            GLYPHS.clear();
                            GLYPHS.putAll(map);
                            com.arcane.inscription.CompiledPlanCache.clear();
                            LOGGER.info("Loaded {} glyph definitions.", GLYPHS.size());
                        }, gameExecutor);
            }

            @Override
            public String getName() {
                return "arcane-glyphs";
            }
        };
    }

    private static Map<ResourceLocation, GlyphDefinition> load(ResourceManager manager) {
        Map<ResourceLocation, GlyphDefinition> loaded = new HashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources("glyphs", rl -> rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            ResourceLocation glyphId = new ResourceLocation(fileId.getNamespace(),
                    fileId.getPath().replace("glyphs/", "").replace(".json", ""));

            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                json.addProperty("id", glyphId.toString());
                GlyphDefinition def = GlyphDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                        .getOrThrow(false, msg -> new IllegalStateException("Failed to parse glyph " + glyphId + ": " + msg));
                loaded.put(glyphId, def);
            } catch (IOException | RuntimeException ex) {
                LOGGER.error("Invalid glyph definition {}: {}", glyphId, ex.getMessage());
            }
        }
        return loaded;
    }

    public static Optional<GlyphDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(GLYPHS.get(id));
    }

    public static GlyphDefinition getOrThrow(ResourceLocation id) {
        return get(id).orElseThrow(() -> new IllegalArgumentException("Unknown glyph: " + id));
    }

    public static Map<ResourceLocation, GlyphDefinition> all() {
        return Collections.unmodifiableMap(GLYPHS);
    }

    public static int size() {
        return GLYPHS.size();
    }
}
