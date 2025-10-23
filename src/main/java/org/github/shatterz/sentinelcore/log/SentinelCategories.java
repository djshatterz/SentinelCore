package org.github.shatterz.sentinelcore.log;

public enum SentinelCategories {
  AUDIT("audit"),
  PERM("perm"),
  MOVE("move"),
  MODMODE("modmode"),
  SPAWN("spawn");

  private final String id;

  SentinelCategories(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
