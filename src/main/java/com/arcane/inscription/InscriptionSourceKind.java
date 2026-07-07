package com.arcane.inscription;

import com.mojang.serialization.Codec;

public enum InscriptionSourceKind implements com.mojang.serialization.StringRepresentable {
    GRIMOIRE("grimoire"),
    WORKBENCH("workbench"),
    WORLD("world"),
    ITEM("item"),
    SCROLL("scroll");

    public static final Codec<InscriptionSourceKind> CODEC = com.mojang.serialization.StringRepresentable.fromEnum(InscriptionSourceKind::values);

    private final String name;

    InscriptionSourceKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
