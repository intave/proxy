package de.jpx3.ips.punish;

import java.util.UUID;

public interface PunishmentDriver {
  void kickPlayer(UUID id, String kickMessage);
  void banPlayerTemporarily(UUID id, long endOfBanTimestamp, String banMessage);
  void banPlayer(UUID id, String banMessage);
}
