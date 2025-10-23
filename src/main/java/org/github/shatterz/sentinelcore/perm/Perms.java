package org.github.shatterz.sentinelcore.perm;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.github.shatterz.sentinelcore.log.SentinelCategories;
import org.github.shatterz.sentinelcore.log.SentinelLogger;
import org.slf4j.Logger;

public final class Perms {
  private static final Logger LOG = SentinelLogger.cat(SentinelCategories.PERM);
  private static final AtomicReference<PermissionService> IMPL = new AtomicReference<>();

  private Perms() {}

  public static void install(PermissionService svc) {
    IMPL.set(svc);
    LOG.info("PermissionService installed: {}", svc != null ? svc.name() : "<none>");
  }

  public static boolean check(UUID subject, String node, Map<String, String> ctx) {
    PermissionService svc = IMPL.get();
    if (svc == null) return false;
    return svc.check(subject, node, ctx != null ? ctx : Collections.emptyMap());
  }

  public static boolean check(UUID subject, String node) {
    return check(subject, node, Collections.emptyMap());
  }
}
