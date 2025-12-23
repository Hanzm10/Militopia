package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class TypeComponent implements Component {
    public enum Type { BASE, UNIT, TERRAIN, MARKER }
    public Type type;

    public TypeComponent(Type type) {
        this.type = type;
    }
}