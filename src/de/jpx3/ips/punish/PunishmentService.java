package de.jpx3.ips.punish;

import com.google.common.base.Preconditions;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.bukkit.packets.PacketInPunishmentRequest;
import de.jpx3.ips.punish.driver.RemotePunishmentDriver;
import de.jpx3.ips.punish.driver.RuntimePunishmentDriver;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.UUID;

public final class PunishmentService {
  private final IntaveProxySupportPlugin plugin;
  private final Configuration configuration;

  public final static String BAN_LAYOUT_CONFIGURATION_KEY = "message-layout.ban-layout";
  public final static String KICK_LAYOUT_CONFIGURATION_KEY = "message-layout.kick-layout";

  private PunishmentDriver punishmentDriver;

  private PunishmentService(IntaveProxySupportPlugin plugin,
                            Configuration configuration
  ) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  public void setup() {
    setupPunishmentDriver();
    setupSubscriptions();
  }

  private void setupSubscriptions() {
    plugin.messengerService()
      .packetSubscriptionService()
      .addSubscriber(
        PacketInPunishmentRequest.class,
        this::processPunishmentPacket
      );
  }

  private void processPunishmentPacket(ProxiedPlayer sender,
                                       PacketInPunishmentRequest packet
  ) {
    UUID id = packet.id();
    String message = packet.message();

    if(message.length() > 64) {
      message = message.substring(0, 64);
    }

    switch (packet.punishmentType()) {
      case BAN:
        punishmentDriver.
          banPlayer(id, message);
        break;
      case KICK:
        punishmentDriver.
          kickPlayer(id, message);
        break;
      case TEMP_BAN:
        punishmentDriver.
          banPlayerTemporarily(id, packet.endTimestamp(), message);
        break;
    }
  }

  private void setupPunishmentDriver() {
    String desiredDriverName = desiredPunishmentDriverName();
    this.punishmentDriver = loadDriverFrom(desiredDriverName);
  }

  private final static String DRIVER_NAME_RUNTIME     = "runtime";
  private final static String DRIVER_NAME_SQL_CACHED  = "sql";
  private final static String DRIVER_NAME_SQL_NOCACHE = "sql-nc";

  private PunishmentDriver loadDriverFrom(String driverName) {
    Preconditions.checkNotNull(driverName);

    PunishmentDriver punishmentDriver;

    switch (driverName.toLowerCase()) {
      case DRIVER_NAME_RUNTIME:
        punishmentDriver = RuntimePunishmentDriver.createFrom(plugin);
        break;
      case DRIVER_NAME_SQL_CACHED:
        punishmentDriver = RemotePunishmentDriver.createWithCachingEnabled(plugin);
        break;
      case DRIVER_NAME_SQL_NOCACHE:
        punishmentDriver = RemotePunishmentDriver.createWithCachingDisabled(plugin);
        break;

      default:
        throw new IllegalStateException("Could not find driver " + driverName);
    }

    return punishmentDriver;
  }

  public String resolveMessageBy(String configurationKey,
                                 BanEntry banEntry
  ) {
    String layout = configuration.getString(configurationKey);
    return MessageFormatter.formatMessage(layout, banEntry);
  }

  public PunishmentDriver punishmentDriver() {
    return punishmentDriver;
  }

  public String desiredPunishmentDriverName() {
    return configuration.getString("driver", "runtime");
  }

  public void setPunishmentDriver(PunishmentDriver punishmentDriver) {
    this.punishmentDriver = punishmentDriver;
  }

  public static PunishmentService createFrom(IntaveProxySupportPlugin plugin,
                                             Configuration configuration
  ) {
    return new PunishmentService(plugin, configuration);
  }
}
