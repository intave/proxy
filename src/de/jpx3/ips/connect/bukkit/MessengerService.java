package de.jpx3.ips.connect.bukkit;

import com.google.common.base.Preconditions;
import de.jpx3.ips.IntaveProxySupportPlugin;
import net.md_5.bungee.config.Configuration;

public final class MessengerService {
  public final static int PROTOCOL_VERSION = 2;
  public final static String INCOMING_CHANNEL = "ipc-s2p";
  public final static String OUTGOING_CHANNEL = "ipc-p2s";
  public final static String PROTOCOL_HEADER = "IPC_BEGIN";
  public final static String PROTOCOL_FOOTER = "IPC_END";

  private IntaveProxySupportPlugin plugin;
  private final boolean enabled;
  private volatile boolean channelOpen = false;

  private PacketSender packetSender;
  private PacketReceiver packetReceiver;
  private PacketSubscriptionService packetSubscriptionService;

  private MessengerService(IntaveProxySupportPlugin plugin, Configuration configuration) {
    this.plugin = plugin;
    this.enabled = configuration.getBoolean("enabled");
  }

  public void setup() {
    packetSubscriptionService = PacketSubscriptionService.createFrom(plugin);
    packetReceiver = PacketReceiver.createFrom(plugin,this);
    packetSender = PacketSender.createFrom(plugin, this);

    if(enabled()) {
      openChannel();
    }
  }

  public void openChannel() {
    if(channelOpen() || !enabled()) {
      throw new IllegalStateException();
    }

    packetSender.setup();
    packetReceiver.setup();
    packetSubscriptionService.setup();
    channelOpen = true;
  }

  public void closeChannel() {
    if(!channelOpen()) {
      throw new IllegalStateException();
    }

    packetSender.reset();
    packetReceiver.unset();
    packetSubscriptionService.reset();
    channelOpen = false;
  }

  public boolean channelOpen() {
    return channelOpen;
  }

  public boolean enabled() {
    return enabled;
  }

  public PacketSender packetSender() {
    return packetSender;
  }

  public PacketReceiver packetReceiver() {
    return packetReceiver;
  }

  public PacketSubscriptionService packetSubscriptionService() {
    return packetSubscriptionService;
  }

  public static MessengerService createFrom(IntaveProxySupportPlugin proxySupportPlugin,
                                            Configuration configuration
  ) {
    Preconditions.checkNotNull(proxySupportPlugin);
    Preconditions.checkNotNull(configuration);
    return new MessengerService(proxySupportPlugin, configuration);
  }
}
