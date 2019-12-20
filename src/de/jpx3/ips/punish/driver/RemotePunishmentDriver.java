package de.jpx3.ips.punish.driver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.database.DatabaseService;
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
  private final static String SELECTION_QUERY = "select * from `ips_ban_entries` where `ips_ban_entries`.`UniquePlayerId` = \"%s\"";
  private final static String INSERTION_QUERY = "insert into `ips_ban_entries` (`EntryId`, `UniquePlayerId`, `BanExpireTimestamp`, `BanReason`) values (NULL, \"%s\", \"%s\", \"%s\")";
  private final boolean useCaches;
  private IntaveProxySupportPlugin plugin;
  private Map<UUID, BanEntry> playerBanCache = Maps.newConcurrentMap();
  private DatabaseService service;

  private RemotePunishmentDriver(IntaveProxySupportPlugin plugin, boolean useCaches) {
    this.plugin = plugin;
    this.useCaches = useCaches;
    this.service = plugin.databaseService();
    this.plugin.getProxy().getPluginManager().registerListener(plugin, this);
  }

  @EventHandler
  public void on(PostLoginEvent postLoginEvent) {
    getBanInfoIfAvailable(postLoginEvent.getPlayer(), banEntry -> {
      if (!banEntry.expired()) {
        postLoginEvent.getPlayer().disconnect("[Intave] " + banEntry.reason());
      }
    });
  }

  @Override
  public void kickPlayer(UUID playerId, String kickMessage) {
    Preconditions.checkNotNull(playerId);
    Preconditions.checkNotNull(kickMessage);

    ProxiedPlayer player = getPlayerFrom(playerId);
    if (player == null) {
      return;
    }

    player.disconnect("[Intave] " + kickMessage);
  }

  @Override
  public void banPlayerTemporarily(UUID playerId, long endOfBanTimestamp, String banMessage) {
    Preconditions.checkNotNull(playerId);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(playerId);
    if (player == null) {
      return;
    }

    BanEntry banEntry = BanEntry.builder()
      .withId(playerId)
      .withEnd(endOfBanTimestamp)
      .withReason(banMessage)
      .construct();

    setBanEntry(banEntry);
  }

  @Override
  public void banPlayer(UUID playerId, String banMessage) {
    Preconditions.checkNotNull(playerId);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(playerId);
    if (player == null) {
      return;
    }

    BanEntry banEntry = BanEntry.builder()
      .withId(playerId)
      .withReason(banMessage)
      .withAnInfiniteDuration()
      .construct();

    setBanEntry(banEntry);
  }

  private void getBanInfoIfAvailable(ProxiedPlayer proxiedPlayer, Consumer<BanEntry> lazyReturn) {
    UUID uniqueId = proxiedPlayer.getUniqueId();
    if (useCaches) {
      if (playerBanCache.containsKey(uniqueId)) {
        lazyReturn.accept(playerBanCache.get(uniqueId));
        return;
      }
    }

    String formattedSelectionQuery = String.format(SELECTION_QUERY, uniqueId.toString());
    service.getQueryExecutor().find(formattedSelectionQuery, mappedResult -> {
      Optional<BanEntry> banEntryOptional = getFromParsedTableData(uniqueId, mappedResult);
      if (!banEntryOptional.isPresent()) {
        return;
      }

      BanEntry banEntry = banEntryOptional.get();
      if (banEntry.expired()) {
        return;
      }

      if (useCaches) {
        playerBanCache.put(uniqueId, banEntry);
      }

      lazyReturn.accept(banEntry);
    });
  }

  private Optional<BanEntry> getFromParsedTableData(UUID uuid, List<Map<String, Object>> tableData) {
    BanEntry banEntry = null;

    for (Map<String, Object> columnData : tableData) {
      long endingOn = (long) columnData.get("BanExpireTimestamp");

      if (endingOn > System.currentTimeMillis()) {
        String reason = (String) columnData.get("BanReason");

        banEntry = BanEntry.builder()
          .withId(uuid)
          .withEnd(endingOn)
          .withReason(reason)
          .construct();

        break;
      }
    }

    return Optional.ofNullable(banEntry);
  }

  private void setBanEntry(BanEntry banEntry) {
    UUID uuid = banEntry.uuid();
    if (useCaches) {
      playerBanCache.put(uuid, banEntry);
    }

    String reason = banEntry.reason();
    long end = banEntry.ending();

    String formattedInsertionQuery = String.format(
      INSERTION_QUERY,
      uuid.toString(),
      end,
      reason
    );
    service.getQueryExecutor().update(formattedInsertionQuery);
  }

  private ProxiedPlayer getPlayerFrom(UUID uuid) {
    return plugin.getProxy().getPlayer(uuid);
  }

  public static RemotePunishmentDriver createWithCachingEnabled(IntaveProxySupportPlugin plugin) {
    return new RemotePunishmentDriver(plugin, true);
  }

  public static RemotePunishmentDriver createWithCachingDisabled(IntaveProxySupportPlugin plugin) {
    return new RemotePunishmentDriver(plugin, false);
  }
}
