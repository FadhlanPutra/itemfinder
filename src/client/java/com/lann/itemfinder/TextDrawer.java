package com.lann.itemfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class TextDrawer {
    private TextDrawer() {}

    private static int forceOpaque(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }

    public static void drawString(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, text, x, y, forceOpaque(color));
    }

    public static void drawString(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, text, x, y, forceOpaque(color));
    }

    public static void drawCenteredString(GuiGraphicsExtractor g, String text, int cx, int y, int color) {
        g.centeredText(Minecraft.getInstance().font, text, cx, y, forceOpaque(color));
    }

    public static void drawCenteredString(GuiGraphicsExtractor g, Component text, int cx, int y, int color) {
        g.centeredText(Minecraft.getInstance().font, text, cx, y, forceOpaque(color));
    }
}