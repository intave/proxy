package de.jpx3.ips.connect.database;

import com.google.common.base.Preconditions;
import de.jpx3.ips.IntaveProxySupportPlugin;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;

public final class SQLService {
  private final Configuration configuration;
  private final Executor executor;
  private IntaveProxySupportPlugin plugin;
  private Connection connection;
  private SQLQueryExecutor queryExecutor;

  private SQLService(IntaveProxySupportPlugin plugin, Configuration configuration, Executor executor) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.executor = executor;
  }

  public static SQLService createFrom(IntaveProxySupportPlugin plugin, Configuration configuration, Executor executor) {
    Preconditions.checkNotNull(plugin);
    Preconditions.checkNotNull(configuration);
    Preconditions.checkNotNull(executor);

    return new SQLService(plugin, configuration, executor);
  }

  public void openConnectionIfEnabled() {
    if (
      queryExecutor != null ||
        !configuration.getBoolean("enabled")
    ) {
      return;
    }

    Configuration connectionSection = configuration.getSection("connection");
    String service = connectionSection.getString("jdbc-service");
    String host = connectionSection.getString("host");
    int port = connectionSection.getInt("port");
    String database = connectionSection.getString("database");
    String user = connectionSection.getString("user");
    String password = connectionSection.getString("password");

    String connectionUrl = "jdbc:" + service + "://" + host + ":" + port + ";databaseName=" + database + ";user=" + user + ";password=" + password + "?autoReconnect=true";

    try (Connection connection = this.connection = DriverManager.getConnection(connectionUrl)) {
      if (configuration.getBoolean("create-tables", true)) {
        String tableCreationQuery = "CREATE TABLE IF NOT EXISTS `" +
          database + "`.`ips_ban_entries` " +
          "( `EntryId` INT NOT NULL AUTO_INCREMENT ," +
          " `UniquePlayerId` VARCHAR(36) NOT NULL ," +
          " `BanExpireTimestamp` BIGINT NOT NULL ," +
          " `BanReason` VARCHAR NOT NULL ," +
          " PRIMARY KEY (`EntryId`) " +
          ") ENGINE = InnoDB;";

        connection.createStatement().execute(tableCreationQuery);
      }

      queryExecutor = new DefaultQueryExecutor(executor, connection);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void closeConnection() {
    try {
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean isConnected() {
    try {
      return !connection.isClosed();
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public SQLQueryExecutor getQueryExecutor() {
    return queryExecutor;
  }

  public void setQueryExecutor(SQLQueryExecutor queryExecutor) {
    Preconditions.checkNotNull(queryExecutor);

    this.queryExecutor = queryExecutor;
  }
}
