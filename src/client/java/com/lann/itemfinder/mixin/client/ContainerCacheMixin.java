package com.lann.itemfinder.mixin.client;

import com.lann.itemfinder.CacheManager;
import com.lann.itemfinder.HighlightRenderer;
import com.lann.itemfinder.ParticleTrail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(Minecraft.class)
public class ContainerCacheMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            BlockPos playerPos = client.player.blockPosition();

            // Auto-clear highlight kalau container yang dibuka di-highlight
            for (BlockPos pos : HighlightRenderer.highlightedPositions.keySet()) {
                if (Math.sqrt(playerPos.distSqr(pos)) <= 5.0) {
                    HighlightRenderer.highlightedPositions.remove(pos);
                    ParticleTrail.removeTarget(pos);
                    if (HighlightRenderer.highlightedPositions.isEmpty()) {
                        HighlightRenderer.clearHighlights();
                        ParticleTrail.clearAll();
                    }
                    break;
                }
            }

            // Schedule cache update setelah 1 tick (biar container menu sudah fully loaded)
            client.execute(() -> {
                if (client.screen instanceof AbstractContainerScreen<?> cs) {
                    cacheOpenedContainer(client, cs);
                }
            });
        }
    }

    private void cacheOpenedContainer(Minecraft client, AbstractContainerScreen<?> screen) {
        if (client.player == null || client.level == null) return;
        if (!CacheManager.isAvailable()) return;

        BlockPos playerPos = client.player.blockPosition();
        AbstractContainerMenu menu = screen.getMenu();

        // Scan slot dari container (skip slot inventory player)
        Map<String, Integer> items = new HashMap<>();
        int containerSlots = menu.slots.size();

        // Slot inventory player biasanya 27 slot terakhir (inventory) + 9 hotbar
        // Kita ambil semua slot non-player-inventory
        int playerInvStart = containerSlots - 36; // 27 main + 9 hotbar
        if (playerInvStart < 0) playerInvStart = 0;

        for (int i = 0; i < playerInvStart; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                items.merge(id, stack.getCount(), Integer::sum);
            }
        }

        if (items.isEmpty()) return;

        // Cari BlockPos container yang paling dekat dengan player (dalam radius 5 block)
        // Cek block entity di sekitar player
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    var blockState = client.level.getBlockState(pos);
                    if (blockState.hasBlockEntity()) {
                        var be = client.level.getBlockEntity(pos);
                        if (be instanceof Container) {
                            String typeName = be.getClass().getSimpleName();
                            CacheManager.cacheContainer(pos, typeName, items);
                            return; // ambil yang pertama ditemukan
                        }
                    }
                }
            }
        }
    }
}