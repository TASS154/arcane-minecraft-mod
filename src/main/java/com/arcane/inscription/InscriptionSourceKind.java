package com.arcane.inscription;

import com.mojang.serialization.Codec;

public enum InscriptionSourceKind implements net.minecraft.util.StringRepresentable {
    GRIMOIRE("grimoire"),
    WORKBENCH("workbench"),
    WORLD("world"),
    ITEM("item"),
    SCROLL("scroll");

    public static final Codec<InscriptionSourceKind> CODEC = net.minecraft.util.StringRepresentable.fromEnum(InscriptionSourceKind::values);

    private final String name;

    InscriptionSourceKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
