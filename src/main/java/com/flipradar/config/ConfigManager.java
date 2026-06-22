package com.flipradar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private FlipRadarConfig config;

    public ConfigManager(Path configPath) {
        this.configPath = configPath;
        this.config = load();
    }

    public synchronized FlipRadarConfig get() {
        return config;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private FlipRadarConfig load() {
        try {
            if (Files.notExists(configPath)) {
                FlipRadarConfig defaults = new FlipRadarConfig();
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, GSON.toJson(defaults), StandardCharsets.UTF_8);
                return defaults;
            }
            FlipRadarConfig loaded = GSON.fromJson(Files.readString(configPath, StandardCharsets.UTF_8), FlipRadarConfig.class);
            return loaded == null ? new FlipRadarConfig() : loaded;
        } catch (Exception ignored) {
            return new FlipRadarConfig();
        }
    }
}
