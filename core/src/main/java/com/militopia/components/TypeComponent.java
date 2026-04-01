package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class TypeComponent implements Component {
    // Add OBJECT to the list
    public enum Type {
        BASE,
        UNIT,
        TERRAIN,
        MARKER,
        ATTACK_MARKER,
        TRANSFORM_MARKER,
        OBJECT
    }

    public Type type;

    public TypeComponent(Type type) {
        this.type = type;
    }
}