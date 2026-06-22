package com.flipradar.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class LocalCache {
    private final Path root;

    public LocalCache(Path root) {
        this.root = root;
    }

    public Optional<String> readFresh(String name, Duration maxAge) {
        try {
            Path path = root.resolve(name);
            if (Files.notExists(path)) {
                return Optional.empty();
            }
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            if (modified.plus(maxAge).isBefore(Instant.now())) {
                return Optional.empty();
            }
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    public Optional<String> readAny(String name) {
        try {
            Path path = root.resolve(name);
            if (Files.notExists(path)) {
                return Optional.empty();
            }
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    public void write(String name, String body, Instant timestamp) {
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve(name), body, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
