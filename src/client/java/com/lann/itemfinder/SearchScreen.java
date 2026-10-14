package com.lann.itemfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SearchScreen extends Screen {

    private EditBox searchBox;
    private List<ItemStack> matchingItems = new ArrayList<>();
    private List<StorageScanner.SearchResult> searchResults = new ArrayList<>();
    private String lastQuery   = "";
    private Component statusMessage = Component.empty();
    private String statusQuery = "";
    private int statusCount = 0;
    private boolean statusFound = false;
    private boolean statusCache = false;    private boolean showingResults = false;
    private boolean showingConfig  = false;
    private int hoveredSlot = -1;
    private int resultScrollOffset = 0;

    private static final int MAX_VISIBLE_RESULTS = 10;
    private static final int ITEM_SIZE  = 18;
    private static final int GRID_COLS  = 9;

    // ── Config panel ────────────────────
    private static final int CFG_START = 78;
    private static final int CFG_GAP   = 16;
    private static final int CFG_TITLE_PAD = 12;
    private static final int CFG_BOTTOM_PAD = 10;
    private static final int CFG_HINT_PAD  = 15;
    private static final int CFG_ROW_COUNT = 10;

    private static int cfgRowY(int index) {
        return CFG_START + (CFG_GAP * index);
    }

    enum EnterAction { NONE, PICK_FIRST, SCAN_ALL }

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
            btn -> { ConfigManager.get().radius = Math.max(10, ConfigManager.get().radius - 5); ConfigManager.save(); })
            .bounds(btnX, cfgRowY(0), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            btn -> { ConfigManager.get().radius = Math.min(200, ConfigManager.get().radius + 5); ConfigManager.save(); })
            .bounds(btnX + btnW + 2, cfgRowY(0), btnW, btnH).build()
        );

        // ROW 1: Highlight duration
        this.addRenderableWidget(Button.builder(Component.literal("-"),
            btn -> { ConfigManager.get().highlightDurationSeconds = Math.max(3, ConfigManager.get().highlightDurationSeconds - 1); ConfigManager.save(); })
            .bounds(btnX, cfgRowY(1), btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"),
            btn -> { ConfigManager.get().highlightDurationSeconds = Math.min(60, ConfigManager.get().highlightDurationSeconds + 1); ConfigManager.save(); })
            .bounds(btnX + btnW + 2, cfgRowY(1), btnW, btnH).build()
        );

        // ROW 2: Highlight pulse
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().highlightPulse = !ConfigManager.get().highlightPulse; ConfigManager.save(); })
            .bounds(btnX, cfgRowY(2), 50, btnH).build()
        );

        // ROW 3: Sort mode
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            btn -> { ConfigManager.get().sortMode = ConfigManager.get().sortMode == 0 ? 1 : 0; ConfigManager.save(); })
            .bounds(btnX, cfgRowY(3), 50, btnH).build()
        );

        // ROW 4: Send to chat
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().sendToChat = !ConfigManager.get().sendToChat; ConfigManager.save(); })
            .bounds(btnX, cfgRowY(4), 50, btnH).build()
        );

        // ROW 5: Particle trail
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().particleTrail = !ConfigManager.get().particleTrail; ConfigManager.save(); })
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
                ConfigManager.save();
            }
        ).bounds(btnX, cfgRowY(6), 50, btnH).build()
        );

        // ROW 7: Search language
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.toggle"),
            btn -> { ConfigManager.get().searchByEnglish = !ConfigManager.get().searchByEnglish; ConfigManager.save(); })
            .bounds(btnX, cfgRowY(7), 50, btnH).build()
        );

        // ROW 8: Enter mode
        this.addRenderableWidget(Button.builder(Component.translatable("gui.itemfinder.change"),
            btn -> { ConfigManager.get().enterMode = (ConfigManager.get().enterMode + 1) % 3; ConfigManager.save(); })
            .bounds(btnX, cfgRowY(8), 50, btnH).build()
        );
    }

    // ── Query handler ───────────────────────────────────────────────────────
    private void onQueryChanged(String query) {
        showingResults = false;
        showingConfig  = false;
        searchResults.clear();
        statusMessage  = Component.empty();
        statusQuery = "";
        statusCount = 0;
        statusFound = false;
        statusCache = false;        resultScrollOffset = 0;
        lastQuery = query.trim().toLowerCase();

        if (lastQuery.length() < 2) { matchingItems.clear(); return; }

        matchingItems.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            String itemId   = id.getPath().toLowerCase();
            String dispName = item.getDefaultInstance().getHoverName().getString().toLowerCase();

            if (matchesQuery(itemId, dispName, lastQuery, ConfigManager.get().searchByEnglish)) {
                matchingItems.add(new ItemStack(item));
            }
        }
    }

    /**
     * Determines whether an item matches the query, per the "Search Language"
     * config (searchByEnglish). Separated from onQueryChanged() so it can be
     * unit-tested directly without the Minecraft item registry.
     */
    static boolean matchesQuery(String itemId, String displayName, String query, boolean searchByEnglish) {
        if (searchByEnglish) {
            return itemId.contains(query) || displayName.contains(query);
        }
        return displayName.contains(query);
    }

    // ── Input handlers ──────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            handleBack();
            return true;
        }

        EnterAction action = decideEnterAction(
            event,
            ConfigManager.get().enterMode,
            !matchingItems.isEmpty(),
            lastQuery.length(),
            showingResults,
            showingConfig
        );
        if (action == EnterAction.PICK_FIRST) {
            doSearch(matchingItems.get(0));
            return true;
        } else if (action == EnterAction.SCAN_ALL) {
            doSearchAll();
            return true;
        }

        return super.keyPressed(event);
    }

    static EnterAction decideEnterAction(KeyEvent event, int enterMode, boolean hasMatchingItems,
                                          int queryLength, boolean showingResults, boolean showingConfig) {
        if (!event.isConfirmation()) return EnterAction.NONE;
        return decideEnterAction(enterMode, hasMatchingItems, queryLength, showingResults, showingConfig);
    }

    static EnterAction decideEnterAction(int enterMode, boolean hasMatchingItems,
                                          int queryLength, boolean showingResults, boolean showingConfig) {
        if (showingResults || showingConfig) return EnterAction.NONE;
        if (enterMode == 1 && hasMatchingItems) return EnterAction.PICK_FIRST;
        if (enterMode == 2 && queryLength >= 2) return EnterAction.SCAN_ALL;
        return EnterAction.NONE;
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (!showingResults && !showingConfig && !matchingItems.isEmpty()) {
            int slot = getSlotAt(mx, my);
            if (slot >= 0 && slot < matchingItems.size()) {
                doSearch(matchingItems.get(slot));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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

        ItemFinderMod.LOGGER.info("[ItemFinder] doSearch invoked for " + targetItem);
        statusMessage = Component.translatable("gui.itemfinder.searching");
        statusCount = 0;
        statusFound = false;
        statusCache = false;
        showingResults = true;
        searchResults.clear();
        resultScrollOffset = 0;

        String targetId = BuiltInRegistries.ITEM.getKey(targetItem.getItem()).getPath();
        String typedQuery = searchBox != null ? searchBox.getValue().trim() : "";
        String label = typedQuery.isEmpty() ? targetItem.getHoverName().getString() : typedQuery;
        statusQuery = label;

        ItemFinderMod.LOGGER.info("[ItemFinder] doSearch: serverHasMod=" + ServerDetector.serverHasMod() + ", targetId=" + targetId);
        if (ServerDetector.serverHasMod()) {
            ClientNetworkHandler.onResultReceived = results -> handleResults(results, label, false);
            ClientNetworkHandler.sendSearchAllRequest(targetId);
        } else {
            BlockPos playerPos = client.player.blockPosition();
            List<CacheManager.CacheSearchResult> cacheResults =
                CacheManager.searchExact(playerPos, ConfigManager.get().radius, targetId);
            ItemFinderMod.LOGGER.info("[ItemFinder] doSearch cache mode: " + cacheResults.size() + " results");
            handleCacheResults(cacheResults, label);
        }
    }

    private void doSearchAll() {
        if (lastQuery.length() < 2) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        ItemFinderMod.LOGGER.info("[ItemFinder] doSearchAll invoked, lastQuery='" + lastQuery + "', serverHasMod=" + ServerDetector.serverHasMod());
        statusMessage = Component.translatable("gui.itemfinder.searching");
        statusCount = 0;
        statusFound = false;
        statusCache = false;
        showingResults = true;
        searchResults.clear();
        resultScrollOffset = 0;

        String query = lastQuery;
        statusQuery = query;

        if (ServerDetector.serverHasMod()) {
            ClientNetworkHandler.onResultReceived = results -> handleResults(results, query, false);
            ClientNetworkHandler.sendSearchAllRequest(query);
        } else {
            BlockPos playerPos = client.player.blockPosition();
            List<CacheManager.CacheSearchResult> cacheResults =
                CacheManager.searchContains(playerPos, ConfigManager.get().radius, query);
            ItemFinderMod.LOGGER.info("[ItemFinder] doSearchAll cache mode: " + cacheResults.size() + " results");
            handleCacheResults(cacheResults, query);
        }
    }

    private void handleResults(List<StorageScanner.SearchResult> results, String label, boolean fromCache) {
        ItemFinderMod.LOGGER.info("[ItemFinder] handleResults invoked, " + results.size() + " results, label=" + label + ", fromCache=" + fromCache);
        Minecraft client = Minecraft.getInstance();
        searchResults = results;

        if (results.isEmpty()) {
            statusMessage = ResultFormatter.notFound(label, ConfigManager.get().radius, fromCache);
            statusFound = false;
        } else {
            statusFound = true;
            statusCount = results.size();
            statusQuery = label;
            statusCache = fromCache;
            statusMessage = statusComponent();
            if (ConfigManager.get().sendToChat) {
                client.player.sendSystemMessage(ResultFormatter.chatHeader(label, fromCache));
                BlockPos pp = client.player.blockPosition();
                for (var r : results)
                    client.player.sendSystemMessage(ResultFormatter.resultLine(r, pp));
            }
            HighlightRenderer.setHighlights(results);
            ParticleTrail.setTargets(results);
        }
        ClientNetworkHandler.onResultReceived = null;

        if (!fromCache) {
            for (StorageScanner.SearchResult r : results) {
                if (r.items != null && !r.items.isEmpty()) {
                    CacheManager.cacheContainer(r.pos, r.containerType, r.items);
                }
            }
        }
    }

    private void handleCacheResults(List<CacheManager.CacheSearchResult> cacheResults, String label) {
        Minecraft client = Minecraft.getInstance();

        List<StorageScanner.SearchResult> results = cacheResults.stream()
            .map(CacheManager.CacheSearchResult::toSearchResult)
            .collect(java.util.stream.Collectors.toList());

        searchResults = results;

        if (results.isEmpty()) {
            statusMessage = ResultFormatter.notFound(label, ConfigManager.get().radius, true);
            statusFound = false;
        } else {
            statusFound = true;
            statusCount = results.size();
            statusQuery = label;
            statusCache = true;
            statusMessage = statusComponent();
            if (ConfigManager.get().sendToChat) {
                client.player.sendSystemMessage(ResultFormatter.chatHeader(label, true));
                BlockPos pp = client.player.blockPosition();
                for (var r : cacheResults)
                    client.player.sendSystemMessage(ResultFormatter.resultLine(r.toSearchResult(), pp));
            }
            HighlightRenderer.setHighlights(results);
            ParticleTrail.setTargets(results);
        }

        if (ConfigManager.get().sendToChat && !results.isEmpty()) {
            client.player.sendSystemMessage(
                Component.translatable("gui.itemfinder.cache_warning"));
        }
    }

    /** Summary component, rebuilt at render so it follows the game language. */
    private Component statusComponent() {
        if (!statusFound) return statusMessage;
        if (statusCache) return Component.translatable("gui.itemfinder.summary.cache",
            statusQuery, statusCount,
            Component.translatable(statusCount == 1
                ? "gui.itemfinder.container.one" : "gui.itemfinder.container.other"));
        return ResultFormatter.summary(statusQuery, statusCount);
    }

    private void handleBack() {
        if (showingConfig) {
            showingConfig = false;
            ConfigManager.save();
        } else if (showingResults) {
            showingResults = false;
            searchResults.clear();
            statusMessage = Component.empty();
            statusQuery = "";
            statusCount = 0;
            statusFound = false;
            statusCache = false;        } else {
            this.onClose();
        }
    }

    // ── Render ──────────────────────────────────────────────────────────────
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        updateButtonVisibility();

        if (showingConfig) renderConfigBackground(g);

        super.extractRenderState(g, mx, my, delta);

        TextDrawer.drawCenteredString(g,
            Component.translatable("gui.itemfinder.title").getString(),
            this.width / 2, 12, 0xFFD700);

        if (showingConfig)        renderConfigLabels(g);
        else if (showingResults)  renderResults(g);
        else                      renderItemGrid(g, mx, my);

        if (!statusMessage.equals(Component.empty()) && !showingConfig)
            TextDrawer.drawCenteredString(g, statusComponent(), this.width / 2, this.height - 40, 0xFFFFFF);

        if (!showingResults && !showingConfig && hoveredSlot >= 0 && hoveredSlot < matchingItems.size())
            g.setTooltipForNextFrame(this.font, matchingItems.get(hoveredSlot), mx, my);
    }

    private void renderItemGrid(GuiGraphicsExtractor g, int mx, int my) {
        if (matchingItems.isEmpty()) {
            Component msg = lastQuery.length() >= 2
                ? Component.translatable("gui.itemfinder.no_match")
                : Component.translatable("gui.itemfinder.search_min");
            TextDrawer.drawCenteredString(g, msg.copy().withStyle(ChatFormatting.GRAY), this.width / 2, 70, 0xAAAAAA);
            return;
        }
        int gx = this.width / 2 - (GRID_COLS * ITEM_SIZE) / 2, gy = 60;
        int rows = (int) Math.ceil((double) matchingItems.size() / GRID_COLS);
        int gw = GRID_COLS * ITEM_SIZE, gh = rows * ITEM_SIZE;
        g.fill(gx - 2, gy - 2, gx + gw + 2, gy + gh + 2, 0x55000000);
        for (int i = 0; i < matchingItems.size(); i++) {
            int col = i % GRID_COLS, row = i / GRID_COLS;
            int ix = gx + col * ITEM_SIZE, iy = gy + row * ITEM_SIZE;
            if (i == hoveredSlot) g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0x55FFFFFF);
            g.item(matchingItems.get(i), ix + 1, iy + 1);
        }
        TextDrawer.drawCenteredString(g,
            Component.literal(String.valueOf(matchingItems.size())).withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("gui.itemfinder.click_to_scan").copy().withStyle(ChatFormatting.GRAY)),
            this.width / 2, gy + gh + 6, 0xAAAAAA);
    }

    private void renderResults(GuiGraphicsExtractor g) {
        if (searchResults.isEmpty()) return;
        int sy = 60, px = this.width / 2 - 130, pw = 260;
        int visible = Math.min(MAX_VISIBLE_RESULTS, searchResults.size());
        g.fill(px - 2, sy - 2, px + pw + 2, sy + visible * 13 + 4, 0x55000000);

        Minecraft client = Minecraft.getInstance();
        BlockPos pp = client.player != null ? client.player.blockPosition() : BlockPos.ZERO;
        for (int i = 0; i < visible; i++) {
            int idx = i + resultScrollOffset;
            if (idx >= searchResults.size()) break;
            var r = searchResults.get(idx);
            TextDrawer.drawString(g, ResultFormatter.resultLine(r, pp), px, sy + i * 13, 0xFFFFFF);
        }
        if (searchResults.size() > MAX_VISIBLE_RESULTS) {
            TextDrawer.drawCenteredString(g,
                Component.literal("scroll ↑↓ (" + (resultScrollOffset + 1) + "-" +
                    Math.min(resultScrollOffset + MAX_VISIBLE_RESULTS, searchResults.size()) +
                    " / " + searchResults.size() + ")").copy().withStyle(ChatFormatting.GRAY),
                this.width / 2, sy + visible * 13 + 8, 0xAAAAAA);
        }
    }

    private void renderConfigBackground(GuiGraphicsExtractor g) {
        int cx     = this.width / 2;
        int labelX = cx - 120;

        int contentBottom = cfgRowY(CFG_ROW_COUNT);
        int hintY = contentBottom + CFG_HINT_PAD;
        int panelTop = cfgRowY(0) - CFG_TITLE_PAD - 10;
        int panelBottom = hintY + CFG_BOTTOM_PAD;
        g.fill(labelX - 4, panelTop, cx + 135, panelBottom, 0xEE000000);
    }

    private void renderConfigLabels(GuiGraphicsExtractor g) {
        ConfigManager.Config cfg = ConfigManager.get();
        int cx     = this.width / 2;
        int labelX = cx - 120;
        int off    = 3;

        int contentBottom = cfgRowY(CFG_ROW_COUNT);
        int hintY = contentBottom + CFG_HINT_PAD;
        int panelTop = cfgRowY(0) - CFG_TITLE_PAD - 10;

        TextDrawer.drawCenteredString(g,
            Component.translatable("gui.itemfinder.config_title").copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            cx, panelTop + 6, 0xFFFFFF);

        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_radius").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + cfg.radius + " ").withStyle(ChatFormatting.YELLOW))
                .append(Component.translatable("gui.itemfinder.config.blocks").copy().withStyle(ChatFormatting.GRAY)),
            labelX, cfgRowY(0) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_highlight_duration").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + cfg.highlightDurationSeconds + "s").withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(1) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_highlight_pulse").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.highlightPulse ? "ON" : "OFF")).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(2) + off, 0xFFFFFF);

        String sortLabel = Component.translatable(
            cfg.sortMode == 0 ? "gui.itemfinder.sort_distance" : "gui.itemfinder.sort_count"
        ).getString();
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_sort").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + sortLabel).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(3) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_chat").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.sendToChat ? "ON" : "OFF")).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(4) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_particle").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + (cfg.particleTrail ? "ON" : "OFF")).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(5) + off, 0xFFFFFF);
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_particle_type").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + cfg.particleType).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(6) + off, 0xFFFFFF);

        String langLabel = Component.translatable(
            cfg.searchByEnglish ? "gui.itemfinder.search_lang_english" : "gui.itemfinder.search_lang_game"
        ).getString();
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_search_lang").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + langLabel).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(7) + off, 0xFFFFFF);

        String enterLabel = Component.translatable(
            switch (cfg.enterMode) {
                case 1 -> "gui.itemfinder.enter_top";
                case 2 -> "gui.itemfinder.enter_all";
                default -> "gui.itemfinder.enter_off";
            }
        ).getString();
        TextDrawer.drawString(g,
            Component.translatable("gui.itemfinder.config_enter_mode").copy().withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + enterLabel).withStyle(ChatFormatting.YELLOW)),
            labelX, cfgRowY(8) + off, 0xFFFFFF);

        TextDrawer.drawCenteredString(g,
            Component.translatable("gui.itemfinder.config_save_hint").copy().withStyle(ChatFormatting.GRAY),
            cx, hintY, 0xAAAAAA);
    }

    private void updateButtonVisibility() {
        int i = 0;
        for (var widget : this.children()) {
            if (i > 3 && widget instanceof Button btn) {
                btn.visible = showingConfig;
            }
            i++;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}