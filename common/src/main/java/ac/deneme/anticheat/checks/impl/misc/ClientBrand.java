package ac.deneme.anticheat.checks.impl.misc;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import lombok.Getter;
import net.kyori.adventure.text.Component;

public class ClientBrand extends DenemeProcessor implements PacketReceiveListener {

    private static final String CHANNEL = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) ? "minecraft:brand" : "MC|Brand";

    @Getter
    private String brand = "vanilla";
    @Getter
    private boolean hasBrand = false;

    public ClientBrand(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            handle(packet.getChannelName(), packet.getData());
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            handle(packet.getChannelName(), packet.getData());
        }
    }

    private void handle(String channel, byte[] data) {
        if (!channel.equals(ClientBrand.CHANNEL)) return;

        if (data.length > 64 || data.length == 0) {
            brand = "sent " + data.length + " bytes as brand";
        } else if (!hasBrand) {
            byte[] minusLength = new byte[data.length - 1];
            System.arraycopy(data, 1, minusLength, 0, minusLength.length);

            brand = new String(minusLength).replace(" (Velocity)", "");
            brand = MessageUtil.stripColor(brand);
            if (!DenemeAPI.INSTANCE.getConfigManager().isIgnoredClient(brand)) {
                String message = DenemeAPI.INSTANCE.getConfigManager().getConfig().getStringElse("client-brand-format", "%prefix% &f%player% joined using %brand%");
                Component component = MessageUtil.replacePlaceholders(player, MessageUtil.miniMessage(message));

                DenemeAPI.INSTANCE.getAlertManager().sendBrand(component, null);
            }

            DenemeAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks().observeBrandFromCheck(player);
        }

        final boolean hasReachHacks = brand.contains("forge")
                && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2)
                && player.getClientVersion().isOlderThan(ClientVersion.V_1_19_4);
        if (hasReachHacks && DenemeAPI.INSTANCE.getConfigManager().isBlockBlacklistedForgeClients()) {
            player.disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(player, DenemeAPI.INSTANCE.getConfigManager().getDisconnectBlacklistedForge())));
        }

        hasBrand = true;
    }
}
