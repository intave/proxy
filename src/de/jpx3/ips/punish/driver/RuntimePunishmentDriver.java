package de.jpx3.ips.punish.driver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.punish.BanEntry;
import de.jpx3.ips.punish.IPunishmentDriver;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.UUID;

public final class RuntimePunishmentDriver implements IPunishmentDriver, Listener {
  private final static String DENY_LOGIN_MESSAGE_PREFIX = "[Intave] ";

  private final Map<UUID, BanEntry> bannedPlayers = Maps.newHashMap();
  private final IntaveProxySupportPlugin plugin;

  private RuntimePunishmentDriver(IntaveProxySupportPlugin plugin) {
    this.plugin = plugin;
  }

  public void registerEvents() {
    this.plugin.getProxy()
      .getPluginManager()
      .registerListener(plugin, this);
  }

  @EventHandler
  public void onPlayerLogin(PreLoginEvent preLoginEvent) {
    PendingConnection connection = preLoginEvent.getConnection();
    UUID playerId = connection.getUniqueId();
    if (bannedPlayers.containsKey(playerId)) {
      BanEntry banEntry = bannedPlayers.get(playerId);
      if (!banEntry.expired()) {
        String banReason = banEntry.reason();
        preLoginEvent.setCancelled(true);
        preLoginEvent.setCancelReason(DENY_LOGIN_MESSAGE_PREFIX + banReason);
      }
    }
  }

  @Override
  public void kickPlayer(UUID id, String kickMessage) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(kickMessage);

    ProxiedPlayer player = getPlayerFrom(id);
    if (player == null)
      return;
    player.disconnect(DENY_LOGIN_MESSAGE_PREFIX + kickMessage);
  }

  @Override
  public void banPlayerTemporarily(UUID id, long endOfBanTimestamp, String banMessage) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(id);
    if (player == null)
      return;

    BanEntry construct = BanEntry.builder()
      .withReason(banMessage)
      .withId(id)
      .withEnd(endOfBanTimestamp)
      .build();
    bannedPlayers.put(id, construct);
    player.disconnect(DENY_LOGIN_MESSAGE_PREFIX + banMessage);
  }

  @Override
  public void banPlayer(UUID id, String banMessage) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(id);
    if (player == null)
      return;

    BanEntry banEntry = BanEntry.builder()
      .withReason(banMessage)
      .withId(id)
      .withAnInfiniteDuration()
      .build();
    bannedPlayers.put(id, banEntry);
    player.disconnect("[Intave] " + banMessage);
  }

  private ProxiedPlayer getPlayerFrom(UUID uuid) {
    return plugin.getProxy().getPlayer(uuid);
  }

  public static RuntimePunishmentDriver createFrom(IntaveProxySupportPlugin plugin) {
    RuntimePunishmentDriver driver = new RuntimePunishmentDriver(plugin);
    driver.registerEvents();
    return driver;
  }
}
