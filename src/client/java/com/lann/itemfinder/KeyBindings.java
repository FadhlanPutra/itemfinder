package com.lann.itemfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;

public class KeyBindings {
    public static KeyMapping OPEN_SEARCH;

    public static void register() {
        OPEN_SEARCH = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.itemfinder.open_search",
            InputConstants.KEY_Y,
            Category.MISC
        ));
    }
}
