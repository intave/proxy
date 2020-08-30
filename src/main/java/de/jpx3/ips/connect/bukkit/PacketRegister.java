package de.jpx3.ips.connect.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.jpx3.ips.connect.bukkit.packets.*;

import java.util.*;

public final class PacketRegister {
  private static final Map<Integer, Class<? extends AbstractPacket>> packetIdToPacketClassMap;

  static {
    Map<Integer, Class<? extends AbstractPacket>> packetMap = new HashMap<>();

    packetMap.put(0, PacketInVersionInfo.class);
    packetMap.put(1, PacketInCommandExecution.class);
    packetMap.put(2, PacketInPunishmentRequest.class);
    packetMap.put(3, PacketInKCVAction.class);

    packetMap.put(100, PacketOutVersionRequest.class);

    packetIdToPacketClassMap = ImmutableMap.copyOf(packetMap);
  }

  public static Map<Integer, Class<? extends AbstractPacket>> packetIdToClassMap() {
    return packetIdToPacketClassMap;
  }

  public static Collection<Class<? extends AbstractPacket>> packetTypes() {
    return Collections.unmodifiableCollection(
      packetIdToPacketClassMap.values()
    );
  }

  public static Optional<Class<? extends AbstractPacket>> classOf(int packetId) {
    Class<? extends AbstractPacket> value = packetIdToPacketClassMap.get(packetId);
    return Optional.ofNullable(value);
  }

  public static boolean packetOutbound(Class<? extends AbstractPacket> packetClass) {
    Preconditions.checkNotNull(packetClass);
    return identifierOf(packetClass) >= 100;
  }

  public static int identifierOf(Class<? extends AbstractPacket> packetClass) {
    Preconditions.checkNotNull(packetClass);
    return packetIdToPacketClassMap
      .entrySet()
      .stream()
      .filter(integerClassEntry ->
        integerClassEntry.getValue().equals(packetClass))
      .findFirst()
      .map(Map.Entry::getKey)
      .orElseThrow(NullPointerException::new);
  }
}
