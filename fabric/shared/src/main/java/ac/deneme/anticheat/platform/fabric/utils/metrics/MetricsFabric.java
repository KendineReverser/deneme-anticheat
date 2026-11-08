package ac.deneme.anticheat.platform.fabric.utils.metrics;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.platform.fabric.AbstractDenemeACFabricEntryPoint;
import net.fabricmc.loader.api.FabricLoader;

import java.util.logging.Level;

public class MetricsFabric implements Metrics {

    private final MetricsBase metricsBase;

    public MetricsFabric(DenemePlugin plugin, int serviceId) {
        BStatsConfig.Config config = BStatsConfig.loadConfig();

        boolean enabled = config.enabled;
        String serverUUID = config.serverUuid;
        boolean logErrors = config.logFailedRequests;
        boolean logSentData = config.logSentData;
        boolean logResponseStatusText = config.logResponseStatusText;

        metricsBase =
                new MetricsBase(
                        "fabric",
                        serverUUID,
                        serviceId,
                        enabled,
                        this::appendPlatformData,
                        this::appendServiceData,
                        submitDataTask -> DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(plugin, submitDataTask),
                        () -> true,
                        (message, error) -> plugin.getLogger().log(Level.WARNING, message, error),
                        (message) -> plugin.getLogger().log(Level.INFO, message),
                        logErrors,
                        logSentData,
                        logResponseStatusText,
                        false);
    }

    public void shutdown() {
        metricsBase.shutdown();
    }

    public void addCustomChart(CustomChart chart) {
        metricsBase.addCustomChart(chart);
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("playerAmount", getPlayerAmount());
        builder.appendField("onlineMode", AbstractDenemeACFabricEntryPoint.server().usesAuthentication() ? 0 : 1);
        builder.appendField("bukkitVersion", DenemeAPI.INSTANCE.getPlatformServer().getPlatformImplementationString());
        builder.appendField("bukkitName", "Fabric");
        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", FabricLoader.getInstance().getModContainer("denemeanticheat").get().getMetadata().getVersion().getFriendlyString());
    }

    private int getPlayerAmount() {
        if (AbstractDenemeACFabricEntryPoint.server().isRunning()) {
            return AbstractDenemeACFabricEntryPoint.server().getPlayerCount();
        }
        return 0;
    }
}
