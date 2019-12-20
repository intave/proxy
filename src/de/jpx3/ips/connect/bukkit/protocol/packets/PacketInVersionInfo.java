package de.jpx3.ips.connect.bukkit.protocol.packets;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import de.jpx3.ips.connect.bukkit.protocol.Packet;

public final class PacketInVersionInfo extends Packet {

  private String intaveVersion;
  private int protocolVersion;

  public PacketInVersionInfo() {
  }

  public PacketInVersionInfo(String intaveVersion, int protocolVersion) {
    this.intaveVersion = intaveVersion;
    this.protocolVersion = protocolVersion;
  }

  @Override
  public void applyFrom(ByteArrayDataInput input) throws IllegalStateException, AssertionError {
    Preconditions.checkNotNull(input);

    intaveVersion = input.readUTF();
    protocolVersion = input.readInt();
  }

  @Override
  public void applyTo(ByteArrayDataOutput output) {
    Preconditions.checkNotNull(output);

    output.writeUTF(intaveVersion);
    output.writeInt(protocolVersion);
  }
}
