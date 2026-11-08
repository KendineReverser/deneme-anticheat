package ac.deneme.anticheat.manager.init.start;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.platform.api.Platform;
import ac.deneme.anticheat.utils.anticheat.LogUtil;

public class TickRunner implements StartableInitable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (DenemeAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(DenemeAPI.INSTANCE.getDenemePlugin(), () -> {
                DenemeAPI.INSTANCE.getTickManager().tickSync();
                DenemeAPI.INSTANCE.getTickManager().tickAsync();
            }, 1, 1);
        } else {
            DenemeAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runAtFixedRate(DenemeAPI.INSTANCE.getDenemePlugin(), () -> DenemeAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
            DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(DenemeAPI.INSTANCE.getDenemePlugin(), () -> DenemeAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
        }
    }
}
