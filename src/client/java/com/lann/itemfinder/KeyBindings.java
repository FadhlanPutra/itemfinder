package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyMapping OPEN_SEARCH;

    public static void register() {
        OPEN_SEARCH = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.itemfinder.open_search",
            GLFW.GLFW_KEY_Y,
            "category.itemfinder"
        ));
    }
}
