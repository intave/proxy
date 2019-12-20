package de.jpx3.ips.config;

import com.google.common.base.Preconditions;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class ConfigurationService {
  private final Configuration configuration;

  private ConfigurationService(Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration configuration() {
    return configuration;
  }

  public static ConfigurationService createFrom(Plugin plugin) {
    Preconditions.checkNotNull(plugin);

    return createFrom(plugin.getDataFolder());
  }

  public static ConfigurationService createFrom(File dataFolder) {
    Preconditions.checkNotNull(dataFolder);

    return createFrom(ConfigurationProvider.getProvider(YamlConfiguration.class), dataFolder);
  }

  public static ConfigurationService createFrom(ConfigurationProvider configurationProvider, File dataFolder) {
    Preconditions.checkNotNull(configurationProvider);
    Preconditions.checkNotNull(dataFolder);

    try {
      Configuration configuration = configurationProvider.load(new File(dataFolder, "config.yml"));
      return new ConfigurationService(configuration);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
