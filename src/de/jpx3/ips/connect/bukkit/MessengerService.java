package de.jpx3.ips.connect.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.bukkit.protocol.Packet;
import de.jpx3.ips.connect.bukkit.protocol.PacketRegister;
import de.jpx3.ips.connect.bukkit.protocol.PacketSerialisationUtilities;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class MessengerService {
  public final static int PROTOCOL_VERSION = 2;
  public final static String INCOMING_CHANNEL = "ipc-s2p";
  public final static String OUTGOING_CHANNEL = "ipc-p2s";
  public final static String PROTOCOL_HEADER = "IPC_BEGIN";
  public final static String PROTOCOL_FOOTER = "IPC_END";
  private final boolean enabled;
  private IntaveProxySupportPlugin plugin;
  private volatile boolean channelOpen = false;
  private Map<Class<? extends Packet>, List<IntavePacketListener>> packetListeners;

  private MessengerService(IntaveProxySupportPlugin plugin, Configuration configuration) {
    this.plugin = plugin;
    this.enabled = configuration.getBoolean("enabled");
  }

  public static MessengerService createFrom(IntaveProxySupportPlugin proxySupportPlugin, Configuration configuration) {
    Preconditions.checkNotNull(proxySupportPlugin);
    Preconditions.checkNotNull(configuration);
    return new MessengerService(proxySupportPlugin, configuration);
  }

  public void start() {
    if (enabled) {
      openChannel();
      registerInputListener();
    }
  }

  public void openChannel() {
    registerOutgoingChannel();
    setupPacketListening();
    channelOpen = true;
  }

  public void closeChannel() {
    unregisterOutgoingChannel();
    clearPacketListeners();
    channelOpen = false;
  }

  private void registerInputListener() {
    plugin.getProxy()
      .getPluginManager()
      .registerListener(plugin, IncomingMessageListener.create(this));
  }

  private void setupPacketListening() {
    packetListeners = Maps.newHashMap();

    // Loop through all packet classes
    for (Class<? extends Packet> packetClass : PacketRegister.packetTypes()) {

      // Filter outgoing packets, because we only want to listen for incoming packets
      if (PacketRegister.isPacketOutbound(packetClass)) {
        continue;
      }

      // Assign a - not yet listener holding - list to each packet class
      packetListeners.put(packetClass, new CopyOnWriteArrayList<>());
    }
  }

  private void clearPacketListeners() {
    packetListeners.clear();
  }

  private void registerOutgoingChannel() {
    plugin.getProxy().registerChannel(OUTGOING_CHANNEL);
  }

  private void unregisterOutgoingChannel() {
    plugin.getProxy().unregisterChannel(OUTGOING_CHANNEL);
  }

  public void sendPacket(ProxiedPlayer player, Packet packet) {
    Preconditions.checkNotNull(player);
    Preconditions.checkNotNull(packet);

    if (!channelOpen()) {
      return;
    }

    player.sendData(OUTGOING_CHANNEL, prepareDataToSend(packet));
  }

  public boolean channelOpen() {
    return channelOpen;
  }

  private byte[] prepareDataToSend(Packet packet) {
    // Create byte array wrapper
    ByteArrayDataOutput byteArrayWrapper = newByteArrayDataOutput();
    // Push protocol head
    pushProtocolHeader(byteArrayWrapper);
    // Push protocol version
    pushProtocolVersion(byteArrayWrapper);
    // Push packet head
    pushPacketHeader(byteArrayWrapper, packet.getClass());
    // Push packet data
    pushPacketData(byteArrayWrapper, packet);
    // Push packet footer
    pushProtocolFooter(byteArrayWrapper);
    // Extract byte array from wrapper
    return byteArrayWrapper.toByteArray();
  }

  private ByteArrayDataOutput newByteArrayDataOutput() {
    //noinspection UnstableApiUsage
    return ByteStreams.newDataOutput();
  }

  private void pushProtocolHeader(ByteArrayDataOutput byteArrayWrapper) {
    byteArrayWrapper.writeUTF(PROTOCOL_HEADER);
  }

  private void pushProtocolVersion(ByteArrayDataOutput byteArrayWrapper) {
    byteArrayWrapper.writeInt(PROTOCOL_VERSION);
  }

  private void pushPacketHeader(
    ByteArrayDataOutput byteArrayWrapper,
    Class<? extends Packet> packetClass
  ) {
    int packetId = packetIdFrom(packetClass);
    byteArrayWrapper.writeInt(packetId);
  }

  private int packetIdFrom(Class<? extends Packet> packetClass) {
    return PacketRegister.identifierOf(packetClass);
  }

  private void pushPacketData(
    ByteArrayDataOutput byteArrayWrapper,
    Packet packetToSend
  ) {
    byte[] bytes = PacketSerialisationUtilities.serializeUsing(packetToSend);
    byteArrayWrapper.write(bytes);
  }

  private void pushProtocolFooter(ByteArrayDataOutput byteArrayWrapper) {
    byteArrayWrapper.writeUTF(PROTOCOL_FOOTER);
  }

  public <T extends Packet> void addPacketListener(
    Class<T> type,
    IntavePacketListener<T> listener
  ) {
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(listener);

    packetListenersOf(type).add(listener);
  }

  public <P extends Packet> void broadcastPacketToListeners(
    ProxiedPlayer sender,
    P packet
  ) {
    Preconditions.checkNotNull(sender);
    Preconditions.checkNotNull(packet);

    packetListenersOf(packet)
      .forEach(packetListener ->
        packetListener.handle(sender, packet));
  }

  private List<IntavePacketListener> packetListenersOf(Packet packet) {
    return packetListenersOf(packet.getClass());
  }

  private List<IntavePacketListener> packetListenersOf(Class<? extends Packet> packetClass) {
    return packetListeners().get(packetClass);
  }

  public Map<Class<? extends Packet>, List<IntavePacketListener>> packetListeners() {
    return packetListeners;
  }
}
