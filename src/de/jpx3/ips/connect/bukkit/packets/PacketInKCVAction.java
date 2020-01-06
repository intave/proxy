package de.jpx3.ips.connect.bukkit.packets;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import de.jpx3.ips.connect.bukkit.AbstractPacket;

import java.util.UUID;

public final class PacketInKCVAction extends AbstractPacket {
  private UUID kickedPlayerId;
  private String kickingCheckName;
  private String kickingCheckCategory;
  private String finalFlagMessage;
  private int finalTotalViolationLevel;

  public PacketInKCVAction() {
  }

  public PacketInKCVAction(UUID playerId, String checkName, String checkCategory, String finalFlagMessage, int finalTotalViolationLevel) {
    this.kickedPlayerId = playerId;
    this.kickingCheckName = checkName;
    this.kickingCheckCategory = checkCategory;
    this.finalFlagMessage = finalFlagMessage;
    this.finalTotalViolationLevel = finalTotalViolationLevel;
  }

  @Override
  public void applyFrom(ByteArrayDataInput input) throws IllegalStateException, AssertionError {
    Preconditions.checkNotNull(input);

    kickedPlayerId = UUID.fromString(input.readUTF());
    kickingCheckName = input.readUTF();
    kickingCheckCategory = input.readUTF();
    finalFlagMessage = input.readUTF();
    finalTotalViolationLevel = input.readInt();
  }

  @Override
  public void applyTo(ByteArrayDataOutput output) {
    Preconditions.checkNotNull(output);

    output.writeUTF(kickedPlayerId.toString());
    output.writeUTF(kickingCheckName);
    output.writeUTF(kickingCheckCategory);
    output.writeUTF(finalFlagMessage);
    output.writeInt(finalTotalViolationLevel);
  }

  public UUID kickedPlayerId() {
    return kickedPlayerId;
  }

  public String kickingCheckName() {
    return kickingCheckName;
  }

  public String kickingCheckCategory() {
    return kickingCheckCategory;
  }

  public String finalFlagMessage() {
    return finalFlagMessage;
  }

  public int finalTotalViolationLevel() {
    return finalTotalViolationLevel;
  }
}
