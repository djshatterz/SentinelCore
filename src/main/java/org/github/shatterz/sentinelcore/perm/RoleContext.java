package org.github.shatterz.sentinelcore.perm;

import java.util.*;

/**
 * Stores per-player state for permission evaluation. Tracks group, op status, mod-mode, vanish, and
 * context flags.
 */
public final class RoleContext {
  private final UUID uuid;
  private String group;
  private boolean isOp;
  private boolean modMode;
  private boolean vanished;
  private final Map<String, String> contextFlags;

  public RoleContext(UUID uuid) {
    this.uuid = uuid;
    this.group = "default";
    this.isOp = false;
    this.modMode = false;
    this.vanished = false;
    this.contextFlags = new HashMap<>();
  }

  public UUID getUuid() {
    return uuid;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group != null ? group : "default";
  }

  public boolean isOp() {
    return isOp;
  }

  public void setOp(boolean op) {
    this.isOp = op;
  }

  public boolean isModMode() {
    return modMode;
  }

  public void setModMode(boolean modMode) {
    this.modMode = modMode;
    updateContextFlag("modmode", String.valueOf(modMode));
  }

  public boolean isVanished() {
    return vanished;
  }

  public void setVanished(boolean vanished) {
    this.vanished = vanished;
    updateContextFlag("vanished", String.valueOf(vanished));
  }

  public Map<String, String> getContextFlags() {
    return Collections.unmodifiableMap(contextFlags);
  }

  public void updateContextFlag(String key, String value) {
    if (value == null) {
      contextFlags.remove(key);
    } else {
      contextFlags.put(key, value);
    }
  }

  public void setWorldContext(String worldId) {
    updateContextFlag("world", worldId);
  }

  @Override
  public String toString() {
    return "RoleContext{"
        + "uuid="
        + uuid
        + ", group='"
        + group
        + '\''
        + ", isOp="
        + isOp
        + ", modMode="
        + modMode
        + ", vanished="
        + vanished
        + ", contextFlags="
        + contextFlags
        + '}';
  }
}
