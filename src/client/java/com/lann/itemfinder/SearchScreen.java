package com.lann.itemfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
// import com.lann.itemfinder.CacheManager;
// import com.lann.itemfinder.ServerDetector;

import java.util.ArrayList;
import java.util.List;

public class SearchScreen extends Screen {

    private EditBox searchBox;
    private List<ItemStack> matchingItems = new ArrayList<>();
    private List<StorageScanner.SearchResult> searchResults = new ArrayList<>();
    private String lastQuery   = "";
    private String statusMessage = "";
    private boolean showingResults = false;
    private boolean showingConfig  = false;
    private int hoveredSlot = -1;
    private int resultScrollOffset = 0;

    private static final int MAX_VISIBLE_RESULTS = 10;
    private static final int ITEM_SIZE  = 18;
    private static final int GRID_COLS  = 9;

    // ── Config panel row positions (fixed = label & button always aligned) ──
    private static final int CFG_START = 78;
    private static final int CFG_GAP   = 16;
    // private static final int CFG_ROW_0 = CFG_START;
    // private static final int CFG_ROW_1 = CFG_START + CFG_GAP;
    // private static final int CFG_ROW_2 = CFG_START + CFG_GAP * 2;
    // private static final int CFG_ROW_3 = CFG_START + CFG_GAP * 3;
    // private static final int CFG_ROW_4 = CFG_START + CFG_GAP * 4;
    // private static final int CFG_ROW_5 = CFG_START + CFG_GAP * 5;
    // private static final int CFG_ROW_6 = CFG_START + CFG_GAP * 6;
    // private static final int CFG_ROW_7 = CFG_START + CFG_GAP * 7;
    // private static final int CFG_ROW_8 = CFG_START + CFG_GAP * 8;

    // ── Config panel — otomatis berdasarkan jumlah row ────────────────────
    // private static final int CFG_START   = 75;
    // private static final int CFG_GAP     = 16;
    private static final int CFG_TITLE_PAD = 12;
    private static final int CFG_BOTTOM_PAD = 10;
    private static final int CFG_HINT_PAD  = 15; // Buat atur teks config hint
    private static final int CFG_ROW_COUNT = 10; // naikkan angka ini kalau tambah config baru

    private static int cfgRowY(int index) {
        return CFG_START + (CFG_GAP * index);
    }

    public SearchScreen() {
        super(Component.translatable("gui.itemfinder.title"));
    }

    @Override
    protected void init() {
        // ── Search box ──────────────────────────────────────────────────────
        searchBox = new EditBox(
            this.font, this.width / 2 - 100, 30, 200, 20,
            Component.translatable("gui.itemfinder.hint")
        );
        searchBox.setHint(Component.translatable("gui.itemfinder.hint"));
        searchBox.setResponder(this::onQueryChanged);
        this.addRenderableWidget(searchBox);
        this.setInitialFocus(searchBox);

        // ── Persistent buttons (index 1, 2, 3) ─────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.itemfinder.back"), btn -> handleBack()
        ).bounds(6, this.height - 26, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.itemfinder.config"), btn -> showingConfig = !showingConfig
        ).bounds(this.width - 68, this.height - 26, 62, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.itemfinder.scan_all"), btn -> doSearchAll()
        ).bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());

        // Config buttons (index 4+, hidden when config closed)
        int cx2  = this.width / 2;
        int btnX = cx2 + 80;
        int btnW = 20;
        int btnH = 13;

        // ROW 0: Radius
        this.addRenderableWidget(Button.builder(Component.literal("-"),
            btn -> { ConfigManager.get().radius = Math.max(10, ConfigManager.get().radius - 5); })
            .bounds(btnX, cfgRowY(0), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            btn -> { ConfigManager.get().radius = Math.min(200, ConfigManager.get().radius + 5); })
            .bounds(btnX + btnW + 2, cfgRowY(0), btnW, btnH).build()
        );

        // ROW 1: Highlight duration
        this.addRenderableWidget(Button.builder(Component.literal("-"),
            btn -> { ConfigManager.get().highlightDurationSeconds = Math.max(3, ConfigManager.get().highlightDurationSeconds - 1); })
            .bounds(btnX, cfgRowY(1), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            btn -> { ConfigManager.get().highlightDurationSeconds = Math.min(60, ConfigManager.get().highlightDurationSeconds + 1); })
            .bounds(btnX + btnW + 2, cfgRowY(1), btnW, btnH).build()
        );

        // ROW 2: Highlight pulse
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().highlightPulse = !ConfigManager.get().highlightPulse; })
            .bounds(btnX, cfgRowY(2), 50, btnH).build()
        );

        // ROW 3: Sort mode
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            btn -> { ConfigManager.get().sortMode = ConfigManager.get().sortMode == 0 ? 1 : 0; })
            .bounds(btnX, cfgRowY(3), 50, btnH).build()
        );

        // ROW 4: Send to chat
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().sendToChat = !ConfigManager.get().sendToChat; })
            .bounds(btnX, cfgRowY(4), 50, btnH).build()
        );

        // ROW 5: Particle trail
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().particleTrail = !ConfigManager.get().particleTrail; })
            .bounds(btnX, cfgRowY(5), 50, btnH).build()
        );

        // ROW 6: Particle type
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            btn -> {
                String[] opts = ParticleTrail.PARTICLE_OPTIONS;
                String cur = ConfigManager.get().particleType;
                int idx = 0;
                for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) { idx = i; break; }
                ConfigManager.get().particleType = opts[(idx + 1) % opts.length];
            }
        ).bounds(btnX, cfgRowY(6), 50, btnH).build()
        );

        // ROW 7: Search language
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().searchByEnglish = !ConfigManager.get().searchByEnglish; })
            .bounds(btnX, cfgRowY(7), 50, btnH).build()
        );

        // ROW 8: Enter mode
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            btn -> { ConfigManager.get().enterMode = (ConfigManager.get().enterMode + 1) % 3; })
            .bounds(btnX, cfgRowY(8), 50, btnH).build()
        );
    }

    // ── Query handler ───────────────────────────────────────────────────────
    private void onQueryChanged(String query) {
        showingResults = false;
        showingConfig  = false;
        searchResults.clear();
        statusMessage  = "";
        resultScrollOffset = 0;
        lastQuery = query.trim().toLowerCase();

        if (lastQuery.length() < 2) { matchingItems.clear(); return; }

        matchingItems.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            String itemId   = id.getPath().toLowerCase();
            String dispName = item.getDefaultInstance().getHoverName().getString().toLowerCase();

            boolean match = ConfigManager.get().searchByEnglish
                ? (itemId.contains(lastQuery) || dispName.contains(lastQuery))
                : dispName.contains(lastQuery);

            if (match) matchingItems.add(new ItemStack(item));
        }
    }

    // ── Input handlers ──────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { handleBack(); return true; } // ESC

        // Enter key
        if (keyCode == 257) {
            int mode = ConfigManager.get().enterMode;
            if (mode == 1 && !matchingItems.isEmpty() && !showingResults && !showingConfig) {
                // Pilih item teratas dari grid
                doSearch(matchingItems.get(0));
                return true;
            } else if (mode == 2 && lastQuery.length() >= 2 && !showingResults && !showingConfig) {
                // Scan all matches
                doSearchAll();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (showingResults && !searchResults.isEmpty()) {
            resultScrollOffset -= (int) sy;
            int max = Math.max(0, searchResults.size() - MAX_VISIBLE_RESULTS);
            resultScrollOffset = Math.max(0, Math.min(resultScrollOffset, max));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!showingResults && !showingConfig && !matchingItems.isEmpty()) {
            int slot = getSlotAt(mx, my);
            if (slot >= 0 && slot < matchingItems.size()) {
                doSearch(matchingItems.get(slot));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        hoveredSlot = getSlotAt(mx, my);
    }

    private int getSlotAt(double mx, double my) {
        if (matchingItems.isEmpty() || showingResults || showingConfig) return -1;
        int gx = this.width / 2 - (GRID_COLS * ITEM_SIZE) / 2;
        int gy = 60;
        int col = (int)(mx - gx) / ITEM_SIZE;
        int row = (int)(my - gy) / ITEM_SIZE;
        if (col < 0 || col >= GRID_COLS) return -1;
        int slot = row * GRID_COLS + col;
        if (slot < 0 || slot >= matchingItems.size()) return -1;
        int cx = gx + col * ITEM_SIZE, cy = gy + row * ITEM_SIZE;
        if (mx >= cx && mx < cx + ITEM_SIZE && my >= cy && my < cy + ITEM_SIZE) return slot;
        return -1;
    }

    // ── Search methods ──────────────────────────────────────────────────────
    private void doSearch(ItemStack targetItem) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        statusMessage = "§e" + Component.translatable("gui.itemfinder.searching").getString();
        showingResults = true;
        searchResults.clear();
        resultScrollOffset = 0;

        String targetId = BuiltInRegistries.ITEM.getKey(targetItem.getItem()).getPath();
        String itemName = targetItem.getHoverName().getString();

        if (ServerDetector.serverHasMod()) {
            // Server mode,  real-time scan via packet
            ClientNetworkHandler.onResultReceived = results -> handleResults(results, itemName, false);
            ClientNetworkHandler.sendSearchAllRequest(targetId);
        } else {
            // Cache mode,  scan dari local cache
            BlockPos playerPos = client.player.blockPosition();
            List<CacheManager.CacheSearchResult> cacheResults =
                CacheManager.searchExact(playerPos, ConfigManager.get().radius, targetId);
            handleCacheResults(cacheResults, itemName);
        }
    }

    private void doSearchAll() {
        if (lastQuery.length() < 2) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        statusMessage = "§e" + Component.translatable("gui.itemfinder.searching").getString();
        showingResults = true;
        searchResults.clear();
        resultScrollOffset = 0;

        String query = lastQuery;

        if (ServerDetector.serverHasMod()) {
            // Server mode
            ClientNetworkHandler.onResultReceived = results -> handleResults(results, "\"" + query + "\"", false);
            ClientNetworkHandler.sendSearchAllRequest(query);
        } else {
            // Cache mode
            BlockPos playerPos = client.player.blockPosition();
            List<CacheManager.CacheSearchResult> cacheResults =
                CacheManager.searchContains(playerPos, ConfigManager.get().radius, query);
            handleCacheResults(cacheResults, "\"" + query + "\"");
        }
    }

    // Handler untuk hasil dari server
    private void handleResults(List<StorageScanner.SearchResult> results, String label, boolean fromCache) {
        Minecraft client = Minecraft.getInstance();
        searchResults = results;

        String prefix = fromCache ? "§e⚠ §7(cache) " : "";

        if (results.isEmpty()) {
            statusMessage = prefix + "§c" + Component.translatable("gui.itemfinder.not_found").getString()
                + " §f" + label + " §c"
                + Component.translatable("gui.itemfinder.in_radius").getString()
                + " §f" + ConfigManager.get().radius + " §c"
                + Component.translatable("gui.itemfinder.block").getString();
        } else {
            statusMessage = prefix + "§a" + Component.translatable("gui.itemfinder.found").getString()
                + " §f" + results.size() + " §a"
                + Component.translatable("gui.itemfinder.container").getString();
            if (ConfigManager.get().sendToChat) {
                client.gui.getChat().addMessage(
                    Component.literal("§6=== Item Finder: §f" + label
                        + (fromCache ? " §7[cache]" : "") + " §6==="));
                for (var r : results)
                    client.gui.getChat().addMessage(Component.literal(r.format()));
            }
            HighlightRenderer.setHighlights(results);
            ParticleTrail.setTargets(results);
        }
        ClientNetworkHandler.onResultReceived = null;
    }

    // Handler untuk hasil dari cache
    private void handleCacheResults(List<CacheManager.CacheSearchResult> cacheResults, String label) {
        Minecraft client = Minecraft.getInstance();

        // Convert ke SearchResult untuk highlight & particle
        List<StorageScanner.SearchResult> results = cacheResults.stream()
            .map(CacheManager.CacheSearchResult::toSearchResult)
            .collect(java.util.stream.Collectors.toList());

        searchResults = results;

        String prefix = "§e⚠ §7(cache) ";

        if (results.isEmpty()) {
            statusMessage = prefix + "§c" + Component.translatable("gui.itemfinder.not_found").getString()
                + " §f" + label + " §c"
                + Component.translatable("gui.itemfinder.in_radius").getString()
                + " §f" + ConfigManager.get().radius + " §c"
                + Component.translatable("gui.itemfinder.block").getString();
        } else {
            statusMessage = prefix + "§a" + Component.translatable("gui.itemfinder.found").getString()
                + " §f" + results.size() + " §a"
                + Component.translatable("gui.itemfinder.container").getString();
            if (ConfigManager.get().sendToChat) {
                client.gui.getChat().addMessage(
                    Component.literal("§6=== Item Finder: §f" + label + " §7[cache] §6==="));
                for (var r : cacheResults)
                    client.gui.getChat().addMessage(Component.literal(r.format()));
            }
            HighlightRenderer.setHighlights(results);
            ParticleTrail.setTargets(results);
        }

        // Tampilkan warning cache di chat
        if (ConfigManager.get().sendToChat && !results.isEmpty()) {
            client.gui.getChat().addMessage(
                Component.literal("§e⚠ §7" + Component.translatable("gui.itemfinder.cache_warning").getString()));
        }
    }
    
    private void handleBack() {
        if (showingConfig) {
            showingConfig = false;
            ConfigManager.save();
        } else if (showingResults) {
            showingResults = false;
            searchResults.clear();
            statusMessage = "";
        } else {
            this.onClose();
        }
    }

    // ── Render ──────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        updateButtonVisibility();
        this.renderBackground(g, mx, my, delta);
        g.drawCenteredString(this.font,
            Component.translatable("gui.itemfinder.title").getString(),
            this.width / 2, 12, 0xFFD700);
        super.render(g, mx, my, delta);

        if (showingConfig)        renderConfig(g);
        else if (showingResults)  renderResults(g);
        else                      renderItemGrid(g, mx, my);

        if (!statusMessage.isEmpty() && !showingConfig)
            g.drawCenteredString(this.font, statusMessage, this.width / 2, this.height - 40, 0xFFFFFF);

        if (!showingResults && !showingConfig && hoveredSlot >= 0 && hoveredSlot < matchingItems.size())
            g.renderTooltip(this.font, matchingItems.get(hoveredSlot), mx, my);
    }

    private void renderItemGrid(GuiGraphics g, int mx, int my) {
        if (matchingItems.isEmpty()) {
            String msg = lastQuery.length() >= 2
                ? Component.translatable("gui.itemfinder.no_match").getString()
                : Component.translatable("gui.itemfinder.search_min").getString();
            g.drawCenteredString(this.font, "§7" + msg, this.width / 2, 70, 0xAAAAAA);
            return;
        }
        int gx = this.width / 2 - (GRID_COLS * ITEM_SIZE) / 2, gy = 60;
        int rows = (int) Math.ceil((double) matchingItems.size() / GRID_COLS);
        int gw = GRID_COLS * ITEM_SIZE, gh = rows * ITEM_SIZE;
        g.fill(gx - 2, gy - 2, gx + gw + 2, gy + gh + 2, 0x88000000);
        for (int i = 0; i < matchingItems.size(); i++) {
            int col = i % GRID_COLS, row = i / GRID_COLS;
            int ix = gx + col * ITEM_SIZE, iy = gy + row * ITEM_SIZE;
            if (i == hoveredSlot) g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0x88FFFFFF);
            g.renderItem(matchingItems.get(i), ix + 1, iy + 1);
        }
        g.drawCenteredString(this.font,
            "§7" + matchingItems.size() + Component.translatable("gui.itemfinder.click_to_scan").getString(),
            this.width / 2, gy + gh + 6, 0xAAAAAA);
    }

    private void renderResults(GuiGraphics g) {
        if (searchResults.isEmpty()) return;
        int sy = 60, px = this.width / 2 - 130, pw = 260;
        int visible = Math.min(MAX_VISIBLE_RESULTS, searchResults.size());
        g.fill(px - 2, sy - 2, px + pw + 2, sy + visible * 13 + 4, 0x88000000);

        Minecraft client = Minecraft.getInstance();
        BlockPos pp = client.player != null ? client.player.blockPosition() : BlockPos.ZERO;
        for (int i = 0; i < visible; i++) {
            int idx = i + resultScrollOffset;
            if (idx >= searchResults.size()) break;
            var r = searchResults.get(idx);
            int dist = (int) Math.sqrt(pp.distSqr(r.pos));
            g.drawString(this.font, r.format() + " §7(" + dist + "m)", px, sy + i * 13, 0xFFFFFF);
        }
        if (searchResults.size() > MAX_VISIBLE_RESULTS) {
            g.drawCenteredString(this.font,
                "§7scroll ↑↓ (" + (resultScrollOffset + 1) + "-" +
                Math.min(resultScrollOffset + MAX_VISIBLE_RESULTS, searchResults.size()) +
                " / " + searchResults.size() + ")",
                this.width / 2, sy + visible * 13 + 8, 0xAAAAAA);
        }
    }

    private void renderConfig(GuiGraphics g) {
        ConfigManager.Config cfg = ConfigManager.get();
        int cx     = this.width / 2;
        int labelX = cx - 120;
        int off    = 3;

        // Background otomatis berdasarkan CFG_ROW_COUNT
        // int panelTop    = cfgRowY(0) - CFG_TITLE_PAD - 10;
        // int panelBottom = cfgRowY(CFG_ROW_COUNT) + CFG_HINT_PAD;
        int contentBottom = cfgRowY(CFG_ROW_COUNT);
        int hintY = contentBottom + CFG_HINT_PAD;
        int panelTop = cfgRowY(0) - CFG_TITLE_PAD - 10;
        int panelBottom = hintY + CFG_BOTTOM_PAD;
        g.fill(labelX - 4, panelTop, cx + 135, panelBottom, 0xAA000000);

        // Title otomatis di atas panel
        g.drawCenteredString(this.font,
            Component.translatable("gui.itemfinder.config_title").getString(),
            cx, panelTop + 4, 0xFFD700);

        // Labels
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_radius").getString() + " §e" + cfg.radius + " block",
            labelX, cfgRowY(0) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_highlight_duration").getString() + " §e" + cfg.highlightDurationSeconds + "s",
            labelX, cfgRowY(1) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_highlight_pulse").getString() + " §e" + (cfg.highlightPulse ? "ON" : "OFF"),
            labelX, cfgRowY(2) + off, 0xFFFFFF);

        String sortLabel = Component.translatable(
            cfg.sortMode == 0 ? "gui.itemfinder.sort_distance" : "gui.itemfinder.sort_count"
        ).getString();
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_sort").getString() + " §e" + sortLabel,
            labelX, cfgRowY(3) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_chat").getString() + " §e" + (cfg.sendToChat ? "ON" : "OFF"),
            labelX, cfgRowY(4) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_particle").getString() + " §e" + (cfg.particleTrail ? "ON" : "OFF"),
            labelX, cfgRowY(5) + off, 0xFFFFFF);

        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_particle_type").getString() + " §e" + cfg.particleType,
            labelX, cfgRowY(6) + off, 0xFFFFFF);

        String langLabel = Component.translatable(
            cfg.searchByEnglish ? "gui.itemfinder.search_lang_english" : "gui.itemfinder.search_lang_game"
        ).getString();
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_search_lang").getString() + " §e" + langLabel,
            labelX, cfgRowY(7) + off, 0xFFFFFF);

        String enterLabel = Component.translatable(
            switch (cfg.enterMode) {
                case 1 -> "gui.itemfinder.enter_top";
                case 2 -> "gui.itemfinder.enter_all";
                default -> "gui.itemfinder.enter_off";
            }
        ).getString();
        g.drawString(this.font,
            Component.translatable("gui.itemfinder.config_enter_mode").getString() + " §e" + enterLabel,
            labelX, cfgRowY(8) + off, 0xFFFFFF);

        // Hint otomatis di bawah row terakhir
        g.drawCenteredString(this.font,
            "§7" + Component.translatable("gui.itemfinder.config_save_hint").getString(),
            // cx, panelBottom - CFG_HINT_PAD + 2, 0x888888);
            cx, hintY, 0x888888);
    }

    private void updateButtonVisibility() {
        int i = 0;
        for (var widget : this.children()) {
            // index 0 = searchBox, 1 = back, 2 = config, 3 = scan all
            // index 4+ = config buttons
            if (i > 3 && widget instanceof Button btn) {
                btn.visible = showingConfig;
            }
            i++;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}