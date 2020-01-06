package de.jpx3.ips.punish;

import java.util.UUID;

public interface IPunishmentDriver {
  void kickPlayer(UUID playerId, String kickMessage);
  void banPlayerTemporarily(UUID playerId, long endOfBanTimestamp, String banMessage);
  void banPlayer(UUID playerId, String banMessage);
}
