package ac.deneme.anticheat;

import ac.deneme.anticheat.api.event.EventBus;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.api.storage.backend.BackendRegistry;
import ac.deneme.anticheat.internal.plugin.resolver.DenemeExtensionManager;
import ac.deneme.anticheat.internal.event.OptimizedEventBus;
import ac.deneme.anticheat.internal.storage.backend.BackendRegistryImpl;
import ac.deneme.anticheat.internal.storage.backend.memory.InMemoryBackendProvider;
import ac.deneme.anticheat.internal.storage.backend.mongo.MongoBackendProvider;
import ac.deneme.anticheat.internal.storage.backend.mysql.MysqlBackendProvider;
import ac.deneme.anticheat.internal.storage.backend.postgres.PostgresBackendProvider;
import ac.deneme.anticheat.internal.storage.backend.redis.RedisBackendProvider;
import ac.deneme.anticheat.internal.storage.backend.sqlite.SqliteBackendProvider;
import ac.deneme.anticheat.manager.AlertManagerImpl;
import ac.deneme.anticheat.manager.DiscordManager;
import ac.deneme.anticheat.manager.InitManager;
import ac.deneme.anticheat.manager.SpectateManager;
import ac.deneme.anticheat.manager.TickManager;
import ac.deneme.anticheat.manager.config.BaseConfigManager;
import ac.deneme.anticheat.manager.datastore.DataStoreLifecycle;
import ac.deneme.anticheat.manager.init.Initable;
import ac.deneme.anticheat.platform.api.Platform;
import ac.deneme.anticheat.platform.api.PlatformLoader;
import ac.deneme.anticheat.platform.api.PlatformServer;
import ac.deneme.anticheat.platform.api.command.CommandService;
import ac.deneme.anticheat.platform.api.manager.ItemResetHandler;
import ac.deneme.anticheat.platform.api.manager.MessagePlaceHolderManager;
import ac.deneme.anticheat.platform.api.manager.PermissionRegistrationManager;
import ac.deneme.anticheat.platform.api.manager.PlatformPluginManager;
import ac.deneme.anticheat.platform.api.player.PlatformPlayerFactory;
import ac.deneme.anticheat.platform.api.scheduler.PlatformScheduler;
import ac.deneme.anticheat.platform.api.sender.SenderFactory;
import ac.deneme.anticheat.utils.anticheat.PlayerDataManager;
import ac.deneme.anticheat.utils.common.arguments.CommonDenemeArguments;
import ac.deneme.anticheat.utils.reflection.ReflectionUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class DenemeAPI {
    public static final DenemeAPI INSTANCE = new DenemeAPI();

    private final Platform platform = detectPlatform();
    private final BaseConfigManager configManager;
    private final AlertManagerImpl alertManager;
    private final SpectateManager spectateManager;
    private final DiscordManager discordManager;
    private final PlayerDataManager playerDataManager;
    private final TickManager tickManager;
    private final DenemeExtensionManager extensionManager;
    private final EventBus eventBus;
    private final DenemeExternalAPI externalAPI;
    private DataStoreLifecycle dataStoreLifecycle;
    private final BackendRegistry backendRegistry = buildBackendRegistry();
    private PlatformLoader loader;
    private InitManager initManager;
    private boolean initialized = false;

    private DenemeAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.extensionManager = new DenemeExtensionManager();
        this.eventBus = new OptimizedEventBus(extensionManager);
        this.externalAPI = new DenemeExternalAPI(this);
    }

    // the order matters
    private static Platform detectPlatform() {
        Platform override = CommonDenemeArguments.PLATFORM_OVERRIDE.value();
        if (override != null) return override;
        if (ReflectionUtils.hasClass("io.papermc.paper.threadedregions.RegionizedServer")) return Platform.FOLIA;
        if (ReflectionUtils.hasClass("org.bukkit.Bukkit")) return Platform.BUKKIT;
        if (ReflectionUtils.hasClass("net.fabricmc.loader.api.FabricLoader")) return Platform.FABRIC;
        throw new IllegalStateException("Unknown platform!");
    }

    public void load(PlatformLoader platformLoader, Initable... platformSpecificInitables) {
        this.loader = platformLoader;
        this.dataStoreLifecycle = new DataStoreLifecycle(getDenemePlugin(), backendRegistry);
        this.initManager = new InitManager(loader.getPacketEvents(), platformSpecificInitables);
        this.initManager.load();
        this.initialized = true;
    }

    private static BackendRegistry buildBackendRegistry() {
        BackendRegistryImpl registry = new BackendRegistryImpl();
        registry.register(new SqliteBackendProvider());
        registry.register(new InMemoryBackendProvider());
        registry.register(new MysqlBackendProvider());
        registry.register(new PostgresBackendProvider());
        registry.register(new MongoBackendProvider());
        registry.register(new RedisBackendProvider());
        return registry;
    }

    public void start() {
        checkInitialized();
        initManager.start();
    }

    public void stop() {
        checkInitialized();
        initManager.stop();
    }

    public PlatformScheduler getScheduler() {
        return loader.getScheduler();
    }

    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return loader.getPlatformPlayerFactory();
    }

    public DenemePlugin getDenemePlugin() {
        return loader.getPlugin();
    }

    public SenderFactory<?> getSenderFactory() {
        return loader.getSenderFactory();
    }

    public ItemResetHandler getItemResetHandler() {
        return loader.getItemResetHandler();
    }

    public PlatformPluginManager getPluginManager() {
        return loader.getPluginManager();
    }

    public PlatformServer getPlatformServer() {
        return loader.getPlatformServer();
    }

    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return loader.getMessagePlaceHolderManager();
    }

    public CommandService getCommandService() {
        return loader.getCommandService();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("DenemeAPI has not been initialized!");
        }
    }

    public PermissionRegistrationManager getPermissionManager() {
        return loader.getPermissionManager();
    }
}
