package de.jpx3.ips;

import de.jpx3.ips.config.ConfigurationService;
import de.jpx3.ips.connect.bukkit.MessengerService;
import de.jpx3.ips.connect.database.SQLService;
import de.jpx3.ips.punish.PunishmentService;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class IntaveProxySupportPlugin extends Plugin {
  private static IntaveProxySupportPlugin singletonInstance;

  private Executor executor = Executors.newSingleThreadExecutor();
  private ConfigurationService configurationService;
  private SQLService sqlService;
  private MessengerService messengerService;
  private PunishmentService punishmentService;

  public static IntaveProxySupportPlugin getSingletonInstance() {
    return singletonInstance;
  }

  @Override
  public void onEnable() {
    singletonInstance = this;

    loadConfiguration();
    loadServices();
    enableServices();
  }

  @Override
  public void onDisable() {
    disableServices();
  }

  private void loadConfiguration() {
    configurationService = ConfigurationService.createFrom(this);
  }

  private void loadServices() {
    Configuration configuration = configurationService.configuration();
    sqlService = SQLService.createFrom(this, configuration.getSection("connection.sql"), executor);
    messengerService = MessengerService.createFrom(this, configuration.getSection("connection.bukkit"));
    punishmentService = PunishmentService.createFrom(this, configuration.getSection("punishment"));
  }

  private void enableServices() {
    sqlService.openConnectionIfEnabled();
    messengerService.start();
  }

  private void disableServices() {
    messengerService.closeChannel();
    sqlService.closeConnection();
  }

  public MessengerService getMessengerService() {
    return messengerService;
  }

  public PunishmentService getPunishmentService() {
    return punishmentService;
  }

  public SQLService getSQLService() {
    return sqlService;
  }
}
