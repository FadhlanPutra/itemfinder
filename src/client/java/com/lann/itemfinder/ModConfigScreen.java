package com.lann.itemfinder;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {

    private final Screen parent;

    private static final int ROW_START = 50;
    private static final int ROW_GAP   = 26;
    private static final int TITLE_PAD = 14; // jarak title ke row pertama
    private static final int HINT_PAD  = 10; // jarak hint ke row terakhir

    // ── Daftar semua baris config ─────────────────────────────────────────
    // Tambah baris baru? Cukup naikkan ROW_COUNT dan tambah widget + label.
    // Background, title, hint, dan Done button otomatis menyesuaikan.
    private static final int ROW_COUNT = 9; // total jumlah baris config

    // Posisi Y dihitung otomatis dari index baris
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

        // ROW 0: Radius
        this.addRenderableWidget(Button.builder(Component.literal("-"),
            b -> ConfigManager.get().radius = Math.max(10, ConfigManager.get().radius - 5))
            .bounds(btnX, rowY(0), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            b -> ConfigManager.get().radius = Math.min(200, ConfigManager.get().radius + 5))
            .bounds(btnX + btnW + 2, rowY(0), btnW, btnH).build());

        // ROW 1: Highlight duration
        this.addRenderableWidget(Button.builder(Component.literal("-"),
            b -> ConfigManager.get().highlightDurationSeconds = Math.max(3, ConfigManager.get().highlightDurationSeconds - 1))
            .bounds(btnX, rowY(1), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            b -> ConfigManager.get().highlightDurationSeconds = Math.min(60, ConfigManager.get().highlightDurationSeconds + 1))
            .bounds(btnX + btnW + 2, rowY(1), btnW, btnH).build());

        // ROW 2: Highlight pulse
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> ConfigManager.get().highlightPulse = !ConfigManager.get().highlightPulse)
            .bounds(btnX, rowY(2), 50, btnH).build());

        // ROW 3: Sort mode
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            b -> ConfigManager.get().sortMode = ConfigManager.get().sortMode == 0 ? 1 : 0)
            .bounds(btnX, rowY(3), 50, btnH).build());

        // ROW 4: Send to chat
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> ConfigManager.get().sendToChat = !ConfigManager.get().sendToChat)
            .bounds(btnX, rowY(4), 50, btnH).build());

        // ROW 5: Particle trail
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> ConfigManager.get().particleTrail = !ConfigManager.get().particleTrail)
            .bounds(btnX, rowY(5), 50, btnH).build());

        // ROW 6: Particle type
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            b -> {
                String[] opts = ParticleTrail.PARTICLE_OPTIONS;
                String cur = ConfigManager.get().particleType;
                int idx = 0;
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals(cur)) { idx = i; break; }
                }
                ConfigManager.get().particleType = opts[(idx + 1) % opts.length];
            })
            .bounds(btnX, rowY(6), 50, btnH).build());

        // ROW 7: Search language
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            b -> ConfigManager.get().searchByEnglish = !ConfigManager.get().searchByEnglish)
            .bounds(btnX, rowY(7), 50, btnH).build());

        // ROW 8: Enter mode
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            b -> ConfigManager.get().enterMode = (ConfigManager.get().enterMode + 1) % 3)
            .bounds(btnX, rowY(8), 50, btnH).build());

        // Done button — selalu di bawah panel
        int doneY = rowY(ROW_COUNT) + HINT_PAD + 8;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            b -> { ConfigManager.save(); this.minecraft.setScreen(parent); }
        ).bounds(cx - 50, doneY, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderBackground(g, mx, my, delta);
        super.render(g, mx, my, delta);

        ConfigManager.Config cfg = ConfigManager.get();
        int cx     = this.width / 2;
        int labelX = cx - 120;
        int off    = 5;

        // ── Background panel — otomatis berdasarkan ROW_COUNT ─────────────
        int panelTop    = ROW_START - TITLE_PAD - 10;
        int panelBottom = rowY(ROW_COUNT) + HINT_PAD;
        g.fill(labelX - 6, panelTop, cx + 140, panelBottom, 0xAA000000);

        // ── Title — otomatis di atas panel ────────────────────────────────
        g.drawCenteredString(this.font,
            this.title.getString(), cx, panelTop + 4, 0xFFD700);

        // ── Labels — satu per baris ───────────────────────────────────────
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_radius").getString() + " §e" + cfg.radius + " block",
            labelX, rowY(0) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_highlight_duration").getString() + " §e" + cfg.highlightDurationSeconds + "s",
            labelX, rowY(1) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_highlight_pulse").getString() + " §e" + (cfg.highlightPulse ? "ON" : "OFF"),
            labelX, rowY(2) + off, 0xFFFFFF);

        String sortLabel = Component.translatable(
            cfg.sortMode == 0 ? "gui.itemfinder.sort_distance" : "gui.itemfinder.sort_count"
        ).getString();
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_sort").getString() + " §e" + sortLabel,
            labelX, rowY(3) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_chat").getString() + " §e" + (cfg.sendToChat ? "ON" : "OFF"),
            labelX, rowY(4) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_particle").getString() + " §e" + (cfg.particleTrail ? "ON" : "OFF"),
            labelX, rowY(5) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_particle_type").getString() + " §e" + cfg.particleType,
            labelX, rowY(6) + off, 0xFFFFFF);

        String langLabel = Component.translatable(
            cfg.searchByEnglish ? "gui.itemfinder.search_lang_english" : "gui.itemfinder.search_lang_game"
        ).getString();
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_search_lang").getString() + " §e" + langLabel,
            labelX, rowY(7) + off, 0xFFFFFF);

        String enterLabel = Component.translatable(
            switch (cfg.enterMode) {
                case 1 -> "gui.itemfinder.enter_top";
                case 2 -> "gui.itemfinder.enter_all";
                default -> "gui.itemfinder.enter_off";
            }
        ).getString();
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_enter_mode").getString() + " §e" + enterLabel,
            labelX, rowY(8) + off, 0xFFFFFF);

        // ── Hint — otomatis di bawah row terakhir ─────────────────────────
        g.drawCenteredString(this.font,
            "§7" + Component.translatable("gui.itemfinder.config_save_hint").getString(),
            cx, panelBottom - HINT_PAD + 2, 0x888888);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        this.minecraft.setScreen(parent);
    }
}