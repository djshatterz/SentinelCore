package org.github.shatterz.sentinelcore.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SentinelLogger {
    private static final Logger ROOT = LoggerFactory.getLogger("SentinelCore");
    private static final Map<SentinelCategories, Logger> BY_CAT = new EnumMap<>(SentinelCategories.class);
    private static final Map<String, Logger> BY_ID = new ConcurrentHashMap<>();

    static {
        for (SentinelCategories c : SentinelCategories.values()) {
            BY_CAT.put(c, LoggerFactory.getLogger("SentinelCore/" + c.id()));
        }
    }

    private SentinelLogger(){}

    public static Logger root() { return ROOT; }
    public static Logger cat(SentinelCategories cat){ return BY_CAT.get(cat); }
    public static Logger cat(String id){
        return BY_ID.computeIfAbsent(id, k -> LoggerFactory.getLogger("SentinelCore/" + k));
    }
}
