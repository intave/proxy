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

    ConfigurationProvider configurationProvider =
      ConfigurationProvider.getProvider(YamlConfiguration.class);
    File configurationFile
      = new File(plugin.getDataFolder(), "config.yml");

    Configuration configuration;
    try {
      configuration = configurationProvider.load(configurationFile);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return new ConfigurationService(configuration);
  }
}
