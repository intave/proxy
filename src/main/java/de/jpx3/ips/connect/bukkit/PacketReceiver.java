package de.jpx3.ips.connect.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.jpx3.ips.IntaveProxySupportPlugin;
import de.jpx3.ips.connect.bukkit.exceptions.InvalidPacketException;
import de.jpx3.ips.connect.bukkit.exceptions.ProtocolVersionMismatchException;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import static de.jpx3.ips.connect.bukkit.MessengerService.*;

public final class PacketReceiver implements Listener {
  private final IntaveProxySupportPlugin plugin;
  private final MessengerService messengerService;

  private PacketReceiver(
    IntaveProxySupportPlugin plugin,
    MessengerService messengerService
  ) {
    this.plugin = plugin;
    this.messengerService = messengerService;
  }

  public void setup() {
    plugin.getProxy().getPluginManager().registerListener(plugin, this);
  }

  public void unset() {
    plugin.getProxy().getPluginManager().unregisterListener(this);
  }

  @SuppressWarnings("unused")
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPluginMessageReceive(PluginMessageEvent event) {
    if (isUpstream(event.getSender())) {
      return;
    }
    boolean isIntavePacket = receivePayloadPacket((ProxiedPlayer) event.getReceiver(), event.getData());
    if(isIntavePacket) {
      event.setCancelled(true);
    }
  }

  public boolean receivePayloadPacket(
    ProxiedPlayer player, byte[] data
  ) {
    ByteArrayDataInput inputData = newByteArrayDataInputFrom(data);
    try {
      String channelName = readChannelName(inputData);
      if (!channelName.equalsIgnoreCase(PROTOCOL_HEADER)) {
        return false;
      }
      int protocolVersion = readProtocolVersion(inputData);
      if (protocolVersion != PROTOCOL_VERSION) {
        String invalidVersionExceptionMessage = String.format(
          "Invalid protocol version (Ours: %s Packet: %s)",
          PROTOCOL_VERSION,
          protocolVersion
        );
        throw new ProtocolVersionMismatchException(
          invalidVersionExceptionMessage
        );
      }
      AbstractPacket constructedPacket = constructPacketFrom(inputData);
      String footer = readFooter(inputData);
      if (!footer.equalsIgnoreCase(PROTOCOL_FOOTER)) {
        throw new InvalidPacketException("Invalid end of packet");
      }
      messengerService
        .packetSubscriptionService()
        .broadcastPacketToSubscribers(
          player, constructedPacket
        );
      return true;
    } catch (IllegalStateException exception) {
      return false;
    } catch (IllegalAccessException | InstantiationException exception) {
      throw new IllegalStateException("Could not handle incoming packet", exception);
    }
  }

  private String readChannelName(ByteArrayDataInput byteArrayWrapper)
    throws IllegalStateException {
    return byteArrayWrapper.readUTF();
  }

  private int readProtocolVersion(ByteArrayDataInput byteArrayWrapper)
    throws IllegalStateException {
    return byteArrayWrapper.readInt();
  }

  private int readPacketIdentifier(ByteArrayDataInput byteArrayWrapper)
    throws IllegalStateException {
    return byteArrayWrapper.readInt();
  }

  private AbstractPacket constructPacketFrom(ByteArrayDataInput byteArrayDataInput)
    throws InstantiationException, IllegalAccessException {
    int packetId = readPacketIdentifier(byteArrayDataInput);
    return constructPacketFrom(byteArrayDataInput, packetId);
  }

  private AbstractPacket constructPacketFrom(
    ByteArrayDataInput byteArrayDataInput,
    int packetId
  ) throws IllegalAccessException, InstantiationException {
    AbstractPacket packet = PacketRegister
      .classOf(packetId)
      .orElseThrow(IllegalStateException::new)
      .newInstance();
    packet.applyFrom(byteArrayDataInput);
    return packet;
  }

  private String readFooter(ByteArrayDataInput byteArrayWrapper)
    throws IllegalStateException {
    return byteArrayWrapper.readUTF();
  }

  private ByteArrayDataInput newByteArrayDataInputFrom(byte[] byteArray) {
    //noinspection UnstableApiUsage
    return ByteStreams.newDataInput(byteArray);
  }

  private boolean isMarkedAsIntaveChannel(String channelTag) {
    return channelTag.equalsIgnoreCase(OUTGOING_CHANNEL);
  }

  private boolean isUpstream(Connection connection) {
    return connection instanceof ProxiedPlayer;
  }

  public static PacketReceiver createFrom(
    IntaveProxySupportPlugin plugin,
    MessengerService messengerService
  ) {
    return new PacketReceiver(plugin, messengerService);
  }
}
