package de.jpx3.ips.connect.bukkit.exceptions;

public final class InvalidPacketException extends RuntimeException {
  public InvalidPacketException() {
  }

  public InvalidPacketException(String s) {
    super(s);
  }

  public InvalidPacketException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidPacketException(Throwable cause) {
    super(cause);
  }
}
