package com.lann.itemfinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;


public class ConfigSaveLoadTest {

    private Path configPath;
    private boolean originalExisted;
    private byte[] originalBytes;

    @BeforeEach
    void backupOriginalConfig() throws IOException {
        configPath = getConfigPath();
        originalExisted = Files.exists(configPath);
        if (originalExisted) {
            originalBytes = Files.readAllBytes(configPath);
        }
    }

    @AfterEach
    void restoreOriginalConfig() throws IOException {
        if (originalExisted) {
            Files.write(configPath, originalBytes);
        } else {
            Files.deleteIfExists(configPath);
        }
        ConfigManager.load();
    }

    @Test
    void allThirteenFieldsSurviveSaveAndLoad() {
        ConfigManager.Config cfg = ConfigManager.get();

        cfg.radius = 137;
        cfg.highlightDurationSeconds = 42;
        cfg.highlightPulse = false;
        cfg.highlightColorR = 0.25f;
        cfg.highlightColorG = 0.75f;
        cfg.highlightColorB = 0.5f;
        cfg.sendToChat = false;
        cfg.sortMode = 1;
        cfg.particleTrail = false;
        cfg.searchByEnglish = true;
        cfg.searchMode = true;
        cfg.particleType = "WITCH";
        cfg.enterMode = 2;

        ConfigManager.save();
        ConfigManager.load();

        ConfigManager.Config loaded = ConfigManager.get();

        Assertions.assertEquals(137, loaded.radius, "radius not saved/read correctly");
        Assertions.assertEquals(42, loaded.highlightDurationSeconds, "highlightDurationSeconds incorrect");
        Assertions.assertFalse(loaded.highlightPulse, "highlightPulse incorrect");
        Assertions.assertEquals(0.25f, loaded.highlightColorR, 0.0001f, "highlightColorR incorrect");
        Assertions.assertEquals(0.75f, loaded.highlightColorG, 0.0001f, "highlightColorG incorrect");
        Assertions.assertEquals(0.5f, loaded.highlightColorB, 0.0001f, "highlightColorB incorrect");
        Assertions.assertFalse(loaded.sendToChat, "sendToChat incorrect");
        Assertions.assertEquals(1, loaded.sortMode, "sortMode incorrect");
        Assertions.assertFalse(loaded.particleTrail, "particleTrail incorrect");
        Assertions.assertTrue(loaded.searchByEnglish, "searchByEnglish incorrect");
        Assertions.assertTrue(loaded.searchMode, "searchMode incorrect");
        Assertions.assertEquals("WITCH", loaded.particleType, "particleType incorrect");
        Assertions.assertEquals(2, loaded.enterMode, "enterMode incorrect");
    }

    @Test
    void missingConfigFileFallsBackToAllDefaults() throws IOException {
        Files.deleteIfExists(configPath);
        ConfigManager.load();
        ConfigManager.Config loaded = ConfigManager.get();

        Assertions.assertEquals(50, loaded.radius, "default radius changed / load() failed to fallback correctly");
        Assertions.assertEquals(10, loaded.highlightDurationSeconds, "default highlightDurationSeconds changed");
        Assertions.assertTrue(loaded.highlightPulse, "default highlightPulse changed");
        Assertions.assertTrue(loaded.sendToChat, "default sendToChat changed");
        Assertions.assertEquals(0, loaded.sortMode, "default sortMode changed");
        Assertions.assertTrue(loaded.particleTrail, "default particleTrail changed");
        Assertions.assertFalse(loaded.searchByEnglish, "default searchByEnglish changed");
        Assertions.assertFalse(loaded.searchMode, "default searchMode changed");
        Assertions.assertEquals("END ROD", loaded.particleType, "default particleType changed");
        Assertions.assertEquals(1, loaded.enterMode, "default enterMode changed");
    }

    @Test
    void corruptedConfigFileFallsBackToDefaultsWithoutCrashing() throws IOException {
        Files.write(configPath, "{ this is not valid json".getBytes());
        Assertions.assertDoesNotThrow(ConfigManager::load,
            "load() must remain safe (fallback to default) even with corrupt config file, not crash");

        ConfigManager.Config loaded = ConfigManager.get();
        Assertions.assertEquals(50, loaded.radius, "should fallback to default radius when file is corrupt");
    }

    private Path getConfigPath() {
        try {
            Field field = ConfigManager.class.getDeclaredField("CONFIG_PATH");
            field.setAccessible(true);
            return (Path) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Cannot access CONFIG_PATH via reflection", e);
        }
    }
}