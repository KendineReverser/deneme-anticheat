package ac.deneme.anticheat.checks.impl.crash;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.inventory.inventory.MenuType;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

@CheckData(name = "CrashD", stableKey = "deneme.crash.lectern", description = "Clicking slots in lectern window")
public class CrashD extends Check implements PacketReceiveListener, PacketSendListener {
    private static final Verbose V = Verbose.of("clickType={clicktype}, button={sint}");

    private MenuType type = MenuType.UNKNOWN;
    private int lecternId = -1;

    public CrashD(DenemePlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);
            this.type = MenuType.getMenuType(window.getType());
            if (type == MenuType.LECTERN) lecternId = window.getContainerId();
        }
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            int clickType = VerboseCodecs.enumId(click.getWindowClickType());
            int button = click.getButton();
            int windowId = click.getWindowId();

            if (type == MenuType.LECTERN && windowId > 0 && windowId == lecternId) {
                if (flag(V.write(verbose()).uint(clickType).sint(button))) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
