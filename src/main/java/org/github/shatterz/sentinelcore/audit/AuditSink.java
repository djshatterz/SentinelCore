package org.github.shatterz.sentinelcore.audit;

import org.github.shatterz.sentinelcore.config.CoreConfig;

interface AuditSink {
  void write(AuditEvent event);

  void reconfigure(CoreConfig.Audit auditCfg);
}
