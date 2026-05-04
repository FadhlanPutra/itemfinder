package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleTrail {

    private static final Map<BlockPos, Integer> targets = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    // Daftar partikel yang bisa dipilih
    public static final String[] PARTICLE_OPTIONS = {
        "FLAME", "ENCHANT", "END ROD", "SOUL FIRE FLAME",
        "WITCH", "DRAGON BREATH", "PORTAL", "HAPPY VILLAGER"
    };

    public static SimpleParticleType getParticle(String name) {
        return switch (name) {
            case "ENCHANT" -> ParticleTypes.ENCHANT;
            case "END ROD" -> ParticleTypes.END_ROD;
            case "SOUL FIRE FLAME" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "WITCH" -> ParticleTypes.WITCH;
            case "DRAGON BREATH" -> ParticleTypes.DRAGON_BREATH;
            case "PORTAL" -> ParticleTypes.PORTAL;
            case "HAPPY VILLAGER" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.FLAME;
        };
    }

    public static void setTargets(java.util.List<StorageScanner.SearchResult> results) {
        targets.clear();
        for (StorageScanner.SearchResult result : results) {
            targets.put(result.pos, result.count);
        }
    }

    public static void removeTarget(BlockPos pos) {
        targets.remove(pos);
    }

    public static void clearAll() {
        targets.clear();
        tickCounter = 0;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ConfigManager.get().particleTrail) return;
            if (targets.isEmpty()) return;
            if (!HighlightRenderer.isActive()) {
                clearAll();
                return;
            }

            LocalPlayer player = client.player;
            if (player == null || client.level == null) return;

            tickCounter++;
            if (tickCounter % 3 != 0) return;

            Vec3 playerPos = player.position().add(0, 1.0, 0);

            double minDist = Double.MAX_VALUE;
            for (BlockPos pos : targets.keySet()) {
                double dist = playerPos.distanceTo(
                    new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                if (dist < minDist) minDist = dist;
            }

            SimpleParticleType chosenParticle = getParticle(ConfigManager.get().particleType);

            for (Map.Entry<BlockPos, Integer> entry : targets.entrySet()) {
                BlockPos pos = entry.getKey();
                Vec3 targetPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                double dist = playerPos.distanceTo(targetPos);

                boolean isNearest = dist <= minDist + 3.0;
                int particleCount = isNearest ? 3 : 1;
                Vec3 direction = targetPos.subtract(playerPos).normalize();

                for (int i = 0; i < particleCount; i++) {
                    double progress = (tickCounter % 20) / 20.0;
                    Vec3 spawnPos = playerPos.add(direction.scale(dist * progress));
                    double rx = (Math.random() - 0.5) * 0.3;
                    double ry = (Math.random() - 0.5) * 0.3;
                    double rz = (Math.random() - 0.5) * 0.3;
                    client.level.addParticle(
                        chosenParticle,
                        spawnPos.x + rx, spawnPos.y + ry, spawnPos.z + rz,
                        direction.x * 0.05, direction.y * 0.05, direction.z * 0.05
                    );
                }
            }
        });
    }
}