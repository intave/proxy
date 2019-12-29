package de.jpx3.ips.connect.bukkit.protocol.packets;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import de.jpx3.ips.connect.bukkit.protocol.Packet;

import java.util.Arrays;
import java.util.UUID;

public final class PacketInPunishmentRequest extends Packet {

  private UUID playerId;
  private PunishmentType punishmentType;
  private String message;
  private long tempbanEndTimestamp;

  public PacketInPunishmentRequest() {
  }

  public PacketInPunishmentRequest(UUID playerId, PunishmentType punishmentType, String message, long tempbanEndTimestamp) {
    this.playerId = playerId;
    this.punishmentType = punishmentType;
    this.message = message;
    this.tempbanEndTimestamp = tempbanEndTimestamp;
  }

  @Override
  public void applyFrom(ByteArrayDataInput input) throws IllegalStateException, AssertionError {
    Preconditions.checkNotNull(input);

    playerId = UUID.fromString(input.readUTF());
    punishmentType = PunishmentType.fromId(input.readInt());
    message = input.readUTF();
    tempbanEndTimestamp = input.readLong();
  }

  @Override
  public void applyTo(ByteArrayDataOutput output) {
    Preconditions.checkNotNull(output);

    output.writeUTF(playerId.toString());
    output.writeInt(punishmentType.typeId());
    output.writeUTF(message);
    output.writeLong(tempbanEndTimestamp);
  }

  public UUID playerId() {
    return playerId;
  }

  public PunishmentType punishmentType() {
    return punishmentType;
  }

  public long endTimestamp() {
    return tempbanEndTimestamp;
  }

  public String message() {
    return message;
  }

  public enum PunishmentType {
    KICK(1),
    TEMP_BAN(2),
    BAN(3);

    private int typeId;

    PunishmentType(int typeId) {
      this.typeId = typeId;
    }

    public static PunishmentType fromId(int id) {
      return Arrays.stream(PunishmentType.values())
        .filter(value -> value.typeId() == id)
        .findFirst()
        .orElse(null);
    }

    public int typeId() {
      return typeId;
    }
  }
}
