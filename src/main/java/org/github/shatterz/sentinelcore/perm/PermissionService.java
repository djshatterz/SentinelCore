package org.github.shatterz.sentinelcore.perm;

import java.util.Map;
import java.util.UUID;

public interface PermissionService {
  boolean check(UUID subject, String node, Map<String, String> ctx);

  String name();
}
