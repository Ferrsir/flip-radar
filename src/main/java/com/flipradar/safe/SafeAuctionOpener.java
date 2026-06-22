package com.flipradar.safe;

import com.flipradar.auction.AuctionItem;
import net.minecraft.client.MinecraftClient;

public final class SafeAuctionOpener {
    public void openAuctionManually(AuctionItem auction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        // Safety rule: this is the only action path. It opens a normal Auction House view.
        // It never clicks slots, never confirms purchases, and never buys anything.
        client.player.networkHandler.sendChatCommand("viewauction " + auction.uuid());
    }
}
