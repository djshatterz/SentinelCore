package org.github.shatterz.sentinelcore.audit;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

public final class AuditManager {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.AUDIT);

  private static volatile boolean ENABLED = true;
  private static volatile Set<String> EXCLUDED = java.util.Collections.emptySet();
  private static volatile Set<String> REDACT = java.util.Collections.emptySet();
  private static final List<AuditSink> SINKS = new CopyOnWriteArrayList<>();

  private AuditManager() {}

  public static void applyConfig(CoreConfig cfg) {
    if (cfg == null) return;
    CoreConfig.Audit a = cfg.audit != null ? cfg.audit : new CoreConfig.Audit();
    ENABLED = a.enabled;
    EXCLUDED = a.excludedCommands != null ? a.excludedCommands : java.util.Collections.emptySet();
    REDACT = a.redactSubstrings != null ? a.redactSubstrings : java.util.Collections.emptySet();
    Path base =
        JsonlFileAuditSink.defaultBaseDir(a.directory != null ? a.directory : "sentinelcore");
    // ensure file sink exists
    JsonlFileAuditSink fileSink = null;
    for (AuditSink s : SINKS) {
      if (s instanceof JsonlFileAuditSink) {
        fileSink = (JsonlFileAuditSink) s;
        break;
      }
    }
    if (fileSink == null) {
      fileSink = new JsonlFileAuditSink(base, a.rotation, a.retentionDays);
      SINKS.add(fileSink);
    }
    fileSink.reconfigure(a);
    fileSink.cleanupOldFiles();

    // ledger sink via logger
    boolean wantLedger =
        a.ledgerEnabled && (a.ledgerMode == null || a.ledgerMode.equalsIgnoreCase("logger"));
    boolean haveLedger = SINKS.stream().anyMatch(s -> s instanceof LoggerAuditSink);
    if (wantLedger && !haveLedger) {
      LoggerAuditSink ls = new LoggerAuditSink();
      ls.reconfigure(a);
      SINKS.add(ls);
    } else if (!wantLedger && haveLedger) {
      SINKS.removeIf(s -> s instanceof LoggerAuditSink);
    } else if (wantLedger) {
      SINKS.stream().filter(s -> s instanceof LoggerAuditSink).forEach(s -> s.reconfigure(a));
    }

    LOG.info(
        "Audit configured: enabled={}, dir={}, rotation={}, retentionDays={}, ledger={}",
        ENABLED,
        base.toAbsolutePath(),
        a.rotation,
        a.retentionDays,
        wantLedger ? a.ledgerMode : "off");
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
    writeAll(ev);
  }

  public static void logSystem(String type, String message, Map<String, Object> meta) {
    if (!ENABLED) return;
    AuditEvent ev = AuditEvent.system(type, message, meta);
    writeAll(ev);
  }

  private static void writeAll(AuditEvent ev) {
    for (AuditSink s : SINKS) {
      try {
        s.write(ev);
      } catch (Throwable t) {
        LOG.warn("Audit sink write failed: {}", s.getClass().getSimpleName(), t);
      }
    }
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
