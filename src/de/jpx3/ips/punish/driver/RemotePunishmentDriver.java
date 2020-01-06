package de.jpx3.ips.punish.driver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.database.DatabaseService;
import de.jpx3.ips.punish.BanEntry;
import de.jpx3.ips.punish.IPunishmentDriver;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class RemotePunishmentDriver implements IPunishmentDriver, Listener {
  private final static String TABLE_NAME = "ips_ban_entries";
  private final static String TABLE_SETUP_QUERY = "create table if not exists `%s`.`"+TABLE_NAME+"` ( `EntryId` INT NOT NULL AUTO_INCREMENT ,`UniquePlayerId` VARCHAR(36) NOT NULL ,`BanExpireTimestamp` BIGINT NOT NULL ,`BanReason` VARCHAR NOT NULL ,PRIMARY KEY (`EntryId`)) ENGINE = InnoDB;";
  private final static String SELECTION_QUERY = "select * from `"+TABLE_NAME+"` where `"+TABLE_NAME+"`.`UniquePlayerId` = \"%s\"";
  private final static String INSERTION_QUERY = "insert into `"+TABLE_NAME+"` (`EntryId`, `UniquePlayerId`, `BanExpireTimestamp`, `BanReason`) values (NULL, \"%s\", \"%s\", \"%s\")";
  private final static String DENY_LOGIN_MESSAGE_PREFIX = "[Intave] ";

  private IntaveProxySupportPlugin plugin;
  private Map<UUID, BanEntry> playerBanCache = Maps.newConcurrentMap();
  private DatabaseService service;
  private final boolean useCaches;

  private RemotePunishmentDriver(IntaveProxySupportPlugin plugin,
                                 boolean useCaches
  ) {
    this.plugin = plugin;
    this.useCaches = useCaches;
    this.service = plugin.databaseService();
  }

  public void registerEvents() {
    this.plugin.getProxy()
      .getPluginManager()
      .registerListener(plugin, this);
  }

  public void setupTableData() {
    if(service.shouldCreateTables()) {
      String tableCreationQuery = String.format(
        TABLE_SETUP_QUERY,
        service.database()
      );
      updateQuery(tableCreationQuery);
    }
  }

  @EventHandler
  public void onPlayerLogin(PostLoginEvent postLoginEvent) {
    resolveBanInfo(postLoginEvent.getPlayer(), banEntry -> {
      if (banEntry != null && !banEntry.expired()) {
        String denyReason = DENY_LOGIN_MESSAGE_PREFIX + banEntry.reason();
        postLoginEvent.getPlayer().disconnect(denyReason);
      }
    });
  }

  @Override
  public void kickPlayer(UUID id, String kickMessage) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(kickMessage);

    ProxiedPlayer player = getPlayerFrom(id);
    if (player == null) {
      return;
    }
    player.disconnect(DENY_LOGIN_MESSAGE_PREFIX + kickMessage);
  }

  @Override
  public void banPlayerTemporarily(UUID id, long endOfBanTimestamp, String banMessage) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(id);
    if (player == null) {
      return;
    }

    BanEntry banEntry = BanEntry.builder()
      .withId(id)
      .withEnd(endOfBanTimestamp)
      .withReason(banMessage)
      .build();
    activateBan(banEntry);
  }

  @Override
  public void banPlayer(UUID id, String banMessage) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(id);
    if (player == null) {
      return;
    }

    BanEntry banEntry = BanEntry.builder()
      .withId(id)
      .withReason(banMessage)
      .withAnInfiniteDuration()
      .build();
    activateBan(banEntry);
  }

  private void activateBan(BanEntry banEntry) {
    UUID id = banEntry.id();
    if (useCaches) {
      setInCache(id, banEntry);
    }

    String reason = banEntry.reason();
    long end = banEntry.ending();

    String formattedInsertionQuery = String.format(
      INSERTION_QUERY,
      id.toString(),
      end,
      reason
    );
    updateQuery(formattedInsertionQuery);
  }

  private void resolveBanInfo(ProxiedPlayer proxiedPlayer,
                              Consumer<BanEntry> lazyReturn
  ) {
    Preconditions.checkNotNull(proxiedPlayer);
    Preconditions.checkNotNull(lazyReturn);

    UUID id = proxiedPlayer.getUniqueId();

    if (useCaches() && isInCache(id)) {
      lazyReturn.accept(getFromCache(id));
      return;
    }

    String queryString = String.format(SELECTION_QUERY, id.toString());
    findByQuery(queryString, mappedResult -> {
      Optional<BanEntry> banSearch = searchActiveBan(id, mappedResult);
      if (!banSearch.isPresent()) {
        return;
      }
      BanEntry banEntry = banSearch.get();
      if (banEntry.expired()) {
        return;
      }
      if (useCaches()) {
        setInCache(id, banEntry);
      }
      lazyReturn.accept(banEntry);
    });
  }

  private final static String COLUMN_NAME_EXPIRATION = "BanExpireTimestamp";
  private final static String COLUMN_NAME_REASON     = "BanReason";

  private Optional<BanEntry> searchActiveBan(UUID id,
                                             List<Map<String, Object>> tableData
  ) {
    for (Map<String, Object> columnData : tableData) {
      long endingOn = (long) columnData.get(COLUMN_NAME_EXPIRATION);
      if (!entryHasExpired(endingOn)) {
        String reason = (String) columnData.get(COLUMN_NAME_REASON);
        BanEntry banEntry = BanEntry.builder()
          .withId(id)
          .withEnd(endingOn)
          .withReason(reason)
          .build();
        return Optional.of(banEntry);
      }
    }
    return Optional.empty();
  }

  private void updateQuery(String queryCommand) {
    service.getQueryExecutor().update(queryCommand);
  }

  private void findByQuery(String searchCommand,
                           Consumer<List<Map<String, Object>>> lazyReturn
  ) {
    service.getQueryExecutor().find(searchCommand, lazyReturn);
  }

  private boolean entryHasExpired(long entryEnd) {
    return System.currentTimeMillis() > entryEnd;
  }

  private BanEntry getFromCache(UUID id) {
    return playerBanCache.get(id);
  }

  private void setInCache(UUID id, BanEntry banEntry) {
    playerBanCache.put(id, banEntry);
  }

  private boolean isInCache(UUID id) {
    return playerBanCache.containsKey(id);
  }

  private boolean useCaches() {
    return useCaches;
  }

  private ProxiedPlayer getPlayerFrom(UUID uuid) {
    return plugin.getProxy().getPlayer(uuid);
  }

  public static RemotePunishmentDriver createWithCachingEnabled(IntaveProxySupportPlugin plugin) {
    RemotePunishmentDriver driver = new RemotePunishmentDriver(plugin, true);
    driver.setupTableData();
    driver.registerEvents();
    return driver;
  }

  public static RemotePunishmentDriver createWithCachingDisabled(IntaveProxySupportPlugin plugin) {
    RemotePunishmentDriver driver = new RemotePunishmentDriver(plugin, false);
    driver.setupTableData();
    driver.registerEvents();
    return driver;
  }
}
