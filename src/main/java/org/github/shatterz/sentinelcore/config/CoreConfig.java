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

    // default role exists but grants nothing
    Permissions p = c.permissions;
    Role def = new Role();
    p.roles.put("default", def);
    p.defaultRole = "default";

    return c;
  }
}
