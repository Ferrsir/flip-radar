package com.flipradar;

import com.flipradar.api.HypixelApiClient;
import com.flipradar.auction.AuctionPageFetcher;
import com.flipradar.config.ConfigManager;
import com.flipradar.data.LocalCache;
import com.flipradar.market.FlipScanner;
import com.flipradar.safe.SafeAuctionOpener;
import com.flipradar.ui.FlipRadarScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public final class FlipRadarClient implements ClientModInitializer {
    public static final String MOD_ID = "flipradar";

    private KeyBinding openKey;
    private FlipScanner scanner;
    private ConfigManager configManager;
    private SafeAuctionOpener auctionOpener;

    @Override
    public void onInitializeClient() {
        Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve(MOD_ID);
        LocalCache cache = new LocalCache(configDir.resolve("cache"));
        configManager = new ConfigManager(configDir.resolve("config.json"));
        auctionOpener = new SafeAuctionOpener();

        HypixelApiClient apiClient = new HypixelApiClient(cache);
        AuctionPageFetcher pageFetcher = new AuctionPageFetcher(apiClient);
        scanner = new FlipScanner(pageFetcher, cache, configManager);

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flipradar.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.flipradar"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new FlipRadarScreen(scanner, configManager, auctionOpener));
            }
        });
    }
}
