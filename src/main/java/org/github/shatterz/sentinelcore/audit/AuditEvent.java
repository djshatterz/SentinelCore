package org.github.shatterz.sentinelcore.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {
  public String type; // e.g., command, admin_action
  public Instant ts = Instant.now();
  public UUID actor;
  public String actorName;
  public String command; // raw or redacted
  public Map<String, Object> meta; // optional extra fields

  public static AuditEvent command(
      UUID actor, String actorName, String command, Map<String, Object> meta) {
    AuditEvent e = new AuditEvent();
    e.type = "command";
    e.actor = actor;
    e.actorName = actorName;
    e.command = command;
    e.meta = meta;
    return e;
  }
}
