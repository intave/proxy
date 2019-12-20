package de.jpx3.ips.connect.bukkit;

import de.jpx3.ips.connect.bukkit.protocol.Packet;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Class generated using IntelliJ IDEA
 * Any distribution is strictly prohibited.
 * Copyright Richard Strunk 2019
 */

public interface IPacketListener<P extends Packet> {
  void handle(ProxiedPlayer sender, P packet);
}
