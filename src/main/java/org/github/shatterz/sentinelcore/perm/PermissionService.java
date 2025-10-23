package org.github.shatterz.sentinelcore.perm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PermissionService {
  boolean check(UUID subject, String node, Map<String, String> ctx);

  String name();

  /**
   * Get the primary group/role for a player.
   *
   * @return group name, or "default" if not found
   */
  String getGroup(UUID subject);

  /** Set the primary group/role for a player. */
  void setGroup(UUID subject, String group);

  /** Get all groups a player inherits from (including their primary group). */
  List<String> getInheritedGroups(UUID subject);

  /** Check if a group exists in the permission system. */
  boolean groupExists(String group);
}
