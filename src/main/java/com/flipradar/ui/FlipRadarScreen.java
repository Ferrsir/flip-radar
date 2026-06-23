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
    private static final int BG = 0xEE07090F;
    private static final int PANEL = 0xF012151D;
    private static final int PANEL_2 = 0xF0181C27;
    private static final int PANEL_3 = 0xF01F162E;
    private static final int LINE = 0xFF2B3143;
    private static final int PURPLE = 0xFFB86CFF;
    private static final int BLUE = 0xFF42B9FF;
    private static final int GREEN = 0xFF73E35A;
    private static final int YELLOW = 0xFFFFC247;
    private static final int RED = 0xFFFF5555;
    private static final int TEXT = 0xFFE8EAF6;
    private static final int MUTED = 0xFFABB2D6;
    private static final int SOFT = 0xFF7680A6;
    private static final int SIDEBAR = 154;
    private static final int GAP = 6;
    private static final int TOP = 8;
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
        int headerX = mainX();
        int headerW = mainW();
        addDrawableChild(ButtonWidget.builder(Text.literal(scanner.isScanning() ? "Scanning..." : "Refresh Now"), button -> {
            scanner.refreshAsync();
            rebuild();
        }).dimensions(headerX + headerW - 106, TOP + 14, 96, 22).build());

        if (!displayedFlips().isEmpty() && rightW() > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Open Auction"), button -> {
                List<FlipCandidate> flips = displayedFlips();
                FlipCandidate candidate = flips.get(Math.min(selectedIndex, flips.size() - 1));
                auctionOpener.openAuctionManually(candidate.auction());
                close();
            }).dimensions(rightX() + 12, height - 48, rightW() - 24, 24).build());
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
        renderChips(context);
        renderMain(context);
        renderRightPanel(context);
        renderBottomBar(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int navY = 42;
        if (mouseX >= 8 && mouseX <= SIDEBAR - 8) {
            for (String item : NAV) {
                if (mouseY >= navY - 5 && mouseY <= navY + 15) {
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
                navY += 28;
            }
        }

        int rowTop = tableY() + 28;
        if (mouseX >= tableX() && mouseX <= tableRight() && mouseY >= rowTop && mouseY <= tableBottom()) {
            int row = ((int) mouseY - rowTop) / rowH();
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
        int maxScroll = Math.max(0, displayedFlips().size() - visibleRows());
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount) * 2));
            selectedIndex = Math.max(scrollOffset, Math.min(selectedIndex, scrollOffset + visibleRows() - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderSidebar(DrawContext context) {
        panel(context, 6, TOP, SIDEBAR - 12, height - 48);
        int y = 42;
        for (String item : NAV) {
            boolean selected = item.equals(activeTab);
            if (selected) {
                context.fill(12, y - 7, SIDEBAR - 12, y + 16, 0xFF27193B);
            }
            drawText(context, navIcon(item) + "  " + item, 18, y, selected ? PURPLE : MUTED);
            y += 28;
        }

        int filterY = Math.min(height - 168, y + 8);
        renderFilterCard(context, filterY);
        renderApiCard(context, height - 104);
    }

    private void renderFilterCard(DrawContext context, int y) {
        if (y < 250 || y + 112 > height - 108) {
            return;
        }
        FlipRadarConfig config = configManager.get();
        panel(context, 8, y, SIDEBAR - 16, 104);
        drawText(context, "FILTERS", 18, y + 12, PURPLE);
        drawText(context, "Min Profit", 18, y + 30, MUTED);
        drawRight(context, compactCoins(config.minimumProfitCoins), SIDEBAR - 18, y + 30, YELLOW);
        drawText(context, "Min Profit %", 18, y + 46, MUTED);
        drawRight(context, String.format(Locale.US, "%.0f%%", config.minimumProfitPercent), SIDEBAR - 18, y + 46, YELLOW);
        drawText(context, "Confidence", 18, y + 62, MUTED);
        drawRight(context, config.minimumConfidencePercent + "%", SIDEBAR - 18, y + 62, YELLOW);
        drawText(context, "Volume", 18, y + 78, MUTED);
        drawRight(context, String.format(Locale.US, "%.0f/day", config.minimumVolumePerDay), SIDEBAR - 18, y + 78, YELLOW);
    }

    private void renderApiCard(DrawContext context, int y) {
        panel(context, 8, y, SIDEBAR - 16, 76);
        drawText(context, "API Status", 18, y + 12, TEXT);
        drawText(context, scanner.isScanning() ? "* Scanning" : "* " + scanner.apiStatus(), 18, y + 30, "OK".equals(scanner.apiStatus()) ? GREEN : YELLOW);
        drawText(context, "Page " + scanner.currentPage() + "/" + Math.max(scanner.currentPage(), scanner.totalPages()), 18, y + 46, MUTED);
        drawText(context, "Scanned " + scanner.auctionsScanned(), 18, y + 62, MUTED);
    }

    private void renderHeader(DrawContext context) {
        int x = mainX();
        int w = mainW();
        panel(context, x, TOP, w, 56);
        context.fill(x + 12, TOP + 13, x + 36, TOP + 37, 0xFF30194A);
        drawCentered(context, "R", x + 24, TOP + 21, PURPLE);
        drawText(context, "FLIP RADAR", x + 46, TOP + 12, PURPLE);
        drawText(context, "Find undervalued BIN auctions and flip manually", x + 46, TOP + 31, MUTED);
        String scan = scanner.isScanning()
                ? "Live scan page " + scanner.currentPage() + "/" + Math.max(scanner.currentPage(), scanner.totalPages())
                : "Last scan " + lastScanText();
        drawRight(context, scan, x + w - 122, TOP + 18, scanner.isScanning() ? YELLOW : MUTED);
        drawRight(context, "Found " + scanner.candidatesFound(), x + w - 122, TOP + 34, GREEN);
    }

    private void renderChips(DrawContext context) {
        int x = mainX();
        int y = TOP + 64;
        int w = mainW();
        int chipW = Math.max(88, (w - GAP * 4) / 5);
        chip(context, x, y, chipW, "Live BIN Deals", displayedFlips().size(), PURPLE);
        chip(context, x + (chipW + GAP), y, chipW, "High Profit", countHighProfit(), YELLOW);
        chip(context, x + (chipW + GAP) * 2, y, chipW, "High % Profit", countHighPercent(), GREEN);
        chip(context, x + (chipW + GAP) * 3, y, chipW, "New Listings", scanner.candidatesFound(), BLUE);
        chip(context, x + (chipW + GAP) * 4, y, chipW, "Filtered", scanner.candidatesFilteredOut(), 0xFF9B6CFF);
    }

    private void renderMain(DrawContext context) {
        if ("About".equals(activeTab)) {
            renderInfo(context, "Informational only. No auto-buy, no slot clicking, no confirmations.");
            return;
        }
        if ("Recent Sales".equals(activeTab) || "Profit Tracker".equals(activeTab) || "Craft Flips".equals(activeTab)) {
            renderInfo(context, activeTab + " will unlock after stored sales history and NBT decoding.");
            return;
        }

        renderTable(context);
        renderProfitPanel(context);
    }

    private void renderTable(DrawContext context) {
        int x = tableX();
        int y = tableY();
        int right = tableRight();
        int bottom = tableBottom();
        panel(context, x, y, right - x, bottom - y);
        drawHeaders(context, x, y, right);

        List<FlipCandidate> flips = displayedFlips();
        if (flips.isEmpty()) {
            drawText(context, scanner.isScanning() ? "Scanning live auctions..." : "No deals match the active filters.", x + 14, y + 48, MUTED);
            drawText(context, "Try lowering filters, or wait for the next API cycle.", x + 14, y + 66, SOFT);
            return;
        }

        int rowY = y + 30;
        for (int i = 0; i < visibleRows() && scrollOffset + i < flips.size(); i++) {
            int index = scrollOffset + i;
            renderRow(context, flips.get(index), index, rowY);
            rowY += rowH();
        }

        context.fill(x, bottom, right, bottom + 28, PANEL_2);
        String text = (scrollOffset + 1) + "-" + Math.min(flips.size(), scrollOffset + visibleRows()) + " / " + flips.size();
        drawCentered(context, text + "  |  mouse wheel scroll  |  click a row", (x + right) / 2, bottom + 10, MUTED);
    }

    private void drawHeaders(DrawContext context, int x, int y, int right) {
        int itemX = x + 14;
        int binX = x + colBin();
        int valueX = x + colValue();
        int profitX = x + colProfit();
        int pctX = x + colPct();
        int confX = x + colConf();
        int volX = x + colVol();
        int ageX = right - 38;
        drawText(context, "Item", itemX, y + 11, TEXT);
        drawText(context, "BIN Price", binX, y + 11, TEXT);
        drawText(context, "Est. Value", valueX, y + 11, TEXT);
        drawText(context, "Profit", profitX, y + 11, TEXT);
        drawText(context, "Profit %", pctX, y + 11, TEXT);
        drawText(context, "Conf", confX, y + 11, TEXT);
        drawText(context, "Vol", volX, y + 11, TEXT);
        drawText(context, "Age", ageX, y + 11, TEXT);
    }

    private void renderRow(DrawContext context, FlipCandidate flip, int index, int y) {
        int x = tableX();
        int right = tableRight();
        boolean selected = index == selectedIndex;
        context.fill(x + 1, y, right - 1, y + rowH() - 1, selected ? 0xFF211431 : (index % 2 == 0 ? 0xAA11151D : 0x7711151D));
        context.fill(x + 12, y + 7, x + 38, y + 33, itemColor(flip));
        context.fill(x + 14, y + 9, x + 36, y + 31, 0x66101018);
        drawCentered(context, itemGlyph(flip), x + 25, y + 16, TEXT);

        int itemW = Math.max(100, colBin() - 54);
        drawText(context, trim(flip.auction().itemName(), itemW), x + 46, y + 7, selected ? PURPLE : TEXT);
        drawText(context, subLabel(flip), x + 46, y + 22, subColor(flip));
        drawText(context, compactCoins(flip.auction().binPrice()), x + colBin(), y + 15, YELLOW);
        drawText(context, compactCoins(flip.estimatedMarketValue()), x + colValue(), y + 15, suspicious(flip) ? YELLOW : TEXT);
        drawText(context, "+" + compactCoins(flip.profitAfterTax()), x + colProfit(), y + 15, suspicious(flip) ? YELLOW : GREEN);
        drawText(context, percentText(flip.profitPercent()) + "%", x + colPct(), y + 15, suspicious(flip) ? YELLOW : GREEN);
        drawText(context, flip.confidencePercent() + "%", x + colConf(), y + 15, confidenceColor(flip.confidencePercent()));
        drawText(context, String.format(Locale.US, "%.0f/day", flip.volumePerDay()), x + colVol(), y + 15, MUTED);
        drawRight(context, flip.ageMinutes() + "m", right - 12, y + 15, flip.ageMinutes() > configManager.get().maxAuctionAgeMinutes ? YELLOW : MUTED);
        context.fill(x + 1, y + rowH() - 1, right - 1, y + rowH(), LINE);
    }

    private void renderRightPanel(DrawContext context) {
        if (rightW() <= 0) {
            return;
        }
        int x = rightX();
        int w = rightW();
        int y = TOP + 64;
        int bottom = height - 42;
        panel(context, x, y, w, bottom - y);
        List<FlipCandidate> flips = displayedFlips();
        if (flips.isEmpty()) {
            drawText(context, "Deal Details", x + 14, y + 16, PURPLE);
            drawText(context, "Select a deal to inspect.", x + 14, y + 36, MUTED);
            return;
        }

        FlipCandidate flip = flips.get(Math.min(selectedIndex, flips.size() - 1));
        context.fill(x + 14, y + 14, x + 50, y + 50, itemColor(flip));
        drawCentered(context, itemGlyph(flip), x + 32, y + 27, TEXT);
        drawText(context, trim(flip.auction().itemName(), w - 78), x + 60, y + 16, PURPLE);
        drawText(context, subLabel(flip), x + 60, y + 33, subColor(flip));

        int line = y + 72;
        panel(context, x + 10, line, w - 20, 126);
        drawText(context, "Deal Summary", x + 20, line + 12, PURPLE);
        int metricY = line + 34;
        metric(context, x + 20, x + w - 20, metricY, "BIN Price", coinText(flip.auction().binPrice()), YELLOW);
        metricY += 17;
        metric(context, x + 20, x + w - 20, metricY, "Est. Market Value", coinText(flip.estimatedMarketValue()), suspicious(flip) ? YELLOW : TEXT);
        metricY += 17;
        metric(context, x + 20, x + w - 20, metricY, "Auction Tax", "-" + coinText(flip.estimatedAuctionTax()), RED);
        metricY += 23;
        metric(context, x + 20, x + w - 20, metricY, "Profit After Tax", "+" + coinText(flip.profitAfterTax()), suspicious(flip) ? YELLOW : GREEN);
        metricY += 17;
        metric(context, x + 20, x + w - 20, metricY, "Profit %", percentText(flip.profitPercent()) + "%", suspicious(flip) ? YELLOW : GREEN);
        metricY += 17;
        metric(context, x + 20, x + w - 20, metricY, "Confidence", flip.confidencePercent() + "%", confidenceColor(flip.confidencePercent()));

        line += 136;
        panel(context, x + 10, line, w - 20, 88);
        drawText(context, "Pricing Notes", x + 20, line + 12, PURPLE);
        drawText(context, trim("Lowest comparable BIN cluster; high outliers ignored.", w - 40), x + 20, line + 32, MUTED);
        drawText(context, trim("Volume " + String.format(Locale.US, "%.0f/day", flip.volumePerDay()) + " | Age " + flip.ageMinutes() + "m", w - 40), x + 20, line + 48, MUTED);
        if (suspicious(flip)) {
            drawText(context, trim("Warning: inspect price manually.", w - 40), x + 20, line + 64, YELLOW);
        } else {
            drawText(context, "Manual purchase required.", x + 20, line + 64, YELLOW);
        }
    }

    private void renderProfitPanel(DrawContext context) {
        int y = tableBottom() + 34;
        if (y + 58 > height - 38) {
            return;
        }
        int x = tableX();
        int right = tableRight();
        panel(context, x, y, right - x, 54);
        drawText(context, "Scan Summary", x + 12, y + 10, PURPLE);
        drawText(context, "Potential profit listed: " + compactCoins(totalProfit()), x + 12, y + 29, GREEN);
        drawText(context, "Deals found: " + displayedFlips().size(), x + 190, y + 29, MUTED);
        drawText(context, "Filtered out: " + scanner.candidatesFilteredOut(), x + 310, y + 29, MUTED);
    }

    private void renderBottomBar(DrawContext context) {
        int y = height - 34;
        context.fill(6, y, width - 6, height - 6, 0xEE0D111A);
        drawText(context, "Manual-only mode", 18, y + 10, YELLOW);
        drawText(context, "No auto-buy, no slot clicking, no confirmations", 122, y + 10, MUTED);
        drawRight(context, "Next API refresh: " + Math.max(0, configManager.get().refreshIntervalSeconds) + "s", width - 18, y + 10, MUTED);
    }

    private void renderInfo(DrawContext context, String message) {
        panel(context, mainX(), TOP + 64, mainW(), height - 112);
        drawText(context, activeTab, mainX() + 16, TOP + 82, PURPLE);
        drawText(context, message, mainX() + 16, TOP + 102, TEXT);
    }

    private void chip(DrawContext context, int x, int y, int w, String label, int count, int color) {
        context.fill(x, y, x + w, y + 28, PANEL_2);
        context.fill(x, y + 27, x + w, y + 28, 0x552B3143);
        drawText(context, label, x + 8, y + 10, color);
        drawRight(context, String.valueOf(count), x + w - 8, y + 10, TEXT);
    }

    private void metric(DrawContext context, int left, int right, int y, String label, String value, int valueColor) {
        drawText(context, label, left, y, MUTED);
        drawRight(context, value, right, y, valueColor);
    }

    private void panel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, PANEL);
        context.fill(x, y, x + w, y + 1, LINE);
        context.fill(x, y + h - 1, x + w, y + h, 0xAA0A0C12);
        context.fill(x, y, x + 1, y + h, LINE);
        context.fill(x + w - 1, y, x + w, y + h, 0xAA0A0C12);
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

    private int mainX() {
        return SIDEBAR + GAP;
    }

    private int mainW() {
        return width - SIDEBAR - rightW() - GAP * 3;
    }

    private int rightW() {
        return width >= 900 ? Math.min(280, Math.max(230, width / 4)) : 0;
    }

    private int rightX() {
        return width - rightW() - GAP;
    }

    private int tableX() {
        return mainX();
    }

    private int tableY() {
        return TOP + 100;
    }

    private int tableRight() {
        return mainX() + mainW();
    }

    private int tableBottom() {
        int preferred = height - 122;
        return Math.max(tableY() + 100, preferred);
    }

    private int rowH() {
        return 42;
    }

    private int visibleRows() {
        return Math.max(1, (tableBottom() - (tableY() + 30)) / rowH());
    }

    private int colBin() {
        return Math.max(190, mainW() - 360);
    }

    private int colValue() {
        return colBin() + 82;
    }

    private int colProfit() {
        return colValue() + 90;
    }

    private int colPct() {
        return colProfit() + 78;
    }

    private int colConf() {
        return colPct() + 58;
    }

    private int colVol() {
        return colConf() + 48;
    }

    private int countHighProfit() {
        return (int) displayedFlips().stream().filter(flip -> flip.profitAfterTax() >= configManager.get().minimumProfitCoins * 3L).count();
    }

    private int countHighPercent() {
        return (int) displayedFlips().stream().filter(flip -> flip.profitPercent() >= configManager.get().minimumProfitPercent * 2.0D).count();
    }

    private long totalProfit() {
        return displayedFlips().stream().mapToLong(FlipCandidate::profitAfterTax).sum();
    }

    private String lastScanText() {
        if (scanner.lastScan().equals(java.time.Instant.EPOCH)) {
            return "never";
        }
        return DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(scanner.lastScan());
    }

    private String navIcon(String item) {
        return switch (item) {
            case "Flip Radar" -> "*";
            case "Watchlist" -> "+";
            case "Craft Flips" -> "^";
            case "Pet Flips" -> "#";
            case "Recent Sales" -> "/";
            case "Profit Tracker" -> "$";
            case "Settings" -> "@";
            default -> "i";
        };
    }

    private String subLabel(FlipCandidate flip) {
        String label = flip.auction().tier();
        if (flip.auction().signature().contains("recombobulated")) {
            label += " | Recombobulated";
        }
        return label;
    }

    private int subColor(FlipCandidate flip) {
        return switch (flip.auction().tier()) {
            case "LEGENDARY" -> YELLOW;
            case "MYTHIC", "DIVINE" -> PURPLE;
            case "EPIC" -> 0xFFFF55FF;
            case "RARE" -> BLUE;
            default -> MUTED;
        };
    }

    private int itemColor(FlipCandidate flip) {
        return switch (flip.auction().tier()) {
            case "LEGENDARY" -> 0x663B2B05;
            case "MYTHIC", "DIVINE" -> 0x66301852;
            case "EPIC" -> 0x66381838;
            case "RARE" -> 0x66203252;
            default -> 0x66222A38;
        };
    }

    private String itemGlyph(FlipCandidate flip) {
        String name = flip.auction().itemName().toLowerCase(Locale.ROOT);
        if (name.contains("pet") || name.startsWith("[lvl")) {
            return "P";
        }
        if (name.contains("boots") || name.contains("leggings") || name.contains("chestplate") || name.contains("helmet")) {
            return "A";
        }
        if (name.contains("sword") || name.contains("blade") || name.contains("staff") || name.contains("bow")) {
            return "W";
        }
        return "$";
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

    private void drawRight(DrawContext context, String value, int rightX, int y, int color) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        context.drawText(minecraft.textRenderer, Text.literal(value), rightX - minecraft.textRenderer.getWidth(value), y, color, false);
    }

    private void drawCentered(DrawContext context, String value, int centerX, int y, int color) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        context.drawText(minecraft.textRenderer, Text.literal(value), centerX - minecraft.textRenderer.getWidth(value) / 2, y, color, false);
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
