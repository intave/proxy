package de.jpx3.ips.connect.bukkit.protocol;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public abstract class Packet {
  public abstract void applyFrom(ByteArrayDataInput input) throws IllegalStateException;

  public abstract void applyTo(ByteArrayDataOutput output);
}
