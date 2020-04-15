package de.jpx3.ips.connect.bukkit.exceptions;

public final class ProtocolVersionMismatchException extends RuntimeException {
  public ProtocolVersionMismatchException() {
  }

  public ProtocolVersionMismatchException(String s) {
    super(s);
  }

  public ProtocolVersionMismatchException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProtocolVersionMismatchException(Throwable cause) {
    super(cause);
  }
}
