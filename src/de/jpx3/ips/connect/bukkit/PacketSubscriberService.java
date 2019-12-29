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
public final class PacketSubscriberService {
  private IntaveProxySupportPlugin plugin;
  private Map<Class<? extends Packet>, List<IPacketSubscriber>> packetSubscriber;

  private PacketSubscriberService(IntaveProxySupportPlugin plugin) {
    this.plugin = plugin;
  }

  public void setup() {
    packetSubscriber = Maps.newHashMap();

    // Loop through all packet classes
    for (Class<? extends Packet> packetClass : PacketRegister.packetTypes()) {

      // Filter outgoing packets, because we only want to listen for incoming packets
      if (PacketRegister.packetOutbound(packetClass)) {
        continue;
      }

      // Assign a empty list to each packet class
      packetSubscriber.put(packetClass, new CopyOnWriteArrayList<>());
    }
  }

  public void reset() {
    packetSubscriber.clear();
  }

  public <T extends Packet> void addSubscriber(
    Class<T> type,
    IPacketSubscriber<T> listener
  ) {
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(listener);

    packetSubscribersOf(type).add(listener);
  }

  public <P extends Packet> void broadcastPacketToSubscribers(
    ProxiedPlayer sender,
    P packet
  ) {
    Preconditions.checkNotNull(sender);
    Preconditions.checkNotNull(packet);

    packetSubscribersOf(packet)
      .forEach(packetListener ->
        packetListener.handle(sender, packet));
  }

  private List<IPacketSubscriber> packetSubscribersOf(Packet packet) {
    return packetSubscribersOf(packet.getClass());
  }

  private List<IPacketSubscriber> packetSubscribersOf(Class<? extends Packet> packetClass) {
    return packetSubscriber().get(packetClass);
  }

  public Map<Class<? extends Packet>, List<IPacketSubscriber>> packetSubscriber() {
    return packetSubscriber;
  }

  public static PacketSubscriberService createFrom(IntaveProxySupportPlugin plugin) {
    return new PacketSubscriberService(plugin);
  }
}
