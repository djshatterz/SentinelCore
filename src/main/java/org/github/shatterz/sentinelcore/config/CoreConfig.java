package org.github.shatterz.sentinelcore.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CoreConfig {
  /** Feature flags toggled at runtime (hot-reload via ConfigManager). */
  public Map<String, Boolean> featureFlags = new HashMap<>();

  /** Logging category switches (can be extended to levels later). */
  public Logging logging = new Logging();

  /** Permission configuration (backend + roles). */
  public Permissions permissions = new Permissions();

  /** Default constructor populates nothing; use defaults() to create a prefilled config. */
  public CoreConfig() {}

  public static class Logging {
    public boolean audit = true;
    public boolean perm = true;
    public boolean move = true;
    public boolean modmode = true;
    public boolean spawn = true;
  }

  /** Permissions schema stored in YAML/JSON config. */
  public static class Permissions {
    public String backend = "memory"; // future: "luckperms"
    public String defaultRole = "default";
    public Map<String, Role> roles = new HashMap<>(); // role name -> role definition
    public Map<String, String> userRoles = new HashMap<>(); // uuid -> role name
  }

  /** A role with simple allow/deny sets and inheritance. */
  public static class Role {
    public Set<String> allow = new HashSet<>();
    public Set<String> deny = new HashSet<>();
    public Set<String> inherits = new HashSet<>();
  }

  /** Create a config with sensible defaults. */
  public static CoreConfig defaults() {
    CoreConfig c = new CoreConfig();
    c.featureFlags.put("exampleFlag", false);

    // Setup group hierarchy: default → moderator → admin → developer → op
    Permissions p = c.permissions;

    // Default role - base permissions for all players
    Role defaultRole = new Role();
    defaultRole.allow.add("sentinelcore.info");
    p.roles.put("default", defaultRole);

    // Moderator - inherits from default
    Role moderator = new Role();
    moderator.allow.add("sentinelcore.mod.*");
    moderator.allow.add("sentinelcore.modmode.use");
    moderator.inherits.add("default");
    p.roles.put("moderator", moderator);

    // Admin - inherits from moderator
    Role admin = new Role();
    admin.allow.add("sentinelcore.admin.*");
    admin.allow.add("sentinelcore.warps.*");
    admin.deny.add("sentinelcore.admin.dangerous");
    admin.inherits.add("moderator");
    p.roles.put("admin", admin);

    // Developer - inherits from admin
    Role developer = new Role();
    developer.allow.add("sentinelcore.admin.dangerous");
    developer.allow.add("sentinelcore.dev.*");
    developer.inherits.add("admin");
    p.roles.put("developer", developer);

    // Op - top-level, inherits from developer
    Role op = new Role();
    op.allow.add("sentinelcore.*");
    op.inherits.add("developer");
    p.roles.put("op", op);

    p.defaultRole = "default";

    return c;
  }
}
