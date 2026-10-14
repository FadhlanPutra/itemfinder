package com.lann.itemfinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {

    private final Screen parent;

    private static final int ROW_START = 50;
    private static final int ROW_GAP   = 26;
    private static final int TITLE_PAD = 14;
    private static final int HINT_PAD  = 10;

    private static final int ROW_COUNT = 9;

    private static int rowY(int index) {
        return ROW_START + (ROW_GAP * index);
    }

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("gui.itemfinder.config_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx   = this.width / 2;
        int btnX = cx + 80;
        int btnW = 20;
        int btnH = 18;

        this.addRenderableWidget(Button.builder(Component.literal("-"),
            b -> { ConfigManager.get().radius = Math.max(10, ConfigManager.get().radius - 5); ConfigManager.save(); })
            .bounds(btnX, rowY(0), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            b -> { ConfigManager.get().radius = Math.min(200, ConfigManager.get().radius + 5); ConfigManager.save(); })
            .bounds(btnX + btnW + 2, rowY(0), btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.literal("-"),
            b -> { ConfigManager.get().highlightDurationSeconds = Math.max(3, ConfigManager.get().highlightDurationSeconds - 1); ConfigManager.save(); })
            .bounds(btnX, rowY(1), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            b -> { ConfigManager.get().highlightDurationSeconds = Math.min(60, ConfigManager.get().highlightDurationSeconds + 1); ConfigManager.save(); })
            .bounds(btnX + btnW + 2, rowY(1), btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> { ConfigManager.get().highlightPulse = !ConfigManager.get().highlightPulse; ConfigManager.save(); })
            .bounds(btnX, rowY(2), 50, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            b -> { ConfigManager.get().sortMode = ConfigManager.get().sortMode == 0 ? 1 : 0; ConfigManager.save(); })
            .bounds(btnX, rowY(3), 50, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> { ConfigManager.get().sendToChat = !ConfigManager.get().sendToChat; ConfigManager.save(); })
            .bounds(btnX, rowY(4), 50, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> { ConfigManager.get().particleTrail = !ConfigManager.get().particleTrail; ConfigManager.save(); })
            .bounds(btnX, rowY(5), 50, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            b -> {
                String[] opts = ParticleTrail.PARTICLE_OPTIONS;
                String cur = ConfigManager.get().particleType;
                int idx = 0;
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals(cur)) { idx = i; break; }
                }
                ConfigManager.get().particleType = opts[(idx + 1) % opts.length];
                ConfigManager.save();
            })
            .bounds(btnX, rowY(6), 50, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> { ConfigManager.get().searchByEnglish = !ConfigManager.get().searchByEnglish; ConfigManager.save(); })
            .bounds(btnX, rowY(7), 50, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            b -> { ConfigManager.get().enterMode = (ConfigManager.get().enterMode + 1) % 3; ConfigManager.save(); })
            .bounds(btnX, rowY(8), 50, btnH).build());

        int doneY = rowY(ROW_COUNT) + HINT_PAD + 8;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            b -> { ConfigManager.save(); this.minecraft.gui.setScreen(parent); }
        ).bounds(cx - 50, doneY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, this.width, this.height, 0xC0101018);

        ConfigManager.Config cfg = ConfigManager.get();
        int cx     = this.width / 2;
        int labelX = cx - 120;
        int off    = 5;

        int panelTop    = ROW_START - TITLE_PAD - 10;
        int panelBottom = rowY(ROW_COUNT) + HINT_PAD;
        g.fill(labelX - 6, panelTop, cx + 140, panelBottom, 0xEE000000);

        TextDrawer.drawCenteredString(g,
            Component.translatable("gui.itemfinder.config_title").copy().withStyle(net.minecraft.ChatFormatting.GOLD),
            cx, panelTop + 6, 0xFFFFFF);

        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_radius").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + cfg.radius + " ").withStyle(net.minecraft.ChatFormatting.YELLOW))
                .append(Component.translatable("gui.itemfinder.config.blocks").copy().withStyle(net.minecraft.ChatFormatting.GRAY)),
            labelX, rowY(0) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_highlight_duration").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + cfg.highlightDurationSeconds + "s").withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(1) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_highlight_pulse").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.highlightPulse ? "ON" : "OFF")).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(2) + off, 0xFFFFFF);

        String sortLabel = Component.translatable(
            cfg.sortMode == 0 ? "gui.itemfinder.sort_distance" : "gui.itemfinder.sort_count"
        ).getString();
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_sort").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + sortLabel).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(3) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_chat").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.sendToChat ? "ON" : "OFF")).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(4) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_particle").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.particleTrail ? "ON" : "OFF")).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(5) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_particle_type").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + cfg.particleType).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(6) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_search_lang").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.searchByEnglish ? "English (ID)" : "Game language")).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(7) + off, 0xFFFFFF);

        String enterLabel;
        switch (cfg.enterMode) {
            case 1: enterLabel = Component.translatable("gui.itemfinder.enter_top").getString(); break;
            case 2: enterLabel = Component.translatable("gui.itemfinder.enter_all").getString(); break;
            default: enterLabel = Component.translatable("gui.itemfinder.enter_off").getString();
        }
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_enter_mode").copy().withStyle(net.minecraft.ChatFormatting.WHITE)
                .append(Component.literal(" " + enterLabel).withStyle(net.minecraft.ChatFormatting.YELLOW)),
            labelX, rowY(8) + off, 0xFFFFFF);

        TextDrawer.drawCenteredString(g,
            Component.translatable("gui.itemfinder.config_save_hint").copy().withStyle(net.minecraft.ChatFormatting.GRAY),
            cx, panelBottom + 4, 0xAAAAAA);

        super.extractRenderState(g, mx, my, delta);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        this.minecraft.gui.setScreen(parent);
    }
}