package de.jpx3.ips.punish.driver;

import com.google.common.base.Preconditions;
import de.jpx3.ips.IntaveProxySupportPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RuntimePunishmentDriver implements PunishmentDriver, Listener {
  private Map<UUID, BanEntry> bannedPlayers = new HashMap<>();
  private IntaveProxySupportPlugin plugin;

  private RuntimePunishmentDriver(IntaveProxySupportPlugin plugin) {
    this.plugin = plugin;
    this.plugin.getProxy()
      .getPluginManager()
      .registerListener(plugin, this);
  }

  @EventHandler
  public void on(PreLoginEvent preLoginEvent) {
    UUID playerId = preLoginEvent.getConnection().getUniqueId();
    if (bannedPlayers.containsKey(playerId)) {
      BanEntry banEntry = bannedPlayers.get(playerId);

      if (!banEntry.expired()) {
        String banReason = banEntry.reason();
        preLoginEvent.setCancelled(true);
        preLoginEvent.setCancelReason("[Intave] " + banReason);
      }
    }
  }

  @Override
  public void kickPlayer(UUID playerId, String kickMessage) {
    Preconditions.checkNotNull(playerId);
    Preconditions.checkNotNull(kickMessage);

    ProxiedPlayer player = getPlayerFrom(playerId);

    if (player == null)
      return;

    player.disconnect("[Intave] " + kickMessage);
  }

  @Override
  public void banPlayerTemporarily(UUID playerId, long endOfBanTimestamp, String banMessage) {
    Preconditions.checkNotNull(playerId);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(playerId);

    if (player == null)
      return;

    BanEntry construct = BanEntry.builder()
      .withReason(banMessage)
      .withId(playerId)
      .withEnd(endOfBanTimestamp)
      .construct();

    bannedPlayers.putIfAbsent(playerId, construct);
    player.disconnect("[Intave] " + banMessage);
  }

  @Override
  public void banPlayer(UUID playerId, String banMessage) {
    Preconditions.checkNotNull(playerId);
    Preconditions.checkNotNull(banMessage);

    ProxiedPlayer player = getPlayerFrom(playerId);

    if (player == null)
      return;

    BanEntry banEntry = BanEntry.builder()
      .withReason(banMessage)
      .withId(playerId)
      .withAnInfiniteDuration()
      .construct();

    bannedPlayers.put(playerId, banEntry);
    player.disconnect("[Intave] " + banMessage);
  }

  private ProxiedPlayer getPlayerFrom(UUID uuid) {
    return plugin.getProxy().getPlayer(uuid);
  }

  public static RuntimePunishmentDriver createFrom(IntaveProxySupportPlugin plugin) {
    return new RuntimePunishmentDriver(plugin);
  }
}
