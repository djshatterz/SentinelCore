package org.github.shatterz.sentinelcore.flags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.github.shatterz.sentinelcore.config.ConfigManager;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FeatureFlagRegistry {
  private static final Logger LOG = LoggerFactory.getLogger("SentinelCore/Flags");
  private static final Map<String, Boolean> FLAGS = new ConcurrentHashMap<>();

  private FeatureFlagRegistry() {}

  public static void initFrom(CoreConfig cfg) {
    FLAGS.clear();
    FLAGS.putAll(cfg.featureFlags);
    LOG.info("Feature flags loaded: {}", FLAGS);
  }

  public static boolean isEnabled(String flag) {
    return FLAGS.getOrDefault(flag, false);
  }

  // wire into config hot-reload
  public static void wireReload() {
    ConfigManager.init(FeatureFlagRegistry::initFrom);
    initFrom(ConfigManager.get());
  }
}
