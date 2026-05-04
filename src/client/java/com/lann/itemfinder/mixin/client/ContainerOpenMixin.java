package com.lann.itemfinder.mixin.client;

import com.lann.itemfinder.HighlightRenderer;
import com.lann.itemfinder.ParticleTrail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ContainerOpenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            BlockPos playerPos = client.player.blockPosition();

            for (BlockPos pos : HighlightRenderer.highlightedPositions.keySet()) {
                double dist = Math.sqrt(playerPos.distSqr(pos));
                if (dist <= 5.0) {
                    HighlightRenderer.highlightedPositions.remove(pos);
                    ParticleTrail.removeTarget(pos);

                    if (HighlightRenderer.highlightedPositions.isEmpty()) {
                        HighlightRenderer.clearHighlights();
                        ParticleTrail.clearAll();
                    }
                    break;
                }
            }
        }
    }
}
