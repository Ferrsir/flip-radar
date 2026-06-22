package com.flipradar.auction;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemSignatureBuilder {
    private static final Pattern PET_LEVEL = Pattern.compile("\\[(?:lvl|level)\\s*(\\d{1,3})]", Pattern.CASE_INSENSITIVE);

    public String build(String itemName, String lore, String tier, String itemBytes) {
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
        return normalizedName + "|" + tier.toLowerCase(Locale.ROOT) + petLevelBucket + "|stars:" + stars + modifiers;
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

    private String normalize(String value) {
        String plain = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("§.", "")
                .toLowerCase(Locale.ROOT);
        return plain.replaceAll("[^a-z0-9 +_-]", " ").replaceAll("\\s+", " ").trim();
    }
}
