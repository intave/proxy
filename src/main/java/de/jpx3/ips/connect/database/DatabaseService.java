package de.jpx3.ips.connect.database;

import com.google.common.base.Preconditions;
import de.jpx3.ips.IntaveProxySupportPlugin;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;

public final class DatabaseService {
  private static final String CONNECTION_URL_LAYOUT = "jdbc:%s://%s:%s/%s?user=%s&password=%s&autoReconnect=true";

  private final IntaveProxySupportPlugin plugin;
  private final Configuration configuration;
  private final Executor executor;

  private Connection connection;
  private IQueryExecutor queryExecutor;
  private String database;

  private DatabaseService(IntaveProxySupportPlugin plugin,
                          Configuration configuration,
                          Executor executor
  ) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.executor = executor;
  }

  private static final String CONFIG_KEY_SERVICE  = "jdbc-service";
  private static final String CONFIG_KEY_HOST     = "host";
  private static final String CONFIG_KEY_PORT     = "port";
  private static final String CONFIG_KEY_DATABASE = "database";
  private static final String CONFIG_KEY_USER     = "user";
  private static final String CONFIG_KEY_PASSWORD = "password";

  public void tryConnection() {
    if (!shouldConnect()) {
      return;
    }

    Configuration config = configuration.getSection("connection");

    String service  = config.getString(CONFIG_KEY_SERVICE);
    String host     = config.getString(CONFIG_KEY_HOST);
    int    port     = config.getInt   (CONFIG_KEY_PORT);
    String database = config.getString(CONFIG_KEY_DATABASE);
    String user     = config.getString(CONFIG_KEY_USER);
    String password = config.getString(CONFIG_KEY_PASSWORD);

    String connectionURL =
      parseConnectionUrlFrom(service, host, port, database, user, password);

    try {
      this.connection = tryConnection(connectionURL);
      this.queryExecutor = new AsyncQueryExecutor(executor, this.connection, database);
      this.database = database;
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

  private Connection tryConnection(String connectionURL)
    throws SQLException {
    return DriverManager.getConnection(connectionURL);
  }

  private String parseConnectionUrlFrom(String service, String host, int port,
                                        String database, String user, String password
  ) {
    return String.format(
      CONNECTION_URL_LAYOUT,
      service, host, port,
      database, user, password
    );
  }

  public boolean shouldCreateTables() {
    return configuration.getBoolean("create-tables", true);
  }

  public boolean shouldConnect() {
    return configuration.getBoolean("enabled");
  }

  public IQueryExecutor getQueryExecutor() {
    return queryExecutor;
  }

  public void setQueryExecutor(IQueryExecutor queryExecutor) {
    Preconditions.checkNotNull(queryExecutor);

    this.queryExecutor = queryExecutor;
  }

  public String database() {
    return database;
  }

  public static DatabaseService createFrom(IntaveProxySupportPlugin plugin,
                                           Configuration configuration,
                                           Executor executor
  ) {
    Preconditions.checkNotNull(plugin);
    Preconditions.checkNotNull(configuration);
    Preconditions.checkNotNull(executor);

    return new DatabaseService(plugin, configuration, executor);
  }
}
