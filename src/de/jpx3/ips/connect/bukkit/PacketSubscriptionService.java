package de.jpx3.ips.connect.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.bukkit.protocol.Packet;
import de.jpx3.ips.connect.bukkit.protocol.PacketRegister;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class PacketSubscriptionService {
  private IntaveProxySupportPlugin plugin;
  private Map<Class<? extends Packet>, List<IPacketSubscriber>> packetSubscriptions;

  private PacketSubscriptionService(IntaveProxySupportPlugin plugin) {
    this.plugin = plugin;
  }

  public void setup() {
    packetSubscriptions = Maps.newHashMap();

    // Loop through all packet classes
    for (Class<? extends Packet> packetClass : PacketRegister.packetTypes()) {

      // Filter outgoing packets, because we only want to listen for incoming packets
      if (PacketRegister.packetOutbound(packetClass)) {
        continue;
      }

      // Assign a empty list to each packet class
      packetSubscriptions.put(packetClass, new CopyOnWriteArrayList<>());
    }
  }

  public void reset() {
    packetSubscriptions.clear();
  }

  public <T extends Packet> void addSubscription(
    Class<T> type,
    IPacketSubscriber<T> subscriber
  ) {
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(subscriber);

    subscriptionsOf(type).add(subscriber);
  }

  public <P extends Packet> void broadcastPacketToSubscribers(
    ProxiedPlayer sender,
    P packet
  ) {
    Preconditions.checkNotNull(sender);
    Preconditions.checkNotNull(packet);

    subscriptionsOf(packet)
      .forEach(packetSubscriber ->
        packetSubscriber.handle(sender, packet));
  }

  private List<IPacketSubscriber> subscriptionsOf(Packet packet) {
    return subscriptionsOf(packet.getClass());
  }

  private List<IPacketSubscriber> subscriptionsOf(Class<? extends Packet> packetClass) {
    return packetSubscriber().get(packetClass);
  }

  public Map<Class<? extends Packet>, List<IPacketSubscriber>> packetSubscriber() {
    return packetSubscriptions;
  }

  public static PacketSubscriptionService createFrom(IntaveProxySupportPlugin plugin) {
    return new PacketSubscriptionService(plugin);
  }
}
