package com.militopia.components;

import com.badlogic.ashley.core.Component;

/** Marker component that identifies an entity as a wild animal. */
public class AnimalComponent implements Component {

    /** The animal type name, e.g. "HORSE", "DEER". Matches {@link com.militopia.config.AnimalType} keys. */
    public final String animalType;

    public AnimalComponent(String animalType) {
        this.animalType = animalType;
    }
}
