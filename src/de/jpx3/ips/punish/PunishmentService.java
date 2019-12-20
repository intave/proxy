package de.jpx3.ips.punish;

import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.bukkit.protocol.packets.PacketInPunishmentRequest;
import de.jpx3.ips.punish.driver.PunishmentDriver;
import de.jpx3.ips.punish.driver.RemotePunishmentDriver;
import de.jpx3.ips.punish.driver.RuntimePunishmentDriver;
import net.md_5.bungee.config.Configuration;

public final class PunishmentService {
  private IntaveProxySupportPlugin plugin;
  private PunishmentDriver punishmentDriver;

  private PunishmentService(IntaveProxySupportPlugin plugin, Configuration configuration) {
    this.plugin = plugin;
    this.punishmentDriver = resolveFrom(configuration);

    this.hookPacketListener();
  }

  private void hookPacketListener() {
    plugin.messengerService().addPacketListener(PacketInPunishmentRequest.class, (sender, packet) -> {
      switch (packet.getPunishmentType()) {
        case BAN:
          punishmentDriver.banPlayer(packet.getPlayerId(), packet.getMessage());
          break;
        case KICK:
          punishmentDriver.kickPlayer(packet.getPlayerId(), packet.getMessage());
          break;
        case TEMP_BAN:
          punishmentDriver.banPlayerTemporarily(packet.getPlayerId(), packet.getTempbanEndTimestamp(), packet.getMessage());
          break;
      }
    });
  }

  private PunishmentDriver resolveFrom(Configuration configuration) {
    String driverIdentifier = configuration.getString("driver", "runtime");
    PunishmentDriver punishmentDriver;

    switch (driverIdentifier.toLowerCase()) {
      case "runtime":
        punishmentDriver = RuntimePunishmentDriver.createFrom(plugin);
        break;
      case "sql":
        punishmentDriver = RemotePunishmentDriver.createWithCachingEnabled(plugin);
        break;
      case "sql-nc":
        punishmentDriver = RemotePunishmentDriver.createWithCachingDisabled(plugin);
        break;

      default:
        throw new IllegalStateException("Could not find driver " + driverIdentifier);
    }

    return punishmentDriver;
  }

  public PunishmentDriver getPunishmentDriver() {
    return punishmentDriver;
  }

  public void setPunishmentDriver(PunishmentDriver punishmentDriver) {
    this.punishmentDriver = punishmentDriver;
  }

  public static PunishmentService createFrom(IntaveProxySupportPlugin plugin, Configuration configuration) {
    return new PunishmentService(plugin, configuration);
  }
}
