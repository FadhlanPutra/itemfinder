package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Map;

public class HudOverlay {

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            if (HighlightRenderer.highlightedPositions.isEmpty()) return;
            if (!HighlightRenderer.isActive()) return;

            int x = client.getWindow().getGuiScaledWidth() - 160;
            int y = 10;

            graphics.fill(x - 4, y - 2, x + 156,
                y + HighlightRenderer.highlightedPositions.size() * 11 + 14, 0xAA000000);
            graphics.drawString(client.font, "§6Item Finder:", x, y, 0xFFFFFF);
            String mode = ServerDetector.serverHasMod() ? "§aServer" : "§eCache";
            graphics.drawString(client.font, "§6Item Finder: " + mode, x, y, 0xFFFFFF);
            y += 11;

            BlockPos playerPos = client.player.blockPosition();
            for (Map.Entry<BlockPos, String> entry : HighlightRenderer.highlightedPositions.entrySet()) {
                BlockPos pos = entry.getKey();
                String label = entry.getValue();
                int dist = (int) Math.sqrt(playerPos.distSqr(pos));
                graphics.drawString(client.font, "§e" + label + " §7" + dist + "m", x, y, 0xFFFFFF);
                y += 11;
            }
        });
    }
}
