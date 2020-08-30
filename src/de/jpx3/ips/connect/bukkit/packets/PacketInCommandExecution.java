package de.jpx3.ips.connect.bukkit.packets;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import de.jpx3.ips.connect.bukkit.AbstractPacket;

import java.util.UUID;

public final class PacketInCommandExecution extends AbstractPacket {
  private UUID id;
  private String command;

  public PacketInCommandExecution() {
  }

  public PacketInCommandExecution(
    UUID id, String command
  ) {
    this.id = id;
    this.command = command;
  }

  @Override
  public void applyFrom(ByteArrayDataInput input)
    throws IllegalStateException, AssertionError {
    Preconditions.checkNotNull(input);

    id = UUID.fromString(input.readUTF());
    command = input.readUTF();
  }

  @Override
  public void applyTo(ByteArrayDataOutput output) {
    Preconditions.checkNotNull(output);

    output.writeUTF(id.toString());
    output.writeUTF(command);
  }

  public UUID id() {
    return id;
  }

  public String command() {
    return command;
  }
}
