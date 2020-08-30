package de.jpx3.ips;

import de.jpx3.ips.config.ConfigurationService;
import de.jpx3.ips.connect.bukkit.MessengerService;
import de.jpx3.ips.connect.database.DatabaseService;
import de.jpx3.ips.punish.PunishmentService;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class IntaveProxySupportPlugin extends Plugin {
  private static IntaveProxySupportPlugin singletonInstance;

  private final Executor executor = Executors.newSingleThreadExecutor();
  private DatabaseService databaseService;
  private MessengerService messengerService;
  private PunishmentService punishmentService;

  @Override
  public void onEnable() {
    singletonInstance = this;

    loadServices();
    enableServices();
  }

  @Override
  public void onDisable() {
    disableServices();
  }

  private void loadServices() {
    ConfigurationService configurationService = ConfigurationService.createFrom(this);
    Configuration configuration = configurationService.configuration();
    Configuration connectionSection = configuration.getSection("connection");
    databaseService = DatabaseService.createFrom(this, connectionSection.getSection("sql"), executor);
    messengerService = MessengerService.createFrom(this, connectionSection.getSection("bukkit"));
    punishmentService = PunishmentService.createFrom(this, configuration.getSection("punishment"));
  }

  private void enableServices() {
    databaseService.tryConnection();
    messengerService.setup();
    punishmentService.setup();
  }

  private void disableServices() {
    messengerService.closeChannel();
    databaseService.closeConnection();
  }

  public MessengerService messengerService() {
    return messengerService;
  }

  public PunishmentService punishmentService() {
    return punishmentService;
  }

  public DatabaseService databaseService() {
    return databaseService;
  }

  public static IntaveProxySupportPlugin singletonInstance() {
    return singletonInstance;
  }
}
