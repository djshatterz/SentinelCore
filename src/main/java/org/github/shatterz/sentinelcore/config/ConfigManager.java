package org.github.shatterz.sentinelcore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigManager {
  private static volatile Thread WATCHER_THREAD;
  private static volatile java.util.function.Consumer<CoreConfig> ON_RELOAD = null;
  private static final java.util.List<java.util.function.Consumer<CoreConfig>> LISTENERS =
      new java.util.concurrent.CopyOnWriteArrayList<>();
  private static final Logger LOG = LoggerFactory.getLogger("SentinelCore/Config");
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
  private static final ObjectMapper JSON = new ObjectMapper();

  private static final String DIR_NAME = "sentinelcore";
  private static final String YAML_NAME = "config.yaml";
  private static final String JSON_NAME = "config.json";

  private static Path configDir() {
    return FabricLoader.getInstance().getConfigDir().resolve(DIR_NAME);
  }

  private static Path yamlPath() {
    return configDir().resolve(YAML_NAME);
  }

  private static Path jsonPath() {
    return configDir().resolve(JSON_NAME);
  }

  private static volatile CoreConfig CURRENT = CoreConfig.defaults();

  public static CoreConfig get() {
    return CURRENT;
  }

  public static void init(Consumer<CoreConfig> onReload) {
    try {
      Files.createDirectories(configDir());
      Path yml = yamlPath();
      Path json = jsonPath();
      if (!Files.exists(yml) && !Files.exists(json)) {
        saveYAML(CoreConfig.defaults());
        LOG.info("Created default config at {}", yml.toAbsolutePath());
      }
      CURRENT = load();
      LOG.info("Loaded config: {} featureFlags={}", fileInUse(), CURRENT.featureFlags.keySet());
      // Store callback and immediately notify listeners for initial load so dependent systems
      // can apply configuration before first use (e.g., permissions backend, name formatting)
      ON_RELOAD = onReload; // <-- store legacy single callback
      notifyListeners(CURRENT);
      startWatcher(onReload);
    } catch (IOException e) {
      LOG.error("Failed to init config, using defaults", e);
      CURRENT = CoreConfig.defaults();
    }
  }

  public static synchronized boolean reloadNow() {
    try {
      CoreConfig newCfg = load();
      CURRENT = newCfg;
      LOG.info("Config reloaded from {}", fileInUse().toAbsolutePath());
      notifyListeners(newCfg);
      return true;
    } catch (Exception ex) {
      LOG.error("Manual reload failed", ex);
      return false;
    }
  }

  public static Path fileInUse() {
    return Files.exists(yamlPath()) ? yamlPath() : jsonPath();
  }

  public static CoreConfig load() throws IOException {
    Path file = fileInUse();
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);

    try (BufferedReader r = Files.newBufferedReader(file)) {
      if (name.endsWith(".json")) {
        return JSON.readValue(r, CoreConfig.class);
      } else {
        return YAML.readValue(r, CoreConfig.class);
      }
    }
  }

  /** Add an additional reload listener without replacing the main callback. */
  public static void addReloadListener(Consumer<CoreConfig> listener) {
    if (listener != null) LISTENERS.add(listener);
  }

  private static void notifyListeners(CoreConfig cfg) {
    if (ON_RELOAD != null) {
      try {
        ON_RELOAD.accept(cfg);
      } catch (Exception cb) {
        LOG.error("Reload callback failed", cb);
      }
    }
    for (Consumer<CoreConfig> l : LISTENERS) {
      try {
        l.accept(cfg);
      } catch (Exception ex) {
        LOG.error("Reload listener failed", ex);
      }
    }
  }

  public static void saveYAML(CoreConfig cfg) throws IOException {
    try (OutputStream out = Files.newOutputStream(yamlPath())) {
      YAML.writerWithDefaultPrettyPrinter().writeValue(out, cfg);
    }
  }

  // naive debounced watcher (single-threaded)
  private static void startWatcher(Consumer<CoreConfig> onReload) {
    // If already running, don't start twice
    if (WATCHER_THREAD != null && WATCHER_THREAD.isAlive()) return;

    WATCHER_THREAD =
        new Thread(
            () -> {
              try (WatchService ws = FileSystems.getDefault().newWatchService()) {
                configDir()
                    .register(
                        ws,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);
                LOG.info("Watching {} for changes", configDir().toAbsolutePath());

                while (!Thread.currentThread().isInterrupted()) {
                  WatchKey key = ws.take();
                  boolean relevant = false;
                  for (WatchEvent<?> ev : key.pollEvents()) {
                    Path p = (Path) ev.context();
                    String n = p.getFileName().toString();
                    if (YAML_NAME.equals(n) || JSON_NAME.equals(n)) relevant = true;
                  }
                  key.reset();

                  if (relevant) {
                    // debounce a bit
                    TimeUnit.MILLISECONDS.sleep(250);
                    try {
                      CoreConfig newCfg = load();
                      CURRENT = newCfg;
                      LOG.info("Config reloaded from {}", fileInUse().toAbsolutePath());
                      notifyListeners(newCfg);
                    } catch (Exception ex) {
                      LOG.error("Failed to reload config", ex);
                    }
                  }
                }
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // graceful exit
              } catch (Exception e) {
                LOG.error("Config watcher stopped", e);
              }
            },
            "SentinelCore-ConfigWatcher");

    WATCHER_THREAD.setDaemon(true);
    WATCHER_THREAD.start();

    // Ensure we stop on JVM shutdown (no lingering threads)
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (WATCHER_THREAD != null) {
                    WATCHER_THREAD.interrupt();
                    try {
                      WATCHER_THREAD.join(1000);
                    } catch (InterruptedException ignored) {
                    }
                  }
                },
                "SentinelCore-ConfigWatcher-Shutdown"));
  }

  private static void saveCurrent() throws IOException {
    Path file = fileInUse();
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".json")) {
      try (OutputStream out = Files.newOutputStream(file)) {
        JSON.writerWithDefaultPrettyPrinter().writeValue(out, CURRENT);
      }
    } else {
      try (OutputStream out = Files.newOutputStream(file)) {
        YAML.writerWithDefaultPrettyPrinter().writeValue(out, CURRENT);
      }
    }
  }

  public static synchronized boolean setUserRole(UUID uuid, String role) {
    try {
      if (CURRENT.permissions == null) {
        CURRENT.permissions = new CoreConfig.Permissions();
      }
      if (CURRENT.permissions.userRoles == null) {
        CURRENT.permissions.userRoles = new java.util.HashMap<>();
      }
      CURRENT.permissions.userRoles.put(uuid.toString(), role);
      saveCurrent(); // persist to file
      LOG.info("Set role {} for {}", role, uuid);
      if (ON_RELOAD != null) {
        try {
          ON_RELOAD.accept(CURRENT);
        } catch (Exception cb) {
          LOG.error("Reload callback failed after role change", cb);
        }
      }
      return true;
    } catch (Exception e) {
      LOG.error("Failed to set role for {} to {}", uuid, role, e);
      return false;
    }
  }
}
