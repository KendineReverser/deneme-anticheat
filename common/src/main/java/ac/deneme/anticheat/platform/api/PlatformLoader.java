package ac.deneme.anticheat.platform.api;

import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.platform.api.command.CommandService;
import ac.deneme.anticheat.platform.api.manager.ItemResetHandler;
import ac.deneme.anticheat.platform.api.manager.MessagePlaceHolderManager;
import ac.deneme.anticheat.platform.api.manager.PermissionRegistrationManager;
import ac.deneme.anticheat.platform.api.manager.PlatformPluginManager;
import ac.deneme.anticheat.platform.api.player.PlatformPlayerFactory;
import ac.deneme.anticheat.platform.api.scheduler.PlatformScheduler;
import ac.deneme.anticheat.platform.api.sender.SenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.jetbrains.annotations.NotNull;

public interface PlatformLoader {
    PlatformScheduler getScheduler();

    PlatformPlayerFactory getPlatformPlayerFactory();

    PacketEventsAPI<?> getPacketEvents();

    ItemResetHandler getItemResetHandler();

    CommandService getCommandService();

    SenderFactory<?> getSenderFactory();

    DenemePlugin getPlugin();

    PlatformPluginManager getPluginManager();

    PlatformServer getPlatformServer();

    void registerAPIService();

    @NotNull
    MessagePlaceHolderManager getMessagePlaceHolderManager();

    PermissionRegistrationManager getPermissionManager();
}
