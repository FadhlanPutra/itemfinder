package com.lann.itemfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TextDrawer {
    private TextDrawer() {}

    private static int forceOpaque(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }

    public static void drawString(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, text, x, y, forceOpaque(color));
    }

    public static void drawString(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, text, x, y, forceOpaque(color));
    }

    public static void drawCenteredString(GuiGraphics g, String text, int cx, int y, int color) {
        g.drawCenteredString(Minecraft.getInstance().font, text, cx, y, forceOpaque(color));
    }

    public static void drawCenteredString(GuiGraphics g, Component text, int cx, int y, int color) {
        g.drawCenteredString(Minecraft.getInstance().font, text, cx, y, forceOpaque(color));
    }
}