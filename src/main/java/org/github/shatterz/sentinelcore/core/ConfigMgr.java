package org.github.shatterz.sentinelcore.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.github.shatterz.sentinelcore.SentinelCore;

/** Tiny JSON config stub so /sccore reload has an effect. */
public final class ConfigMgr {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String FILE_NAME = "sentinelcore/config.json";
  private static Path path;
  private static volatile RootConfig CONFIG = new RootConfig();

  public static class RootConfig {
    public boolean debug = false;
    public Features features = new Features();

    public static class Features {
      public boolean spawnelytra = true;
      public boolean spawnprotection = true;
      public boolean prefixes = true;
      public boolean permissions = true;
    }
  }

  private ConfigMgr() {}

  public static void init() {
    path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    load();
  }

  public static synchronized void load() {
    try {
      if (!Files.exists(path)) {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(new RootConfig()));
      }
      String json = Files.readString(path);
      CONFIG = GSON.fromJson(json, RootConfig.class);
      SentinelCore.LOGGER.info("[SentinelCore/Config] Loaded {}", path.toAbsolutePath());
    } catch (IOException e) {
      SentinelCore.LOGGER.error("[SentinelCore/Config] Failed to load config", e);
    }
  }

  public static RootConfig get() {
    return CONFIG;
  }
}
