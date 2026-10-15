package com.arcane.glyph;

import com.arcane.ArcaneMod;
import net.minecraft.resources.ResourceLocation;

public final class ArcaneIds {
    public static final ResourceLocation SELF = of("self");
    public static final ResourceLocation TOUCH = of("touch");
    public static final ResourceLocation PROJECTILE = of("projectile");

    public static final ResourceLocation DAMAGE = of("damage");
    public static final ResourceLocation FIRE = of("fire");
    public static final ResourceLocation PUSH = of("push");
    public static final ResourceLocation HEAL = of("heal");

    public static final ResourceLocation HOSTILE = of("hostile");
    public static final ResourceLocation SELF_FILTER = of("self_filter");
    public static final ResourceLocation BLOCKS = of("blocks");

    public static final ResourceLocation ON_HIT_ENTITY = of("on_hit_entity");
    public static final ResourceLocation AFTER_SECONDS = of("after_seconds");

    public static final ResourceLocation ACTIVATE = of("activate");

    private ArcaneIds() {}

    public static ResourceLocation of(String path) {
        return new ResourceLocation(ArcaneMod.MODID, path);
    }
}
