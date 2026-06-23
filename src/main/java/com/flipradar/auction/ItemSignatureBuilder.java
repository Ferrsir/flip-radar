package com.flipradar.auction;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemSignatureBuilder {
    private static final Pattern PET_LEVEL = Pattern.compile("\\[(?:lvl|level)\\s*(\\d{1,3})]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCHANT_LINE = Pattern.compile("\\b([A-Z][A-Za-z' -]+)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X)\\b");

    public String build(String itemName, String lore, String tier, String itemBytes) {
        return build(itemName, lore, tier, itemBytes, SkyBlockItemData.unknown());
    }

    public String build(String itemName, String lore, String tier, String itemBytes, SkyBlockItemData itemData) {
        if (itemData.hasNbtIdentity()) {
            return buildFromNbt(itemName, tier, itemData);
        }

        String normalizedName = normalize(itemName);
        String petLevelBucket = petLevelBucket(normalizedName);
        normalizedName = normalizedName
                .replaceAll("\\[(?:lvl|level)\\s*\\d{1,3}]", "")
                .replaceAll("\\[[^]]+]", "")
                .trim();
        String modifiers = "";

        if (contains(lore, "Recombobulated")) {
            modifiers += "|recombobulated";
        }
        if (contains(lore, "Hot Potato Book")) {
            modifiers += "|hot-potato";
        }
        if (contains(lore, "Gemstone")) {
            modifiers += "|gemstones";
        }
        if (contains(lore, "Attribute")) {
            modifiers += "|attributes";
        }

        int stars = countStars(itemName + " " + lore);
        int enchantWeight = enchantWeight(lore);
        return normalizedName + "|" + tier.toLowerCase(Locale.ROOT) + petLevelBucket + "|stars:" + stars + "|enchants:" + enchantWeight + modifiers;
    }

    private String buildFromNbt(String itemName, String tier, SkyBlockItemData itemData) {
        StringBuilder signature = new StringBuilder();
        signature.append("id:").append(itemData.skyBlockId());
        signature.append("|tier:").append(tier.toLowerCase(Locale.ROOT));

        if (!itemData.petType().isBlank()) {
            signature.append("|pet:").append(itemData.petType().toLowerCase(Locale.ROOT));
            signature.append("|pet-tier:").append(itemData.petTier().toLowerCase(Locale.ROOT));
            signature.append(itemData.petLevel() >= 100 ? "|pet-level:100" : "|pet-level:1");
        }

        signature.append("|stars:").append(itemData.dungeonStars());
        signature.append("|recomb:").append(itemData.recombobulated());
        signature.append("|hpb:").append(Math.min(15, itemData.hotPotatoBooks()));

        if (!itemData.enchants().isEmpty()) {
            signature.append("|enchants:");
            itemData.enchants().forEach((key, value) -> signature.append(key).append('=').append(value).append(','));
        } else {
            signature.append("|enchants:0");
        }

        if (!itemData.gemstones().isEmpty()) {
            signature.append("|gems:");
            itemData.gemstones().forEach((key, value) -> signature.append(key).append('=').append(value).append(','));
        }

        if (!itemData.attributes().isEmpty()) {
            signature.append("|attributes:");
            itemData.attributes().forEach((key, value) -> signature.append(key).append('=').append(value).append(','));
        }

        return signature.toString();
    }

    private String petLevelBucket(String normalizedName) {
        Matcher matcher = PET_LEVEL.matcher(normalizedName);
        if (!matcher.find()) {
            return "";
        }

        int level = Integer.parseInt(matcher.group(1));
        return level >= 100 ? "|pet-level:100" : "|pet-level:1";
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private int countStars(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\u272a') {
                count++;
            }
        }
        return count;
    }

    private int enchantWeight(String lore) {
        Matcher matcher = ENCHANT_LINE.matcher(lore == null ? "" : lore.replaceAll("Â§.", ""));
        int weight = 0;
        while (matcher.find()) {
            weight += romanValue(matcher.group(2));
        }
        return Math.min(20, weight / 3);
    }

    private int romanValue(String roman) {
        return switch (roman) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            default -> 0;
        };
    }

    private String normalize(String value) {
        String plain = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("§.", "")
                .toLowerCase(Locale.ROOT);
        return plain.replaceAll("[^a-z0-9 +_-]", " ").replaceAll("\\s+", " ").trim();
    }
}
