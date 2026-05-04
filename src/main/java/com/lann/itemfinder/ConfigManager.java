package com.lann.itemfinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("itemfinder.json");

    public static class Config {
        public int radius = 50;
        public int highlightDurationSeconds = 10;
        public boolean highlightPulse = true;
        public float highlightColorR = 1.0f;
        public float highlightColorG = 1.0f;
        public float highlightColorB = 0.0f;
        public boolean sendToChat = true;
        public int sortMode = 0; // 0 = jarak, 1 = jumlah item
        public boolean particleTrail = true;
        public boolean searchByEnglish = false; // true = pakai nama english (item ID), false = pakai nama bahasa game
        public boolean searchMode = false; // false = exact item pick, true = langsung scan semua yang cocok
        public String particleType = "END ROD"; // pilihan: FLAME, ENCHANT, END_ROD, SOUL_FIRE_FLAME, WITCH, DRAGON_BREATH
        public int enterMode = 1; // 0 = off, 1 = pilih teratas, 2 = scan all matches
    }

    private static Config current = new Config();

    public static Config get() {
        return current;
    }

    public static void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            current = GSON.fromJson(reader, Config.class);
            if (current == null) current = new Config();
        } catch (Exception e) {
            ItemFinderMod.LOGGER.warn("Failed to load config, using default: " + e.getMessage());
            current = new Config();
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(current, writer);
        } catch (Exception e) {
            ItemFinderMod.LOGGER.warn("Failed to save config: " + e.getMessage());
        }
    }
}
