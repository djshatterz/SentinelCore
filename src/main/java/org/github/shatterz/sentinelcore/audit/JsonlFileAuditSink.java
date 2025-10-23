package org.github.shatterz.sentinelcore.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple JSONL sink with daily rotation and retention cleanup. */
final class JsonlFileAuditSink implements AuditSink {
  private static final Logger LOG = LoggerFactory.getLogger("SentinelCore/AuditSink");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    MAPPER.registerModule(new JavaTimeModule());
  }

  private Path baseDir; // <gameDir>/logs/<directory>
  private String rotation; // daily|size (size ignored for now)
  private int retentionDays;

  JsonlFileAuditSink(Path baseDir, String rotation, int retentionDays) {
    this.baseDir = baseDir;
    this.rotation = rotation;
    this.retentionDays = retentionDays;
  }

  @Override
  public void reconfigure(CoreConfig.Audit auditCfg) {
    Path base = defaultBaseDir(auditCfg.directory != null ? auditCfg.directory : "sentinelcore");
    this.baseDir = base;
    this.rotation = auditCfg.rotation != null ? auditCfg.rotation : "daily";
    this.retentionDays = auditCfg.retentionDays;
  }

  @Override
  public void write(AuditEvent event) {
    try {
      Files.createDirectories(baseDir);
      Path file = currentFile(event.ts);
      try (BufferedWriter w =
          Files.newBufferedWriter(
              file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        String json = MAPPER.writeValueAsString(event);
        w.write(json);
        w.newLine();
      }
    } catch (IOException ex) {
      LOG.error("Failed to write audit event", ex);
    }
  }

  private Path currentFile(Instant ts) {
    if ("daily".equalsIgnoreCase(rotation)) {
      LocalDate day = ts.atZone(ZoneId.systemDefault()).toLocalDate();
      String name = "audit-" + day.format(DateTimeFormatter.ISO_DATE) + ".jsonl";
      return baseDir.resolve(name);
    }
    // default fallback to daily
    LocalDate day = ts.atZone(ZoneId.systemDefault()).toLocalDate();
    String name = "audit-" + day.format(DateTimeFormatter.ISO_DATE) + ".jsonl";
    return baseDir.resolve(name);
  }

  void cleanupOldFiles() {
    if (retentionDays <= 0) return;
    try (Stream<Path> files = Files.list(baseDir)) {
      Instant cutoff = Instant.now().minusSeconds(retentionDays * 86400L);
      files
          .filter(
              p ->
                  p.getFileName().toString().startsWith("audit-")
                      && p.toString().endsWith(".jsonl"))
          .sorted(Comparator.naturalOrder())
          .forEach(
              p -> {
                try {
                  FileTime ft = Files.getLastModifiedTime(p);
                  if (ft.toInstant().isBefore(cutoff)) {
                    Files.deleteIfExists(p);
                  }
                } catch (IOException ignored) {
                }
              });
    } catch (IOException e) {
      // ignore
    }
  }

  static Path defaultBaseDir(String subdir) {
    Path game = FabricLoader.getInstance().getGameDir();
    return game.resolve("logs").resolve(subdir);
  }
}
