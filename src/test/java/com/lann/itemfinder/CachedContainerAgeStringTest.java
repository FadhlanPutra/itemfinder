package com.lann.itemfinder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CachedContainerAgeStringTest {

    @Test
    void justOpened_showsJustNow() {
        CacheManager.CachedContainer cc = new CacheManager.CachedContainer("chest");
        cc.lastOpened = System.currentTimeMillis();
        Assertions.assertEquals("just now", cc.getAgeString());
    }

    @Test
    void fewMinutesAgo_showsMinutes() {
        CacheManager.CachedContainer cc = new CacheManager.CachedContainer("chest");
        cc.lastOpened = System.currentTimeMillis() - (5 * 60_000L); // 5 minutes ago
        Assertions.assertEquals("5m ago", cc.getAgeString());
    }

    @Test
    void fewHoursAgo_showsHours() {
        CacheManager.CachedContainer cc = new CacheManager.CachedContainer("chest");
        cc.lastOpened = System.currentTimeMillis() - (3 * 3_600_000L); // 3 hours ago
        Assertions.assertEquals("3h ago", cc.getAgeString());
    }

    @Test
    void fewDaysAgo_showsDays() {
        CacheManager.CachedContainer cc = new CacheManager.CachedContainer("chest");
        cc.lastOpened = System.currentTimeMillis() - (4 * 86_400_000L); // 4 days ago
        Assertions.assertEquals("4d ago", cc.getAgeString());
    }

    @Test
    void exactlyOneDayAgo_showsOneDayNotHours() {
        CacheManager.CachedContainer cc = new CacheManager.CachedContainer("chest");
        cc.lastOpened = System.currentTimeMillis() - 86_400_000L;
        Assertions.assertEquals("1d ago", cc.getAgeString());
    }

    @Test
    void justUnderOneMinute_showsJustNowNotZeroMinutes() {
        CacheManager.CachedContainer cc = new CacheManager.CachedContainer("chest");
        cc.lastOpened = System.currentTimeMillis() - 59_000L;
        Assertions.assertEquals("just now", cc.getAgeString(),
            "Expected 'just now' for a container opened just under one minute ago.");
    }
}