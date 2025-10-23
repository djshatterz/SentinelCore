package org.github.shatterz.sentinelcore.config;

import java.security.Permissions;
import java.util.HashMap;
import java.util.Map;

public class CoreConfig {
    public Map<String, Boolean> featureFlags = new HashMap<>();
    public Logging logging = new Logging();
    public Permissions permissions = new Permissions();

    public static class Permissions {
        public String backend = "memory"; // future: "luckperms"
        public String defaultRole = "default";
        public Map<String, String> roles = new java.util.HashMap<>();
        public Map<String, String> userRoles = new java.util.HashMap<>(); // UUID to role
    }

    public static class Role {
        public java.util.Set<String> allow = new java.util.HashSet<>();
        public java.util.Set<String> deny = new java.util.HashSet<>();
        public java.util.Set<String> inherit = new java.util.HashSet<>();
    }

    public static class Logging {
        // per-category on/off (you can extend to levels later)
        public boolean audit = true;
        public boolean perm = true;
        public boolean move = true;
        public boolean modmode = true;
        public boolean spawn = true;
    }

    public static CoreConfig defaults() {
        CoreConfig c = new CoreConfig();
        c.featureFlags.put("exampleFlag", false);

        // default role allows nothing
        Permissions p = c.permissions;
        Role def = new Role();
        p.roles.put("default", def);
        p.defaultRole = "default";
        return c;
    }
}