package org.github.shatterz.sentinelcore.audit;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

public final class AuditManager {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.AUDIT);

  private static volatile boolean ENABLED = true;
  private static volatile Set<String> EXCLUDED = java.util.Collections.emptySet();
  private static volatile Set<String> REDACT = java.util.Collections.emptySet();
  private static volatile JsonlFileAuditSink SINK =
      new JsonlFileAuditSink(JsonlFileAuditSink.defaultBaseDir("sentinelcore"), "daily", 7);

  private AuditManager() {}

  public static void applyConfig(CoreConfig cfg) {
    if (cfg == null) return;
    CoreConfig.Audit a = cfg.audit != null ? cfg.audit : new CoreConfig.Audit();
    ENABLED = a.enabled;
    EXCLUDED = a.excludedCommands != null ? a.excludedCommands : java.util.Collections.emptySet();
    REDACT = a.redactSubstrings != null ? a.redactSubstrings : java.util.Collections.emptySet();
    Path base =
        JsonlFileAuditSink.defaultBaseDir(a.directory != null ? a.directory : "sentinelcore");
    if (SINK == null) {
      SINK = new JsonlFileAuditSink(base, a.rotation, a.retentionDays);
    } else {
      SINK.reconfigure(base, a.rotation, a.retentionDays);
    }
    SINK.cleanupOldFiles();
    LOG.info(
        "Audit configured: enabled={}, dir={}, rotation={}, retentionDays={}",
        ENABLED,
        base.toAbsolutePath(),
        a.rotation,
        a.retentionDays);
  }

  public static boolean isEnabled() {
    return ENABLED;
  }

  public static void toggle(boolean enabled) {
    ENABLED = enabled;
    LOG.info("Audit logging {}", enabled ? "enabled" : "disabled");
    // write a system event so /sclogs tail shows something immediately
    logSystem(
        "audit_toggle", enabled ? "enabled" : "disabled", java.util.Map.of("enabled", enabled));
  }

  public static void logAdminCommand(
      UUID actor, String actorName, String rawCommand, Map<String, Object> meta) {
    if (!ENABLED) return;
    if (rawCommand == null) return;

    // Exclusion by root literal
    String root = extractRoot(rawCommand);
    if (EXCLUDED.contains(root)) return;

    String redacted = applyRedaction(rawCommand);
    Map<String, Object> safeMeta = meta != null ? new HashMap<>(meta) : new HashMap<>();
    AuditEvent ev = AuditEvent.command(actor, actorName, redacted, safeMeta);
    SINK.write(ev);
  }

  public static void logSystem(String type, String message, Map<String, Object> meta) {
    if (!ENABLED) return;
    AuditEvent ev = AuditEvent.system(type, message, meta);
    SINK.write(ev);
  }

  private static String applyRedaction(String s) {
    if (s == null || REDACT.isEmpty()) return s;
    String r = s;
    for (String sub : REDACT) {
      if (sub == null || sub.isEmpty()) continue;
      r = r.replace(sub, repeat('*', Math.min(sub.length(), 8)));
    }
    return r;
  }

  private static String extractRoot(String cmd) {
    String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
    int sp = c.indexOf(' ');
    return sp > 0 ? c.substring(0, sp) : c;
  }

  private static String repeat(char ch, int n) {
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) sb.append(ch);
    return sb.toString();
  }
}
