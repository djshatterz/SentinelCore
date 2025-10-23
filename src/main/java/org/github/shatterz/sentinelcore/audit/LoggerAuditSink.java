package org.github.shatterz.sentinelcore.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.github.shatterz.sentinelcore.config.CoreConfig;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

final class LoggerAuditSink implements AuditSink {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.AUDIT);

  static {
    MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    MAPPER.registerModule(new JavaTimeModule());
  }

  private String mode = "logger"; // future use

  @Override
  public void write(AuditEvent event) {
    try {
      String json = MAPPER.writeValueAsString(event);
      LOG.info(json);
    } catch (Exception e) {
      LOG.warn("Failed to log audit event via logger", e);
    }
  }

  @Override
  public void reconfigure(CoreConfig.Audit auditCfg) {
    this.mode = auditCfg.ledgerMode != null ? auditCfg.ledgerMode : "logger";
  }
}
