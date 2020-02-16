package de.jpx3.ips.connect.bukkit;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface PacketSubscriber<P extends AbstractPacket> {
  void handle(ProxiedPlayer sender, P packet);
}
