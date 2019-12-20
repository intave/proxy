package de.jpx3.ips.connect.database;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface IQueryExecutor {
  void update(String query);

  void find(String query, Consumer<List<Map<String, Object>>> lazyReturn);
}
