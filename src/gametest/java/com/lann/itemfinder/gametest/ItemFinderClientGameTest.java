package com.lann.itemfinder.gametest;


import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

@SuppressWarnings("UnstableApiUsage")
public class ItemFinderClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {

            for (int i = 0; i < 40; i++) {
                context.waitTick();
            }

            context.takeScreenshot("01-world-baseline");

            for (int i = 0; i < 20; i++) {
                context.waitTick();
            }

            context.takeScreenshot("02-hud-after-mod-loaded");
        }
    }
}