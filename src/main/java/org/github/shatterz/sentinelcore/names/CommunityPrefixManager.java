package org.github.shatterz.sentinelcore.names;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

/**
 * Manages player community prefix selections. Stores per-UUID selections in a YAML file.
 *
 * <p>Storage: config/sentinelcore/community-selections.yaml
 */
public final class CommunityPrefixManager {
  private static final Logger LOG = SentinelLogger.root();
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

  private static final Map<UUID, String> SELECTIONS = new ConcurrentHashMap<>();
  private static Path storageFile;

  private CommunityPrefixManager() {}

  /** Initialize the manager and load selections from disk. */
  public static void init() {
    Path configDir =
        FabricLoader.getInstance()
            .getConfigDir()
            .resolve("sentinelcore")
            .resolve("community-selections.yaml");
    storageFile = configDir;

    load();
    LOG.info("Community prefix manager initialized. Storage: {}", storageFile);
  }

  /** Get the selected prefix ID for a player, or null if none selected. */
  public static String getSelectedPrefix(UUID uuid) {
    return SELECTIONS.get(uuid);
  }

  /** Set the selected prefix ID for a player. Pass null to clear. */
  public static void setSelectedPrefix(UUID uuid, String prefixId) {
    if (prefixId == null) {
      SELECTIONS.remove(uuid);
    } else {
      SELECTIONS.put(uuid, prefixId);
    }
    save();
  }

  /** Clear the selected prefix for a player. */
  public static void clearPrefix(UUID uuid) {
    setSelectedPrefix(uuid, null);
  }

  /** Get all selections (for admin inspection). */
  public static Map<UUID, String> getAllSelections() {
    return new HashMap<>(SELECTIONS);
  }

  /** Load selections from disk. */
  private static void load() {
    if (!Files.exists(storageFile)) {
      LOG.info("No community selections file found. Starting fresh.");
      return;
    }

    try {
      @SuppressWarnings("unchecked")
      Map<String, String> loaded =
          YAML.readValue(storageFile.toFile(), Map.class); // UUID strings -> prefix IDs
      SELECTIONS.clear();
      for (Map.Entry<String, String> entry : loaded.entrySet()) {
        try {
          UUID uuid = UUID.fromString(entry.getKey());
          SELECTIONS.put(uuid, entry.getValue());
        } catch (IllegalArgumentException e) {
          LOG.warn("Skipping invalid UUID in community selections: {}", entry.getKey());
        }
      }
      LOG.info("Loaded {} community prefix selections.", SELECTIONS.size());
    } catch (IOException e) {
      LOG.error("Failed to load community selections from {}", storageFile, e);
    }
  }

  /** Save selections to disk. */
  private static void save() {
    try {
      // Ensure parent directory exists
      Files.createDirectories(storageFile.getParent());

      // Convert UUID -> String for YAML serialization
      Map<String, String> toSave = new HashMap<>();
      for (Map.Entry<UUID, String> entry : SELECTIONS.entrySet()) {
        toSave.put(entry.getKey().toString(), entry.getValue());
      }

      YAML.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), toSave);
      LOG.debug("Saved {} community prefix selections to {}", toSave.size(), storageFile);
    } catch (IOException e) {
      LOG.error("Failed to save community selections to {}", storageFile, e);
    }
  }

  /** Reload selections from disk (for config hot-reload). */
  public static void reload() {
    LOG.info("Reloading community prefix selections...");
    load();
  }
}
