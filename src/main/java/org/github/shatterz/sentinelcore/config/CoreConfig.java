package org.github.shatterz.sentinelcore.config;

import java.util.HashMap;
import java.util.Map;

public class CoreConfig {
    public Map<String, Boolean> featureFlags = new HashMap<>();
    public Logging logging = new Logging();

    public static class Logging {
        // per-category on/off (you can extend to levels later)
        public boolean audit = true;
        public boolean perm = true;
        public boolean move = true;
        public boolean modmode = true;
        public boolean spawn = true;
    }

    public static CoreConfig defaults(){
        CoreConfig c = new CoreConfig();
        c.featureFlags.put("exampleFlag", false);
        return c;
    }
}
