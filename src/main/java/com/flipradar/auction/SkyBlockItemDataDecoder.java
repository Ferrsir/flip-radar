package com.flipradar.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public final class SkyBlockItemDataDecoder {
    public SkyBlockItemData decode(String itemBytes) {
        if (itemBytes == null || itemBytes.isBlank()) {
            return SkyBlockItemData.unknown();
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(itemBytes);
            NbtCompound root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtSizeTracker.of(2_000_000L));
            NbtCompound item = firstItem(root);
            NbtCompound tag = item.getCompoundOrEmpty("tag");
            NbtCompound extra = tag.getCompoundOrEmpty("ExtraAttributes");
            if (extra.isEmpty()) {
                extra = item.getCompoundOrEmpty("ExtraAttributes");
            }
            return decodeExtraAttributes(extra);
        } catch (IllegalArgumentException | IOException ignored) {
            return SkyBlockItemData.unknown();
        }
    }

    private NbtCompound firstItem(NbtCompound root) {
        NbtList items = root.getListOrEmpty("i");
        if (!items.isEmpty()) {
            return items.getCompoundOrEmpty(0);
        }
        return root;
    }

    private SkyBlockItemData decodeExtraAttributes(NbtCompound extra) {
        if (extra.isEmpty()) {
            return SkyBlockItemData.unknown();
        }

        String skyBlockId = extra.getString("id", "");
        Map<String, Integer> enchants = intMap(extra.getCompoundOrEmpty("enchantments"));
        Map<String, String> gemstones = stringMap(extra.getCompoundOrEmpty("gems"));
        Map<String, Integer> attributes = intMap(extra.getCompoundOrEmpty("attributes"));

        int petLevel = 0;
        String petType = "";
        String petTier = "";
        String petInfo = extra.getString("petInfo", "");
        if (!petInfo.isBlank()) {
            try {
                JsonObject pet = JsonParser.parseString(petInfo).getAsJsonObject();
                petType = pet.has("type") ? pet.get("type").getAsString() : "";
                petTier = pet.has("tier") ? pet.get("tier").getAsString() : "";
                petLevel = estimatePetLevel(pet.has("exp") ? pet.get("exp").getAsDouble() : 0.0D);
            } catch (Exception ignored) {
                petType = "";
                petTier = "";
            }
        }

        return new SkyBlockItemData(
                skyBlockId,
                extra.getInt("rarity_upgrades", 0) > 0,
                extra.getInt("upgrade_level", 0),
                extra.getInt("hot_potato_count", 0),
                extra.getInt("rarity_upgrades", 0),
                petLevel,
                petType,
                petTier,
                enchants,
                gemstones,
                attributes
        );
    }

    private Map<String, Integer> intMap(NbtCompound compound) {
        Map<String, Integer> values = new TreeMap<>();
        for (String key : compound.getKeys()) {
            values.put(key, compound.getInt(key, 0));
        }
        return values;
    }

    private Map<String, String> stringMap(NbtCompound compound) {
        Map<String, String> values = new TreeMap<>();
        for (Map.Entry<String, NbtElement> entry : compound.entrySet()) {
            values.put(entry.getKey(), entry.getValue().asString().orElse(entry.getValue().toString()));
        }
        return values;
    }

    private int estimatePetLevel(double exp) {
        if (exp <= 0.0D) {
            return 1;
        }
        if (exp >= 25_353_230.0D) {
            return 100;
        }
        return Math.max(1, Math.min(99, (int) Math.floor(Math.sqrt(exp / 2535.323D))));
    }
}
