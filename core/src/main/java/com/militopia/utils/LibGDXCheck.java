package com.militopia.utils;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import java.lang.reflect.Method;

public class LibGDXCheck {
    public static void main(String[] args) {
        System.out.println("Methods in ScrollPane:");
        for (Method m : ScrollPane.class.getDeclaredMethods()) {
            System.out.println("  " + m.getName());
        }
    }
}
