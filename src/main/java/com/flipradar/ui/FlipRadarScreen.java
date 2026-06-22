package com.flipradar.ui;

import com.flipradar.config.ConfigManager;
import com.flipradar.config.FlipRadarConfig;
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
    private static final int BG = 0xEE080A12;
    private static final int SIDEBAR_BG = 0xFF0C0F1D;
    private static final int HEADER_BG = 0xFF101421;
    private static final int PANEL = 0xFF151927;
    private static final int PANEL_SOFT = 0xFF1A1F31;
    private static final int ROW_ALT = 0xFF111625;
    private static final int ROW_SELECTED = 0xFF2B1E46;
    private static final int LINE = 0xFF2E3150;
    private static final int PURPLE = 0xFFB86CFF;
    private static final int TEXT = 0xFFE8EAF6;
    private static final int MUTED = 0xFFABB2D6;
    private static final int GREEN = 0xFF7CFFB2;
    private static final int YELLOW = 0xFFFFD166;
    private static final int RED = 0xFFFF6B7A;
    private static final int SIDEBAR = 126;
    private static final int HEADER = 64;
    private static final String[] NAV = {"Flip Radar", "Watchlist", "Pet Flips", "Recent Sales", "Profit Tracker", "Settings", "About"};
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
        }).dimensions(width - 98, 18, 78, 20).build());

        if (!displayedFlips().isEmpty() && detailWidth() > 0) {
            int detailX = width - detailWidth() - 12;
            addDrawableChild(ButtonWidget.builder(Text.literal("Open Auction"), button -> {
                List<FlipCandidate> flips = displayedFlips();
                FlipCandidate candidate = flips.get(Math.min(selectedIndex, flips.size() - 1));
                auctionOpener.openAuctionManually(candidate.auction());
                close();
            }).dimensions(detailX + 16, height - 34, detailWidth() - 32, 22).build());
        }
    }

    private void rebuild() {
        clearChildren();
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        renderSidebar(context);
        renderHeader(context);
        renderContent(context);
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
            int navY = 76;
            for (String item : NAV) {
                if (mouseY >= navY - 6 && mouseY <= navY + 15) {
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
                navY += 24;
            }
        }

        int rowTop = tableY() + 30;
        if (mouseX >= tableX() && mouseX <= tableRight() && mouseY >= rowTop && mouseY <= tableBottom()) {
            int row = ((int) mouseY - rowTop) / rowHeight();
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
        int maxScroll = Math.max(0, flips.size() - visibleRows());
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount) * 3));
            selectedIndex = Math.max(scrollOffset, Math.min(selectedIndex, scrollOffset + visibleRows() - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderSidebar(DrawContext context) {
        context.fill(0, 0, SIDEBAR, height, SIDEBAR_BG);
        drawText(context, "FLIP", 18, 22, PURPLE);
        drawText(context, "RADAR", 18, 36, PURPLE);
        drawText(context, "safe manual scan", 18, 54, 0xFF6F779F);

        int y = 76;
        for (String item : NAV) {
            boolean selected = item.equals(activeTab);
            if (selected) {
                context.fill(12, y - 7, SIDEBAR - 12, y + 16, 0xFF241A3C);
            }
            drawText(context, item, 18, y, selected ? PURPLE : MUTED);
            y += 24;
        }
    }

    private void renderHeader(DrawContext context) {
        context.fill(SIDEBAR, 0, width, HEADER, HEADER_BG);
        int x = SIDEBAR + 14;
        drawText(context, "Flip Radar", x, 13, PURPLE);
        drawText(context, scanner.isScanning() ? "Live scan running" : "Live scan idle", x, 30, MUTED);
        drawText(context, "Found " + scanner.candidatesFound() + " | Scanned " + scanner.auctionsScanned() + " | Filtered " + scanner.candidatesFilteredOut(), x, 47, 0xFF7F87AD);

        int statusX = Math.max(x + 300, width - 270);
        String status = scanner.isScanning()
                ? "Page " + scanner.currentPage() + "/" + Math.max(scanner.currentPage(), scanner.totalPages())
                : "API " + scanner.apiStatus();
        drawText(context, status, statusX, 22, "OK".equals(scanner.apiStatus()) ? GREEN : YELLOW);
        if (!scanner.lastScan().equals(java.time.Instant.EPOCH)) {
            String time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(scanner.lastScan());
            drawText(context, "Last scan " + time, statusX, 39, MUTED);
        }
    }

    private void renderContent(DrawContext context) {
        if ("About".equals(activeTab)) {
            renderInfo(context, "Informational only. No auto-buy, no slot clicking, no confirmations.");
            return;
        }
        if ("Recent Sales".equals(activeTab) || "Profit Tracker".equals(activeTab)) {
            renderInfo(context, activeTab + " will use stored sales history after NBT decoding is added.");
            return;
        }

        renderFilterStrip(context);
        renderTable(context);
        renderDetailPanel(context);
    }

    private void renderFilterStrip(DrawContext context) {
        int x = tableX();
        int y = HEADER + 12;
        int right = tableRight();
        context.fill(x, y, right, y + 26, PANEL_SOFT);
        FlipRadarConfig config = configManager.get();
        String filters = "Min profit " + compactCoins(config.minimumProfitCoins)
                + " | Min " + String.format(Locale.US, "%.0f%%", config.minimumProfitPercent)
                + " | Conf " + config.minimumConfidencePercent + "%"
                + " | Vol " + String.format(Locale.US, "%.0f/day", config.minimumVolumePerDay)
                + " | Max buy " + compactCoins(config.maxPurchasePriceCoins);
        drawText(context, trim(filters, right - x - 16), x + 8, y + 9, MUTED);
    }

    private void renderTable(DrawContext context) {
        int x = tableX();
        int y = tableY();
        int right = tableRight();
        int bottom = tableBottom();
        context.fill(x, y, right, bottom, PANEL);

        int tableWidth = right - x - 16;
        int itemW = Math.max(120, tableWidth - 330);
        int binX = x + 8 + itemW + 10;
        int valueX = binX + 70;
        int profitX = valueX + 78;
        int pctX = profitX + 78;
        int confX = pctX + 48;

        drawText(context, "Item", x + 8, y + 10, TEXT);
        drawText(context, "BIN", binX, y + 10, TEXT);
        drawText(context, "Est.", valueX, y + 10, TEXT);
        drawText(context, "Profit", profitX, y + 10, TEXT);
        drawText(context, "%", pctX, y + 10, TEXT);
        drawText(context, "Conf", confX, y + 10, TEXT);

        List<FlipCandidate> flips = displayedFlips();
        if (flips.isEmpty()) {
            String message = scanner.isScanning()
                    ? "Scanning live auctions... " + scanner.auctionsScanned() + " checked so far."
                    : "No flips match these filters yet.";
            drawText(context, message, x + 10, y + 42, MUTED);
            drawText(context, "Tip: lower min profit/confidence or wait for the next live scan.", x + 10, y + 60, 0xFF7F87AD);
            return;
        }

        int rowY = y + 30;
        for (int i = 0; i < visibleRows() && scrollOffset + i < flips.size(); i++) {
            int index = scrollOffset + i;
            FlipCandidate flip = flips.get(index);
            boolean selected = index == selectedIndex;
            context.fill(x + 1, rowY, right - 1, rowY + rowHeight() - 1, selected ? ROW_SELECTED : (index % 2 == 0 ? ROW_ALT : PANEL));
            int estimateColor = suspicious(flip) ? YELLOW : MUTED;
            drawText(context, trim(flip.auction().itemName(), itemW), x + 8, rowY + 5, TEXT);
            drawText(context, compactCoins(flip.auction().binPrice()), binX, rowY + 5, MUTED);
            drawText(context, compactCoins(flip.estimatedMarketValue()), valueX, rowY + 5, estimateColor);
            drawText(context, "+" + compactCoins(flip.profitAfterTax()), profitX, rowY + 5, suspicious(flip) ? YELLOW : GREEN);
            drawText(context, percentText(flip.profitPercent()), pctX, rowY + 5, suspicious(flip) ? YELLOW : GREEN);
            drawText(context, flip.confidencePercent() + "%", confX, rowY + 5, confidenceColor(flip.confidencePercent()));
            context.fill(x + 1, rowY + rowHeight() - 1, right - 1, rowY + rowHeight(), LINE);
            rowY += rowHeight();
        }

        renderFooter(context, x, bottom);
    }

    private void renderFooter(DrawContext context, int x, int bottom) {
        List<FlipCandidate> flips = displayedFlips();
        String text = flips.isEmpty()
                ? "0 results"
                : (scrollOffset + 1) + "-" + Math.min(flips.size(), scrollOffset + visibleRows()) + " / " + flips.size();
        drawText(context, text + " | mouse wheel scroll | click a row for details", x + 8, bottom + 10, MUTED);
    }

    private void renderDetailPanel(DrawContext context) {
        int detailWidth = detailWidth();
        if (detailWidth <= 0) {
            return;
        }

        int x = width - detailWidth - 12;
        int y = HEADER + 12;
        int bottom = height - 44;
        context.fill(x, y, width - 12, bottom, PANEL);
        List<FlipCandidate> flips = displayedFlips();
        if (flips.isEmpty()) {
            drawText(context, "Details", x + 14, y + 14, PURPLE);
            drawText(context, "Select a flip to inspect pricing.", x + 14, y + 34, MUTED);
            return;
        }

        FlipCandidate flip = flips.get(Math.min(selectedIndex, flips.size() - 1));
        int textW = detailWidth - 28;
        int line = y + 14;
        drawText(context, trim(flip.auction().itemName(), textW), x + 14, line, PURPLE);
        line += 20;
        drawText(context, "Rarity " + flip.auction().tier(), x + 14, line, MUTED);
        line += 18;
        context.fill(x + 12, line, width - 24, line + 1, LINE);
        line += 12;

        line = metric(context, x, line, "BIN", coinText(flip.auction().binPrice()), MUTED);
        line = metric(context, x, line, "Estimated value", coinText(flip.estimatedMarketValue()), suspicious(flip) ? YELLOW : TEXT);
        line = metric(context, x, line, "Auction tax", coinText(flip.estimatedAuctionTax()), MUTED);
        line = metric(context, x, line, "Profit", "+" + coinText(flip.profitAfterTax()), suspicious(flip) ? YELLOW : GREEN);
        line = metric(context, x, line, "Profit percent", percentText(flip.profitPercent()) + "%", suspicious(flip) ? YELLOW : GREEN);
        line = metric(context, x, line, "Confidence", flip.confidencePercent() + "%", confidenceColor(flip.confidencePercent()));
        line = metric(context, x, line, "Volume", String.format(Locale.US, "%.0f/day", flip.volumePerDay()), MUTED);
        line = metric(context, x, line, "Age", flip.ageMinutes() + "m", flip.ageMinutes() > configManager.get().maxAuctionAgeMinutes ? YELLOW : MUTED);

        line += 8;
        drawText(context, "Pricing basis", x + 14, line, TEXT);
        line += 16;
        String basis = "Lowest comparable BIN cluster. Outliers ignored.";
        drawText(context, trim(basis, textW), x + 14, line, MUTED);
        line += 16;
        if (suspicious(flip)) {
            drawText(context, trim("Warning: estimate is unusually high. Inspect manually.", textW), x + 14, line, YELLOW);
            line += 16;
        }
        drawText(context, trim("Manual only. No buying or slot clicking.", textW), x + 14, Math.min(line + 8, bottom - 22), YELLOW);
    }

    private int metric(DrawContext context, int x, int y, String label, String value, int valueColor) {
        drawText(context, label, x + 14, y, 0xFF7F87AD);
        drawRightText(context, value, width - 28, y, valueColor);
        return y + 16;
    }

    private void renderInfo(DrawContext context, String message) {
        int x = tableX();
        int y = HEADER + 12;
        context.fill(x, y, width - 12, height - 16, PANEL);
        drawText(context, activeTab, x + 14, y + 14, PURPLE);
        drawText(context, message, x + 14, y + 36, TEXT);
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

    private int tableX() {
        return SIDEBAR + 14;
    }

    private int tableY() {
        return HEADER + 46;
    }

    private int tableRight() {
        int detailWidth = detailWidth();
        return detailWidth > 0 ? width - detailWidth - 26 : width - 14;
    }

    private int tableBottom() {
        return height - 34;
    }

    private int detailWidth() {
        return width >= 780 ? Math.min(270, Math.max(220, width / 4)) : 0;
    }

    private int rowHeight() {
        return 18;
    }

    private int visibleRows() {
        return Math.max(1, (tableBottom() - (tableY() + 30)) / rowHeight());
    }

    private int confidenceColor(int confidence) {
        if (confidence >= 80) {
            return GREEN;
        }
        if (confidence >= 65) {
            return YELLOW;
        }
        return RED;
    }

    private boolean suspicious(FlipCandidate flip) {
        return flip.profitPercent() > 250.0D || flip.estimatedMarketValue() > flip.auction().binPrice() * 4L;
    }

    private void drawText(DrawContext context, String value, int x, int y, int color) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        context.drawText(minecraft.textRenderer, Text.literal(value), x, y, color, false);
    }

    private void drawRightText(DrawContext context, String value, int rightX, int y, int color) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        context.drawText(minecraft.textRenderer, Text.literal(value), rightX - minecraft.textRenderer.getWidth(value), y, color, false);
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

    private String percentText(double percent) {
        if (percent >= 1000.0D) {
            return "999+";
        }
        return String.format(Locale.US, "%.0f", percent);
    }

    private String compactCoins(long coins) {
        if (coins >= 1_000_000_000L) {
            return String.format(Locale.US, "%.2fb", coins / 1_000_000_000.0D);
        }
        if (coins >= 10_000_000L) {
            return String.format(Locale.US, "%.1fm", coins / 1_000_000.0D);
        }
        if (coins >= 1_000_000L) {
            return String.format(Locale.US, "%.2fm", coins / 1_000_000.0D);
        }
        if (coins >= 1_000L) {
            return String.format(Locale.US, "%.0fk", coins / 1_000.0D);
        }
        return COINS.format(coins);
    }
}
