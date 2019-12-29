package de.jpx3.ips.connect.bukkit;

import de.jpx3.ips.connect.bukkit.protocol.Packet;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface IPacketSubscriber<P extends Packet> {
  void handle(ProxiedPlayer sender, P packet);
}
