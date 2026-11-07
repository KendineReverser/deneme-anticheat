package ac.deneme.anticheat;

import ac.deneme.anticheat.api.DenemeAbstractAPI;
import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.alerts.AlertManager;
import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.event.EventBus;
import ac.deneme.anticheat.api.event.events.DenemeReloadEvent;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.api.storage.backend.BackendRegistry;
import ac.deneme.anticheat.manager.config.ConfigManagerFileImpl;
import ac.deneme.anticheat.manager.init.start.StartableInitable;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.LogUtil;
import ac.deneme.anticheat.utils.anticheat.MessageUtil;
import ac.deneme.anticheat.utils.common.ConfigReloadObserver;
import ac.deneme.anticheat.utils.common.PropertiesUtil;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// This is used for deneme's external API. It has its own class just for organization.
public class DenemeExternalAPI implements DenemeAbstractAPI, ConfigReloadObserver, StartableInitable {

    // Holder class — DenemeExternalAPI is constructed inside DenemeAPI's ctor,
    // so a plain static-final would see a null DenemeAPI.INSTANCE. Holder
    // init runs on first fire, after DenemeAPI is fully built.
    private static final class Channels {
        static final DenemeReloadEvent.Channel RELOAD = DenemeAPI.INSTANCE.getEventBus().get(DenemeReloadEvent.class);
    }

    private final DenemeAPI api;
    @Getter
    private final Map<String, Function<DenemeUser, String>> variableReplacements = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, String> staticReplacements = new ConcurrentHashMap<>();
    private final Map<String, Function<Object, Object>> functions = new ConcurrentHashMap<>();
    private final ConfigManagerFileImpl configManagerFile = new ConfigManagerFileImpl();
    private final String denemeVersion;
    private ConfigManager configManager = null;
    private boolean started = false;

    public DenemeExternalAPI(DenemeAPI api) {
        this.api = api;
        this.denemeVersion = resolveDenemeVersion(api);
    }

    @Override
    public @NotNull EventBus getEventBus() {
        return api.getEventBus();
    }

    @Deprecated
    @Override
    public @Nullable DenemeUser getDenemeUser(Player player) {
        return getDenemeUser(player.getUniqueId());
    }

    @Override
    public @Nullable DenemeUser getDenemeUser(UUID uuid) {
        return api.getPlayerDataManager().getPlayer(uuid);
    }

    @Override
    public void registerVariable(String string, Function<DenemeUser, String> replacement) {
        if (replacement == null) {
            variableReplacements.remove(string);
        } else {
            variableReplacements.put(string, replacement);
        }
    }

    @Override
    public void registerVariable(String variable, String replacement) {
        if (replacement == null) {
            staticReplacements.remove(variable);
        } else {
            staticReplacements.put(variable, replacement);
        }
    }

    @Override
    public String getDenemeVersion() {
        return denemeVersion;
    }

    private static String resolveDenemeVersion(DenemeAPI api) {
        try {
            Properties properties = PropertiesUtil.readProperties(DenemeExternalAPI.class, "denemeanticheat.properties");
            String buildVersion = properties.getProperty("build.version");
            if (buildVersion != null && !buildVersion.isBlank() && !buildVersion.startsWith("${")) {
                return buildVersion;
            }
        } catch (RuntimeException ignored) {
        }

        try {
            return api.getDenemePlugin().getDescription().getVersion();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    @Override
    public void registerFunction(String key, Function<Object, Object> function) {
        if (function == null) {
            functions.remove(key);
        } else {
            functions.put(key, function);
        }
    }

    @Override
    public Function<Object, Object> getFunction(String key) {
        return functions.get(key);
    }

    @Override
    public AlertManager getAlertManager() {
        return DenemeAPI.INSTANCE.getAlertManager();
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public int getCurrentTick() {
        return DenemeAPI.INSTANCE.getTickManager().currentTick;
    }

    @Override
    public @NotNull DenemePlugin getDenemePlugin(@NotNull Object o) {
        return this.api.getExtensionManager().getPlugin(o);
    }

    @Override
    public @NotNull BackendRegistry getBackendRegistry() {
        return api.getBackendRegistry();
    }

    // on load, load the config & register the service
    public void load() {
        reload(configManagerFile);
        api.getLoader().registerAPIService();
    }

    // handles any config loading that's needed to be done after load
    @Override
    public void start() {
        started = true;
        try {
            DenemeAPI.INSTANCE.getConfigManager().start();
        } catch (Exception e) {
            LogUtil.error("Failed to start config manager.", e);
        }
    }

    @Override
    public void reload(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(DenemeAPI.INSTANCE.getDenemePlugin(),
                    () -> successfulReload(config));
        } else {
            successfulReload(config);
        }
    }

    @Override
    public CompletableFuture<Boolean> reloadAsync(ConfigManager config) {
        if (config.isLoadedAsync() && started) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(DenemeAPI.INSTANCE.getDenemePlugin(),
                    () -> future.complete(successfulReload(config)));
            return future;
        }
        return CompletableFuture.completedFuture(successfulReload(config));
    }

    private boolean successfulReload(ConfigManager config) {
        try {
            config.reload();
            DenemeAPI.INSTANCE.getConfigManager().load(config);
            if (started) DenemeAPI.INSTANCE.getConfigManager().start();
            onReload(config);
            if (started)
                DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(DenemeAPI.INSTANCE.getDenemePlugin(),
                        () -> Channels.RELOAD.fire(true));
            return true;
        } catch (Exception e) {
            LogUtil.error("Failed to reload config", e);
        }
        if (started)
            DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(DenemeAPI.INSTANCE.getDenemePlugin(),
                    () -> Channels.RELOAD.fire(false));
        return false;
    }

    @Override
    public void onReload(ConfigManager newConfig) {
        if (newConfig == null) {
            LogUtil.warn("ConfigManager not set. Using default config file manager.");
            configManager = configManagerFile;
        } else {
            configManager = newConfig;
        }
        // Update variables
        updateVariables();
        // Restart
        DenemeAPI.INSTANCE.getAlertManager().reload(configManager);
        DenemeAPI.INSTANCE.getDiscordManager().reload();
        DenemeAPI.INSTANCE.getSpectateManager().reload();
        // First-load guard: load() calls reload() before start() runs, so this fires once with started=false before the datastore exists. Subsequent /deneme reload calls see started=true and proceed (including disabled→enabled flips — DataStoreLifecycle.reload() re-evaluates builder.enabled() each time).
        if (!started) return;
        // Hot-reload picks up backend swaps + routing + connection-pool edits without a server restart. Drains in-flight writes for shutdown-drain-timeout-ms then drops; brief mid-reload unavailability is the tradeoff.
        if (DenemeAPI.INSTANCE.getDataStoreLifecycle() != null) {
            DenemeAPI.INSTANCE.getDataStoreLifecycle().reload();
        }
        // Reload checks for all players
        for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.runSafely(() -> player.reload(configManager));
        }
    }

    private void updateVariables() {
        variableReplacements.putIfAbsent("%player%", DenemeUser::getName);
        variableReplacements.putIfAbsent("%uuid%", user -> user.getUniqueId().toString());
        variableReplacements.putIfAbsent("%ping%", user -> user.getTransactionPing() + "");
        variableReplacements.putIfAbsent("%brand%", DenemeUser::getBrand);
        variableReplacements.putIfAbsent("%h_sensitivity%", user -> ((int) Math.round(user.getHorizontalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%v_sensitivity%", user -> ((int) Math.round(user.getVerticalSensitivity() * 200)) + "");
        variableReplacements.putIfAbsent("%fast_math%", user -> !user.isVanillaMath() + "");
        variableReplacements.putIfAbsent("%tps%", user -> String.format("%.2f", DenemeAPI.INSTANCE.getPlatformServer().getTPS()));
        variableReplacements.putIfAbsent("%version%", DenemeUser::getVersionName);
        // static variables
        staticReplacements.put("%prefix%", MessageUtil.translateAlternateColorCodes('&', DenemeAPI.INSTANCE.getConfigManager().getPrefix()));
        staticReplacements.putIfAbsent("%deneme_version%", getDenemeVersion());
    }
}
