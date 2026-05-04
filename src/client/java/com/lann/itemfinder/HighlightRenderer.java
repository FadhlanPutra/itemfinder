package com.lann.itemfinder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightRenderer {

    public static final Map<BlockPos, String> highlightedPositions = new ConcurrentHashMap<>();
    public static long highlightUntil = 0;

    public static boolean isActive() {
        return System.currentTimeMillis() < highlightUntil;
    }

    public static void setHighlights(java.util.List<StorageScanner.SearchResult> results) {
        highlightedPositions.clear();
        for (StorageScanner.SearchResult result : results) {
            highlightedPositions.put(result.pos, result.containerType + " (" + result.count + ")");
        }
        int duration = ConfigManager.get().highlightDurationSeconds;
        highlightUntil = System.currentTimeMillis() + (duration * 1000L);
    }

    public static void clearHighlights() {
        highlightedPositions.clear();
        highlightUntil = 0;
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (System.currentTimeMillis() > highlightUntil) {
                highlightedPositions.clear();
                return;
            }
            if (highlightedPositions.isEmpty()) return;
            renderHighlights(context);
        });
    }

    private static void renderHighlights(WorldRenderContext context) {
        var camera = context.camera().getPosition();
        PoseStack poseStack = context.matrixStack();
        if (poseStack == null) return;

        var bufferSource = context.consumers();
        if (bufferSource == null) return;

        ConfigManager.Config cfg = ConfigManager.get();

        float pulse = cfg.highlightPulse
            ? (float)(Math.sin(System.currentTimeMillis() / 300.0) * 0.25 + 0.75)
            : 1.0f;

        float r = cfg.highlightColorR * pulse;
        float g = cfg.highlightColorG * pulse;
        float b = cfg.highlightColorB * pulse;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (BlockPos pos : highlightedPositions.keySet()) {
            drawBlockOutline(poseStack, buffer, pos, r, g, b);
        }

        poseStack.popPose();
    }

    private static void drawBlockOutline(PoseStack poseStack, VertexConsumer buffer,
                                          BlockPos pos, float r, float g, float b) {
        float x = pos.getX(), y = pos.getY(), z = pos.getZ();
        float x2 = x + 1f, y2 = y + 1f, z2 = z + 1f;
        float e = 0.002f;
        x -= e; y -= e; z -= e;
        x2 += e; y2 += e; z2 += e;

        Matrix4f mat = poseStack.last().pose();
        float a = 1.0f;

        line(buffer, mat, x,  y,  z,  x2, y,  z,  r, g, b, a);
        line(buffer, mat, x2, y,  z,  x2, y,  z2, r, g, b, a);
        line(buffer, mat, x2, y,  z2, x,  y,  z2, r, g, b, a);
        line(buffer, mat, x,  y,  z2, x,  y,  z,  r, g, b, a);
        line(buffer, mat, x,  y2, z,  x2, y2, z,  r, g, b, a);
        line(buffer, mat, x2, y2, z,  x2, y2, z2, r, g, b, a);
        line(buffer, mat, x2, y2, z2, x,  y2, z2, r, g, b, a);
        line(buffer, mat, x,  y2, z2, x,  y2, z,  r, g, b, a);
        line(buffer, mat, x,  y,  z,  x,  y2, z,  r, g, b, a);
        line(buffer, mat, x2, y,  z,  x2, y2, z,  r, g, b, a);
        line(buffer, mat, x2, y,  z2, x2, y2, z2, r, g, b, a);
        line(buffer, mat, x,  y,  z2, x,  y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer buf, Matrix4f mat,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        float nx = x2-x1, ny = y2-y1, nz = z2-z1;
        float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
        nx/=len; ny/=len; nz/=len;
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buf.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
