# Flip Radar

Flip Radar is a Fabric 1.21.11 client-side Minecraft mod for safe, manual Hypixel SkyBlock BIN flip scanning.

It is informational only. It fetches public Hypixel auction data, estimates potential flips, and shows them in an in-game GUI. The mod never auto-buys, auto-clicks, auto-confirms, clicks inventory slots, relists, claims, or performs purchases for the player.

## MVP Features

- Fetches active SkyBlock auction pages from the official Hypixel Public API.
- Filters to Buy It Now auctions.
- Caches auction data locally to reduce API pressure.
- Builds item signatures for comparable pricing.
- Estimates market value from locally observed lowest BIN samples.
- Calculates estimated auction tax, profit, profit percent, and confidence.
- Filters flips by budget and user settings.
- Shows results in a dark neon-purple in-game screen.
- Provides a manual `Open Auction` shortcut that sends `/ah sellerName` only after a direct user click.

## Controls

- Default keybind: `R`
- Category: `Flip Radar`

## Build

Use Java 21.

```powershell
gradle build
```

If you want the Gradle wrapper in this repo:

```powershell
gradle wrapper
```

## Safety

Hypixel rules apply. Flip Radar intentionally avoids automation:

- no auto-buy
- no auto-confirm
- no inventory slot clicking
- no repeated command spam
- no macro behavior
- no bypasses

Every purchase decision and click remains manual.
