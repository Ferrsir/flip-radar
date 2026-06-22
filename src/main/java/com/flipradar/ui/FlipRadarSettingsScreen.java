package com.flipradar.ui;

import com.flipradar.config.ConfigManager;
import com.flipradar.config.FlipRadarConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class FlipRadarSettingsScreen extends Screen {
    private final ConfigManager configManager;
    private final Screen parent;
    private TextFieldWidget availableCoins;
    private TextFieldWidget maxPurchasePrice;
    private TextFieldWidget minProfit;

    public FlipRadarSettingsScreen(ConfigManager configManager, Screen parent) {
        super(Text.literal("Flip Radar Settings"));
        this.configManager = configManager;
        this.parent = parent;
    }

    @Override
    protected void init() {
        FlipRadarConfig config = configManager.get();
        availableCoins = field(190, 72, String.valueOf(config.availableCoins));
        maxPurchasePrice = field(190, 106, String.valueOf(config.maxPurchasePriceCoins));
        minProfit = field(190, 140, String.valueOf(config.minimumProfitCoins));

        addDrawableChild(availableCoins);
        addDrawableChild(maxPurchasePrice);
        addDrawableChild(minProfit);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            config.availableCoins = parseLong(availableCoins.getText(), config.availableCoins);
            config.maxPurchasePriceCoins = parseLong(maxPurchasePrice.getText(), config.maxPurchasePriceCoins);
            config.minimumProfitCoins = parseLong(minProfit.getText(), config.minimumProfitCoins);
            configManager.save();
            client.setScreen(parent);
        }).dimensions(width / 2 - 100, height - 38, 96, 22).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> client.setScreen(parent))
                .dimensions(width / 2 + 4, height - 38, 96, 22)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xEE090B12);
        context.drawText(textRenderer, Text.literal("Flip Radar Settings"), 32, 30, 0xFFB86CFF, false);
        context.drawText(textRenderer, Text.literal("Available coins"), 32, 78, 0xFFE8EAF6, false);
        context.drawText(textRenderer, Text.literal("Max purchase price"), 32, 112, 0xFFE8EAF6, false);
        context.drawText(textRenderer, Text.literal("Minimum profit"), 32, 146, 0xFFE8EAF6, false);
        super.render(context, mouseX, mouseY, delta);
    }

    private TextFieldWidget field(int x, int y, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 160, 20, Text.empty());
        field.setText(value);
        field.setMaxLength(18);
        return field;
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
