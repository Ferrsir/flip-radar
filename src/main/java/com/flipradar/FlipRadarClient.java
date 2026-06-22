package com.flipradar;

import com.flipradar.api.HypixelApiClient;
import com.flipradar.auction.AuctionPageFetcher;
import com.flipradar.config.ConfigManager;
import com.flipradar.data.LocalCache;
import com.flipradar.market.FlipCandidate;
import com.flipradar.market.FlipScanner;
import com.flipradar.safe.SafeAuctionOpener;
import com.flipradar.ui.FlipRadarScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;

public final class FlipRadarClient implements ClientModInitializer {
    public static final String MOD_ID = "flipradar";
    private static final NumberFormat COINS = NumberFormat.getIntegerInstance(Locale.US);

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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("flipradar")
                    .executes(context -> openScreen()));
            dispatcher.register(ClientCommandManager.literal("fr")
                    .executes(context -> openScreen()));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            scanner.refreshIfDue();

            for (FlipCandidate candidate : scanner.consumeNewAlerts()) {
                client.player.sendMessage(alertText(candidate), false);
            }
        });
    }

    private int openScreen() {
        MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().setScreen(new FlipRadarScreen(scanner, configManager, auctionOpener))
        );
        return 1;
    }

    private Text alertText(FlipCandidate candidate) {
        String command = "/viewauction " + candidate.auction().uuid();
        MutableText prefix = Text.literal("[FlipRadar] ").formatted(Formatting.DARK_PURPLE);
        MutableText body = Text.literal("flip +" + COINS.format(candidate.profitAfterTax()) + " ").formatted(Formatting.GREEN);
        MutableText link = Text.literal("[click here to open]")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.UNDERLINE)
                .styled(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open auction manually. You still inspect and buy."))));
        return prefix.append(body).append(link);
    }
}
