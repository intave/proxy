package de.jpx3.ips.connect.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.jpx3.ips.IntaveProxySupportPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static de.jpx3.ips.connect.bukkit.MessengerService.*;

public final class PacketSender {
  private IntaveProxySupportPlugin plugin;
  private MessengerService messengerService;

  private PacketSender(IntaveProxySupportPlugin plugin, MessengerService messengerService) {
    this.plugin = plugin;
    this.messengerService = messengerService;
  }

  public void setup() {
    plugin.getProxy().registerChannel(OUTGOING_CHANNEL);
  }

  public void reset() {
    plugin.getProxy().unregisterChannel(OUTGOING_CHANNEL);
  }

  public void sendPacket(ProxiedPlayer player, AbstractPacket packet) {
    Preconditions.checkNotNull(player);
    Preconditions.checkNotNull(packet);

    messengerService
      .packetSubscriptionService()
      .broadcastPacketToSubscribers(player, packet);
    player.sendData(OUTGOING_CHANNEL, prepareDataToSend(packet));
  }

  private byte[] prepareDataToSend(AbstractPacket packet) {
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
    // Push protocol footer
    pushProtocolFooter(byteArrayWrapper);
    // Extract byte array from wrapper
    return byteArrayWrapper.toByteArray();
  }

  private void pushProtocolHeader(ByteArrayDataOutput byteArrayWrapper) {
    byteArrayWrapper.writeUTF(PROTOCOL_HEADER);
  }

  private void pushProtocolVersion(ByteArrayDataOutput byteArrayWrapper) {
    byteArrayWrapper.writeInt(PROTOCOL_VERSION);
  }

  private void pushPacketHeader(
    ByteArrayDataOutput byteArrayWrapper,
    Class<? extends AbstractPacket> packetClass
  ) {
    byteArrayWrapper.writeInt(PacketRegister.identifierOf(packetClass));
  }

  private void pushPacketData(
    ByteArrayDataOutput byteArrayWrapper,
    AbstractPacket packetToSend
  ) {
    byte[] bytes = serialize(packetToSend);
    byteArrayWrapper.write(bytes);
  }

  private byte[] serialize(AbstractPacket packet) {
    //noinspection UnstableApiUsage
    ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
    packet.applyTo(dataOutput);
    return dataOutput.toByteArray();
  }

  private void pushProtocolFooter(ByteArrayDataOutput byteArrayWrapper) {
    byteArrayWrapper.writeUTF(PROTOCOL_FOOTER);
  }

  private ByteArrayDataOutput newByteArrayDataOutput() {
    //noinspection UnstableApiUsage
    return ByteStreams.newDataOutput();
  }

  public static PacketSender createFrom(IntaveProxySupportPlugin plugin,
                                        MessengerService messengerService) {
    return new PacketSender(plugin, messengerService);
  }
}
