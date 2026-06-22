package com.flipradar.ui;

import com.flipradar.config.ConfigManager;
import com.flipradar.market.FlipCandidate;
import com.flipradar.market.FlipScanner;
import com.flipradar.safe.SafeAuctionOpener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class FlipRadarScreen extends Screen {
    private static final int PURPLE = 0xFFB86CFF;
    private static final int DARK = 0xEE090B12;
    private static final int PANEL = 0xFF151827;
    private static final int LINE = 0xFF2E3150;
    private static final NumberFormat COINS = NumberFormat.getIntegerInstance(Locale.US);

    private final FlipScanner scanner;
    private final ConfigManager configManager;
    private final SafeAuctionOpener auctionOpener;
    private int selectedIndex = 0;

    public FlipRadarScreen(FlipScanner scanner, ConfigManager configManager, SafeAuctionOpener auctionOpener) {
        super(Text.literal("Flip Radar"));
        this.scanner = scanner;
        this.configManager = configManager;
        this.auctionOpener = auctionOpener;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal(scanner.isScanning() ? "Scanning..." : "Refresh Now"), button -> {
            scanner.refreshAsync();
            rebuild();
        }).dimensions(width - 126, 16, 108, 20).build());

        List<FlipCandidate> flips = scanner.latest();
        if (!flips.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Open Auction"), button -> {
                FlipCandidate candidate = flips.get(Math.min(selectedIndex, flips.size() - 1));
                auctionOpener.openAuctionManually(candidate.auction());
                close();
            }).dimensions(width - 154, height - 36, 136, 22).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), button -> client.setScreen(new FlipRadarSettingsScreen(configManager, this)))
                .dimensions(16, height - 36, 86, 22)
                .build());
    }

    private void rebuild() {
        clearChildren();
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, DARK);
        renderSidebar(context);
        renderHeader(context);
        renderTable(context, mouseX, mouseY);
        renderDetailPanel(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSidebar(DrawContext context) {
        context.fill(0, 0, 128, height, 0xFF0D1020);
        drawText(context, "Flip Radar", 16, 22, PURPLE);
        String[] nav = {"Watchlist", "Craft Flips", "Pet Flips", "Recent Sales", "Profit Tracker", "Settings", "About"};
        int y = 58;
        for (String item : nav) {
            drawText(context, item, 18, y, 0xFFABB2D6);
            y += 20;
        }
    }

    private void renderHeader(DrawContext context) {
        context.fill(128, 0, width, 58, 0xFF101422);
        drawText(context, "FLIP RADAR", 146, 14, PURPLE);
        drawText(context, "Find undervalued BIN auctions & flip manually", 146, 32, 0xFFABB2D6);
        String status = "API: " + scanner.apiStatus();
        drawText(context, status, width - 236, 42, "OK".equals(scanner.apiStatus()) ? 0xFF7CFFB2 : 0xFFFFD166);
        if (!scanner.lastScan().equals(java.time.Instant.EPOCH)) {
            String time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(scanner.lastScan());
            drawText(context, "Last scan " + time, width - 236, 28, 0xFFABB2D6);
        }
    }

    private void renderTable(DrawContext context, int mouseX, int mouseY) {
        int x = 144;
        int y = 76;
        int tableRight = width - 240;
        context.fill(x, y, tableRight, height - 58, PANEL);
        drawText(context, "Item", x + 8, y + 8, 0xFFFFFFFF);
        drawText(context, "BIN", x + 178, y + 8, 0xFFFFFFFF);
        drawText(context, "Value", x + 264, y + 8, 0xFFFFFFFF);
        drawText(context, "Profit", x + 354, y + 8, 0xFFFFFFFF);
        drawText(context, "%", x + 444, y + 8, 0xFFFFFFFF);
        drawText(context, "Conf", x + 490, y + 8, 0xFFFFFFFF);

        List<FlipCandidate> flips = scanner.latest();
        if (flips.isEmpty()) {
            drawText(context, "No flips yet. Press Refresh Now.", x + 8, y + 38, 0xFFABB2D6);
            return;
        }

        int rowY = y + 28;
        int maxRows = Math.min(flips.size(), Math.max(1, (height - rowY - 68) / 18));
        for (int i = 0; i < maxRows; i++) {
            FlipCandidate flip = flips.get(i);
            boolean hovered = mouseX >= x && mouseX <= tableRight && mouseY >= rowY && mouseY <= rowY + 17;
            if (hovered) {
                selectedIndex = i;
            }
            context.fill(x + 1, rowY, tableRight - 1, rowY + 17, i == selectedIndex ? 0xFF2B1E46 : 0x00111111);
            drawText(context, truncate(flip.auction().itemName(), 26), x + 8, rowY + 5, 0xFFE8EAF6);
            drawText(context, coinText(flip.auction().binPrice()), x + 178, rowY + 5, 0xFFCED6FF);
            drawText(context, coinText(flip.estimatedMarketValue()), x + 264, rowY + 5, 0xFFCED6FF);
            drawText(context, "+" + coinText(flip.profitAfterTax()), x + 354, rowY + 5, 0xFF7CFFB2);
            drawText(context, String.format(Locale.US, "%.1f", flip.profitPercent()), x + 444, rowY + 5, 0xFF7CFFB2);
            drawText(context, flip.confidencePercent() + "%", x + 490, rowY + 5, 0xFFFFD166);
            context.fill(x + 1, rowY + 17, tableRight - 1, rowY + 18, LINE);
            rowY += 18;
        }
    }

    private void renderDetailPanel(DrawContext context) {
        int x = width - 222;
        int y = 76;
        context.fill(x, y, width - 18, height - 58, PANEL);
        List<FlipCandidate> flips = scanner.latest();
        if (flips.isEmpty()) {
            drawText(context, "Details", x + 12, y + 12, PURPLE);
            return;
        }

        FlipCandidate flip = flips.get(Math.min(selectedIndex, flips.size() - 1));
        int line = y + 14;
        drawText(context, truncate(flip.auction().itemName(), 28), x + 12, line, PURPLE);
        line += 22;
        drawText(context, "Rarity: " + flip.auction().tier(), x + 12, line, 0xFFABB2D6);
        line += 18;
        drawText(context, "BIN: " + coinText(flip.auction().binPrice()), x + 12, line, 0xFFE8EAF6);
        line += 18;
        drawText(context, "Value: " + coinText(flip.estimatedMarketValue()), x + 12, line, 0xFFE8EAF6);
        line += 18;
        drawText(context, "Tax: " + coinText(flip.estimatedAuctionTax()), x + 12, line, 0xFFE8EAF6);
        line += 18;
        drawText(context, "Profit: +" + coinText(flip.profitAfterTax()), x + 12, line, 0xFF7CFFB2);
        line += 18;
        drawText(context, "Profit %: " + String.format(Locale.US, "%.1f%%", flip.profitPercent()), x + 12, line, 0xFF7CFFB2);
        line += 18;
        drawText(context, "Confidence: " + flip.confidencePercent() + "%", x + 12, line, 0xFFFFD166);
        line += 18;
        drawText(context, "Volume: " + String.format(Locale.US, "%.0f/day", flip.volumePerDay()), x + 12, line, 0xFFABB2D6);
        line += 18;
        drawText(context, "Age: " + flip.ageMinutes() + "m", x + 12, line, 0xFFABB2D6);
        line += 26;
        drawText(context, "Manual only. Inspect before buying.", x + 12, line, 0xFFFFD166);
    }

    private void drawText(DrawContext context, String value, int x, int y, int color) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        context.drawText(minecraft.textRenderer, Text.literal(value), x, y, color, false);
    }

    private String coinText(long coins) {
        return COINS.format(coins);
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "...";
    }
}
