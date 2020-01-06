package de.jpx3.ips.connect.database;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class AsyncQueryExecutor implements IQueryExecutor {
  private final Executor executor;
  private final Connection connection;
  private final String databaseName;

  private volatile Statement statement;

  AsyncQueryExecutor(Executor executor,
                     Connection connection,
                     String databaseName
  ) {
    this.executor = executor;
    this.connection = connection;
    this.databaseName = databaseName;
  }

  @Override
  public void update(String query) {
    Preconditions.checkNotNull(query);
    ensureStatementPresence();

    pushToExecutor(() -> {
      try {
        statement.execute(query);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  public void find(String query,
                   Consumer<List<Map<String, Object>>> lazyReturn
  ) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(lazyReturn);
    ensureStatementPresence();

    pushToExecutor(() -> {
      try {
        ResultSet resultSet = statement.executeQuery(query);
        lazyReturn.accept(asTableData(resultSet));
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  private void pushToExecutor(Runnable runnable) {
    Preconditions.checkNotNull(runnable);

    executor.execute(runnable);
  }

  private void ensureStatementPresence() {
    if (statement == null) {
      statement = createStatement();
    }
  }

  private Statement createStatement() {
    try {
      return connection.createStatement();
    } catch (SQLException e) {
      throw new IllegalStateException();
    }
  }

  private List<Map<String, Object>> asTableData(ResultSet resultSet)
    throws SQLException {
    Preconditions.checkNotNull(resultSet);

    List<Map<String, Object>> results = Lists.newArrayList();

    while (resultSet.next()) {
      results.add(collectSelectedRowOf(resultSet));
    }
    return results;
  }

  private Map<String, Object> collectSelectedRowOf(ResultSet resultSet)
    throws SQLException {
    Preconditions.checkNotNull(resultSet);

    ResultSetMetaData meta = resultSet.getMetaData();
    Map<String, Object> row = Maps.newHashMap();

    int columnCount = meta.getColumnCount();

    for (int i = 0; i < columnCount; ++i) {
      int columnIndex = i + 1;
      row.put(
        meta.getColumnName(columnIndex),
        resultSet.getObject(columnIndex)
      );
    }

    return row;
  }

  public String database() {
    return databaseName;
  }
}
