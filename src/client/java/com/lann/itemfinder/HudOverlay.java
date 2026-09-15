package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.client.DeltaTracker;

import java.util.Map;

public class HudOverlay {

    public static void register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(ItemFinderMod.MOD_ID, "hud_overlay"),
            HudOverlay::renderHud
        );
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (HighlightRenderer.highlightedPositions.isEmpty()) return;
        if (!HighlightRenderer.isActive()) return;

        int x = client.getWindow().getGuiScaledWidth() - 160;
        int y = 10;

        graphics.fill(x - 4, y - 2, x + 156,
            y + HighlightRenderer.highlightedPositions.size() * 11 + 14, 0xAA000000);

        String mode = ServerDetector.serverHasMod() ? "§aServer" : "§eCache";
        TextDrawer.drawString(graphics, "§6Item Finder: " + mode, x, y, 0xFFFFFF);
        y += 11;

        BlockPos playerPos = client.player.blockPosition();
        for (Map.Entry<BlockPos, String> entry : HighlightRenderer.highlightedPositions.entrySet()) {
            BlockPos pos = entry.getKey();
            String label = entry.getValue();
            int dist = (int) Math.sqrt(playerPos.distSqr(pos));
            TextDrawer.drawString(graphics, "§e" + label + " §7" + dist + "m", x, y, 0xFFFFFF);
            y += 11;
        }
    }
}