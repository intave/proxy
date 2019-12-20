package de.jpx3.ips.connect.database;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class DefaultQueryExecutor implements IQueryExecutor {
  private Executor executor;
  private Connection connection;
  private volatile Statement statement;

  DefaultQueryExecutor(Executor executor, Connection connection) {
    this.executor = executor;
    this.connection = connection;
  }

  public static List<Map<String, Object>> parseResultSetToTableData(ResultSet resultSet) throws SQLException {
    Preconditions.checkNotNull(resultSet);

    List<Map<String, Object>> results = Lists.newArrayList();
    ResultSetMetaData meta = resultSet.getMetaData();
    int columnCount = meta.getColumnCount();

    while (resultSet.next()) {
      Map<String, Object> row = Maps.newHashMap();
      for (int i = 0; i < columnCount; ++i) {
        int columnIndex = i + 1;
        row.put(
          meta.getColumnName(columnIndex),
          resultSet.getObject(columnIndex)
        );
      }
      results.add(row);
    }

    return results;
  }

  @Override
  public void update(String query) {
    Preconditions.checkNotNull(query);
    ensureStatementPresence();

    executor.execute(() -> {
      try {
        statement.execute(query);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  public void find(String query, Consumer<List<Map<String, Object>>> lazyReturn) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(lazyReturn);
    ensureStatementPresence();

    executor.execute(() -> {
      try {
        ResultSet resultSet = statement.executeQuery(query);
        List<Map<String, Object>> parsedData = parseResultSetToTableData(resultSet);
        lazyReturn.accept(parsedData);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  private void ensureStatementPresence() {
    if (statement == null) {
      try {
        this.statement = connection.createStatement();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
