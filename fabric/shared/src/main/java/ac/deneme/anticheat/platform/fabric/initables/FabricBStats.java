package ac.deneme.anticheat.platform.fabric.initables;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.init.start.StartableInitable;
import ac.deneme.anticheat.manager.init.stop.StoppableInitable;
import ac.deneme.anticheat.platform.fabric.utils.metrics.MetricsFabric;
import ac.deneme.anticheat.utils.anticheat.Constants;

public class FabricBStats implements StartableInitable, StoppableInitable {

    private MetricsFabric metricsFabric;

    @Override
    public void start() {
        try {
            metricsFabric = new MetricsFabric(DenemeAPI.INSTANCE.getDenemePlugin(), Constants.BSTATS_PLUGIN_ID);
        } catch (Exception ignored) {}
    }

    @Override
    public void stop() {
        if (metricsFabric != null)
            metricsFabric.shutdown();
    }
}
