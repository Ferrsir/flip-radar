package com.flipradar.auction;

import java.util.Map;
import java.util.TreeMap;

public record SkyBlockItemData(
        String skyBlockId,
        boolean recombobulated,
        int dungeonStars,
        int hotPotatoBooks,
        int rarityUpgrades,
        int petLevel,
        String petType,
        String petTier,
        Map<String, Integer> enchants,
        Map<String, String> gemstones,
        Map<String, Integer> attributes
) {
    public static SkyBlockItemData unknown() {
        return new SkyBlockItemData(
                "",
                false,
                0,
                0,
                0,
                0,
                "",
                "",
                new TreeMap<>(),
                new TreeMap<>(),
                new TreeMap<>()
        );
    }

    public boolean hasNbtIdentity() {
        return !skyBlockId.isBlank();
    }
}
