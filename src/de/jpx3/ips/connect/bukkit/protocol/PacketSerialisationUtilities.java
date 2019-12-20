package de.jpx3.ips.connect.bukkit.protocol;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public final class PacketSerialisationUtilities {
  private PacketSerialisationUtilities() {
    throw new UnsupportedOperationException();
  }

  public static byte[] serializeUsing(Packet packet) {
    Preconditions.checkNotNull(packet);

    //noinspection UnstableApiUsage
    ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
    packet.applyTo(dataOutput);
    return dataOutput.toByteArray();
  }

  public static Packet deserializeUsing(int packetId, ByteArrayDataInput dataInput)
    throws InstantiationException, IllegalAccessException, IllegalStateException {
    Preconditions.checkNotNull(dataInput);

    Packet packet = PacketRegister
      .classOf(packetId)
      .orElseThrow(IllegalStateException::new)
      .newInstance();
    packet.applyFrom(dataInput);
    return packet;
  }
}
