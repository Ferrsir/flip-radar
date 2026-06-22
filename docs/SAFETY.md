# Flip Radar Safety Model

Flip Radar is designed as an informational scanner only.

Allowed behavior:

- Fetch official Hypixel Public API auction data.
- Cache data locally to avoid unnecessary requests.
- Score possible manual BIN flips.
- Show auction information in a GUI.
- Send a single normal `/viewauction <auctionUuid>` command after the player clicks `Open Auction`.

Forbidden behavior:

- Auto-buying.
- Auto-confirming.
- Clicking inventory slots.
- Repeated command loops.
- Macro behavior.
- Claiming, listing, relisting, or purchasing items.
- Bypassing normal player action.

All purchase decisions and all Auction House clicks remain the player's manual action.
