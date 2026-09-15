package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyMapping OPEN_SEARCH;

    public static void register() {
        OPEN_SEARCH = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.itemfinder.open_search",
            GLFW.GLFW_KEY_Y,
            Category.MISC
        ));
    }
}
