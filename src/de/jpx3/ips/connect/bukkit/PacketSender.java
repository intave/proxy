package de.jpx3.ips.connect.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.bukkit.protocol.Packet;
import de.jpx3.ips.connect.bukkit.protocol.PacketRegister;
import de.jpx3.ips.connect.bukkit.protocol.PacketSerialisationUtilities;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static de.jpx3.ips.connect.bukkit.MessengerService.*;

public final class PacketSender {
  private IntaveProxySupportPlugin plugin;

  private PacketSender(IntaveProxySupportPlugin plugin) {
    this.plugin = plugin;
  }

  public void setup() {
    plugin.getProxy().registerChannel(OUTGOING_CHANNEL);
  }

  public void reset() {
    plugin.getProxy().unregisterChannel(OUTGOING_CHANNEL);
  }

  public void sendPacket(ProxiedPlayer player, Packet packet) {
    Preconditions.checkNotNull(player);
    Preconditions.checkNotNull(packet);

    player.sendData(OUTGOING_CHANNEL, prepareDataToSend(packet));
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

  public static PacketSender createFrom(IntaveProxySupportPlugin plugin) {
    return new PacketSender(plugin);
  }
}
