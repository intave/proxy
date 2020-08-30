package de.jpx3.ips.connect.bukkit.packets;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import de.jpx3.ips.connect.bukkit.AbstractPacket;

import java.util.Arrays;
import java.util.UUID;

public final class PacketInPunishmentRequest extends AbstractPacket {

  private UUID id;
  private PunishmentType punishmentType;
  private String message;
  private long endTimestamp;

  public PacketInPunishmentRequest() {
  }

  public PacketInPunishmentRequest(
    UUID id, PunishmentType punishmentType,
    String message, long endTimestamp
  ) {
    this.id = id;
    this.punishmentType = punishmentType;
    this.message = message;
    this.endTimestamp = endTimestamp;
  }

  @Override
  public void applyFrom(ByteArrayDataInput input)
    throws IllegalStateException, AssertionError {
    Preconditions.checkNotNull(input);

    id = UUID.fromString(input.readUTF());
    punishmentType = PunishmentType.fromId(input.readInt());
    message = input.readUTF();
    endTimestamp = input.readLong();
  }

  @Override
  public void applyTo(ByteArrayDataOutput output) {
    Preconditions.checkNotNull(output);

    output.writeUTF(id.toString());
    output.writeInt(punishmentType.typeId());
    output.writeUTF(message);
    output.writeLong(endTimestamp);
  }

  public UUID id() {
    return id;
  }

  public PunishmentType punishmentType() {
    return punishmentType;
  }

  public long endTimestamp() {
    return endTimestamp;
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
