package com.flipradar.ui;

import com.flipradar.config.ConfigManager;
import com.flipradar.market.FlipCandidate;
import com.flipradar.market.FlipScanner;
import com.flipradar.safe.SafeAuctionOpener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
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
    private static final int SIDEBAR = 116;
    private static final int HEADER = 54;
    private static final String[] NAV = {"Flip Radar", "Watchlist", "Craft Flips", "Pet Flips", "Recent Sales", "Profit Tracker", "Settings", "About"};
    private static final NumberFormat COINS = NumberFormat.getIntegerInstance(Locale.US);

    private final FlipScanner scanner;
    private final ConfigManager configManager;
    private final SafeAuctionOpener auctionOpener;
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private String activeTab = "Flip Radar";

    public FlipRadarScreen(FlipScanner scanner, ConfigManager configManager, SafeAuctionOpener auctionOpener) {
        super(Text.literal("Flip Radar"));
        this.scanner = scanner;
        this.configManager = configManager;
        this.auctionOpener = auctionOpener;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal(scanner.isScanning() ? "Scanning..." : "Refresh"), button -> {
            scanner.refreshAsync();
            rebuild();
        }).dimensions(width - 98, 14, 78, 20).build());

        if (!displayedFlips().isEmpty()) {
            int detailWidth = detailWidth();
            int openX = detailWidth > 0 ? width - detailWidth + 12 : width - 142;
            addDrawableChild(ButtonWidget.builder(Text.literal("Open Auction"), button -> {
                List<FlipCandidate> flips = displayedFlips();
                FlipCandidate candidate = flips.get(Math.min(selectedIndex, flips.size() - 1));
                auctionOpener.openAuctionManually(candidate.auction());
                close();
            }).dimensions(openX, height - 30, Math.min(124, detailWidth - 24), 20).build());
        }
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
        renderMain(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        if (mouseX < SIDEBAR) {
            int navY = 38;
            for (String item : NAV) {
                if (mouseY >= navY - 5 && mouseY <= navY + 14) {
                    if ("Settings".equals(item)) {
                        client.setScreen(new FlipRadarSettingsScreen(configManager, this));
                    } else {
                        activeTab = item;
                        selectedIndex = 0;
                        scrollOffset = 0;
                        rebuild();
                    }
                    return true;
                }
                navY += 22;
            }
        }

        int tableX = SIDEBAR + 12;
        int tableY = HEADER + 16;
        int rowTop = tableY + 25;
        int tableRight = tableRight();
        if (mouseX >= tableX && mouseX <= tableRight && mouseY >= rowTop && mouseY <= height - 42) {
            int row = ((int) mouseY - rowTop) / 16;
            int index = scrollOffset + row;
            if (index >= 0 && index < displayedFlips().size()) {
                selectedIndex = index;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<FlipCandidate> flips = displayedFlips();
        int maxVisible = visibleRows();
        int maxScroll = Math.max(0, flips.size() - maxVisible);
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount) * 3));
            selectedIndex = Math.max(scrollOffset, Math.min(selectedIndex, scrollOffset + maxVisible - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderSidebar(DrawContext context) {
        context.fill(0, 0, SIDEBAR, height, 0xFF0D1020);
        int y = 38;
        for (String item : NAV) {
            boolean selected = item.equals(activeTab);
            if (selected) {
                context.fill(10, y - 7, SIDEBAR - 10, y + 16, 0xFF23183A);
            }
            drawText(context, item, 14, y, selected ? PURPLE : 0xFFABB2D6);
            y += 22;
        }
    }

    private void renderHeader(DrawContext context) {
        context.fill(SIDEBAR, 0, width, HEADER, 0xFF101422);
        drawText(context, "FLIP RADAR", SIDEBAR + 14, 12, PURPLE);
        drawText(context, "Manual BIN scan", SIDEBAR + 14, 30, 0xFFABB2D6);

        String status = "API: " + scanner.apiStatus();
        drawText(context, status, Math.max(SIDEBAR + 220, width - 220), 30, "OK".equals(scanner.apiStatus()) ? 0xFF7CFFB2 : 0xFFFFD166);
        if (!scanner.lastScan().equals(java.time.Instant.EPOCH)) {
            String time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(scanner.lastScan());
            drawText(context, "Last scan " + time, Math.max(SIDEBAR + 220, width - 220), 14, 0xFFABB2D6);
        }
    }

    private void renderMain(DrawContext context) {
        if ("About".equals(activeTab)) {
            renderInfo(context, "Flip Radar is informational only. It never buys, clicks slots, or confirms purchases.");
            return;
        }
        if ("Recent Sales".equals(activeTab) || "Profit Tracker".equals(activeTab) || "Craft Flips".equals(activeTab)) {
            renderInfo(context, activeTab + " is planned. Main BIN scanning is live.");
            return;
        }

        renderTable(context);
        renderDetailPanel(context);
    }

    private void renderInfo(DrawContext context, String message) {
        int x = SIDEBAR + 14;
        int y = HEADER + 18;
        context.fill(x, y, width - 16, height - 16, PANEL);
        drawText(context, activeTab, x + 12, y + 12, PURPLE);
        drawText(context, message, x + 12, y + 34, 0xFFE8EAF6);
    }

    private void renderTable(DrawContext context) {
        int x = SIDEBAR + 12;
        int y = HEADER + 16;
        int right = tableRight();
        int bottom = height - 38;
        context.fill(x, y, right, bottom, PANEL);

        int widthAvailable = Math.max(260, right - x - 16);
        int itemW = Math.max(110, widthAvailable - 322);
        int binX = x + 8 + itemW + 8;
        int valueX = binX + 78;
        int profitX = valueX + 86;
        int pctX = profitX + 84;
        int confX = pctX + 42;

        drawText(context, "Item", x + 8, y + 8, 0xFFFFFFFF);
        drawText(context, "BIN", binX, y + 8, 0xFFFFFFFF);
        drawText(context, "Value", valueX, y + 8, 0xFFFFFFFF);
        drawText(context, "Profit", profitX, y + 8, 0xFFFFFFFF);
        drawText(context, "%", pctX, y + 8, 0xFFFFFFFF);
        drawText(context, "Conf", confX, y + 8, 0xFFFFFFFF);

        List<FlipCandidate> flips = displayedFlips();
        if (flips.isEmpty()) {
            drawText(context, "No flips in this tab. Run /fr and press Refresh.", x + 8, y + 36, 0xFFABB2D6);
            return;
        }

        int rowY = y + 25;
        int maxRows = visibleRows();
        for (int i = 0; i < maxRows && scrollOffset + i < flips.size(); i++) {
            int index = scrollOffset + i;
            FlipCandidate flip = flips.get(index);
            context.fill(x + 1, rowY, right - 1, rowY + 15, index == selectedIndex ? 0xFF2B1E46 : 0x00111111);
            drawText(context, trim(flip.auction().itemName(), itemW), x + 8, rowY + 4, 0xFFE8EAF6);
            drawText(context, compactCoins(flip.auction().binPrice()), binX, rowY + 4, 0xFFCED6FF);
            drawText(context, compactCoins(flip.estimatedMarketValue()), valueX, rowY + 4, 0xFFCED6FF);
            drawText(context, "+" + compactCoins(flip.profitAfterTax()), profitX, rowY + 4, 0xFF7CFFB2);
            drawText(context, String.format(Locale.US, "%.0f", flip.profitPercent()), pctX, rowY + 4, 0xFF7CFFB2);
            drawText(context, flip.confidencePercent() + "%", confX, rowY + 4, 0xFFFFD166);
            context.fill(x + 1, rowY + 15, right - 1, rowY + 16, LINE);
            rowY += 16;
        }

        if (flips.size() > maxRows) {
            drawText(context, (scrollOffset + 1) + "-" + Math.min(flips.size(), scrollOffset + maxRows) + " / " + flips.size(), x + 8, bottom + 10, 0xFFABB2D6);
        }
    }

    private void renderDetailPanel(DrawContext context) {
        int detailWidth = detailWidth();
        if (detailWidth <= 0) {
            return;
        }

        int x = width - detailWidth;
        int y = HEADER + 16;
        context.fill(x, y, width - 12, height - 38, PANEL);
        List<FlipCandidate> flips = displayedFlips();
        if (flips.isEmpty()) {
            drawText(context, "Details", x + 12, y + 12, PURPLE);
            return;
        }

        FlipCandidate flip = flips.get(Math.min(selectedIndex, flips.size() - 1));
        int textW = detailWidth - 28;
        int line = y + 12;
        drawText(context, trim(flip.auction().itemName(), textW), x + 12, line, PURPLE);
        line += 20;
        drawText(context, "Rarity: " + flip.auction().tier(), x + 12, line, 0xFFABB2D6);
        line += 16;
        drawText(context, "BIN: " + coinText(flip.auction().binPrice()), x + 12, line, 0xFFE8EAF6);
        line += 16;
        drawText(context, "Value: " + coinText(flip.estimatedMarketValue()), x + 12, line, 0xFFE8EAF6);
        line += 16;
        drawText(context, "Tax: " + coinText(flip.estimatedAuctionTax()), x + 12, line, 0xFFE8EAF6);
        line += 16;
        drawText(context, "Profit: +" + coinText(flip.profitAfterTax()), x + 12, line, 0xFF7CFFB2);
        line += 16;
        drawText(context, "Profit %: " + String.format(Locale.US, "%.1f%%", flip.profitPercent()), x + 12, line, 0xFF7CFFB2);
        line += 16;
        drawText(context, "Confidence: " + flip.confidencePercent() + "%", x + 12, line, 0xFFFFD166);
        line += 16;
        drawText(context, "Volume: " + String.format(Locale.US, "%.0f/day", flip.volumePerDay()), x + 12, line, 0xFFABB2D6);
        line += 16;
        drawText(context, "Age: " + flip.ageMinutes() + "m", x + 12, line, 0xFFABB2D6);
        line += 22;
        drawText(context, trim("Manual only. Inspect before buying.", textW), x + 12, line, 0xFFFFD166);
    }

    private List<FlipCandidate> displayedFlips() {
        List<FlipCandidate> flips = scanner.latest();
        if ("Pet Flips".equals(activeTab)) {
            return flips.stream().filter(flip -> flip.auction().itemName().startsWith("[Lvl ")).toList();
        }
        if ("Watchlist".equals(activeTab)) {
            if (configManager.get().whitelistItems.isEmpty()) {
                return List.of();
            }
            return flips.stream()
                    .filter(flip -> configManager.get().whitelistItems.stream()
                            .anyMatch(item -> flip.auction().itemName().toLowerCase(Locale.ROOT).contains(item.toLowerCase(Locale.ROOT))))
                    .toList();
        }
        return flips;
    }

    private int detailWidth() {
        return width >= 720 ? Math.min(250, Math.max(190, width / 4)) : 0;
    }

    private int tableRight() {
        int detailWidth = detailWidth();
        return detailWidth > 0 ? width - detailWidth - 12 : width - 12;
    }

    private int visibleRows() {
        return Math.max(1, (height - (HEADER + 16 + 25) - 48) / 16);
    }

    private void drawText(DrawContext context, String value, int x, int y, int color) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        context.drawText(minecraft.textRenderer, Text.literal(value), x, y, color, false);
    }

    private String trim(String value, int maxWidth) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        return minecraft.textRenderer.trimToWidth(value, Math.max(0, maxWidth - minecraft.textRenderer.getWidth(ellipsis))) + ellipsis;
    }

    private String coinText(long coins) {
        return COINS.format(coins);
    }

    private String compactCoins(long coins) {
        if (coins >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fb", coins / 1_000_000_000.0D);
        }
        if (coins >= 1_000_000L) {
            return String.format(Locale.US, "%.1fm", coins / 1_000_000.0D);
        }
        if (coins >= 1_000L) {
            return String.format(Locale.US, "%.0fk", coins / 1_000.0D);
        }
        return COINS.format(coins);
    }
}
