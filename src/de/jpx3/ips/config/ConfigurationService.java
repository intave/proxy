package de.jpx3.ips.config;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;

public final class ConfigurationService {
  private final Configuration configuration;

  private ConfigurationService(Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration configuration() {
    return configuration;
  }

  private final static String CONFIGURATION_NAME = "config.yml";
  private final static String DATAFOLDER_CREATION_ERROR = "Unable to create data folder";
  private final static String CONFIGURATION_CREATION_ERROR = "Unable to create configuration file";

  public static ConfigurationService createFrom(Plugin plugin) {
    Preconditions.checkNotNull(plugin);

    ConfigurationProvider configurationProvider =
      ConfigurationProvider.getProvider(YamlConfiguration.class);

    File dataFolder = plugin.getDataFolder();
    File configurationFile = new File(dataFolder, CONFIGURATION_NAME);

    ensureConfigurationExistence(
      dataFolder,
      configurationFile
    );

    Configuration configuration;
    try {
      configuration = configurationProvider.load(configurationFile);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return new ConfigurationService(configuration);
  }

  private static void ensureConfigurationExistence(
    File dataFolder,
    File configurationFile
  ) {
    if(!dataFolder.exists()) {
      if(!dataFolder.mkdir()) {
        System.out.println(CONFIGURATION_CREATION_ERROR);
      }
    }

    if(!configurationFile.exists()) {
      try {
        configurationFile.createNewFile();

        moveResourceToFile(
          CONFIGURATION_NAME,
          configurationFile
        );

      } catch (IOException e) {
        throw new IllegalStateException(DATAFOLDER_CREATION_ERROR, e);
      }
    }
  }

  private final static String RESOURCE_MOVE_TO_FILE_ERROR_LAYOUT = "Unable to move resource %s to %s";

  private static void moveResourceToFile(
    String resource,
    File outputFile
  ) {
    try {
      ClassLoader classLoader =
        ConfigurationService.class.getClassLoader();
      try (InputStream inputStream =
             classLoader.getResourceAsStream(resource);
           OutputStream outputStream =
             new FileOutputStream(outputFile)) {

        ByteStreams.copy(inputStream, outputStream);
      }
    } catch (IOException exception) {
      String errorMessage = String.format(
        RESOURCE_MOVE_TO_FILE_ERROR_LAYOUT,
        resource,
        outputFile.getAbsolutePath()
      );
      throw new IllegalStateException(
        errorMessage, exception
      );
    }
  }
}
