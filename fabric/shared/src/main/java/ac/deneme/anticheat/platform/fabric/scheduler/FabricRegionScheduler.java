package ac.deneme.anticheat.platform.fabric.scheduler;

import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.platform.api.scheduler.RegionScheduler;
import ac.deneme.anticheat.platform.api.scheduler.TaskHandle;
import ac.deneme.anticheat.platform.api.world.PlatformWorld;
import ac.deneme.anticheat.platform.fabric.AbstractDenemeACFabricEntryPoint;
import ac.deneme.anticheat.platform.fabric.FabricServerEvents;
import ac.deneme.anticheat.platform.fabric.inject.FabricMinecraftServerHandle;
import ac.deneme.anticheat.utils.math.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FabricRegionScheduler implements RegionScheduler {
    private final Map<FabricPlatformScheduler.ScheduledTask, Runnable> taskMap = new ConcurrentHashMap<>();

    public FabricRegionScheduler() {
        FabricServerEvents.onEndTick(this::handleTasks);
    }

    private void handleTasks(FabricMinecraftServerHandle server) {
        FabricPlatformScheduler.handleSyncTasks(taskMap, server);
    }

    @Override
    public void execute(@NotNull DenemePlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable run) {
        run(plugin, world, chunkX, chunkZ, run);
    }

    @Override
    public void execute(@NotNull DenemePlugin plugin, @NotNull Location location, @NotNull Runnable run) {
        execute(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, run);
    }

    @Override
    public TaskHandle run(@NotNull DenemePlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return runDelayed(plugin, world, chunkX, chunkZ, task, 0);
    }

    @Override
    public TaskHandle run(@NotNull DenemePlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return run(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    @Override
    public TaskHandle runDelayed(@NotNull DenemePlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks) {
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                task,
                AbstractDenemeACFabricEntryPoint.server().getTickCount() + delayTicks,
                0,
                false,
                plugin
        );
        Runnable cancellationTask = () -> taskMap.remove(scheduledTask);
        taskMap.put(scheduledTask, cancellationTask);
        return new FabricTaskHandle(cancellationTask, true);
    }
    @Override
    public TaskHandle runDelayed(@NotNull DenemePlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return runDelayed(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, delayTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull DenemePlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        FabricPlatformScheduler.ScheduledTask scheduledTask = new FabricPlatformScheduler.ScheduledTask(
                task,
                AbstractDenemeACFabricEntryPoint.server().getTickCount() + initialDelayTicks,
                periodTicks,
                true,
                plugin
        );
        Runnable cancellationTask = () -> taskMap.remove(scheduledTask);
        taskMap.put(scheduledTask, cancellationTask);
        return new FabricTaskHandle(cancellationTask, true);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull DenemePlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, initialDelayTicks, periodTicks);
    }

    public void cancel(@NotNull DenemePlugin plugin) {
        FabricPlatformScheduler.cancelPluginTasks(taskMap, plugin);
    }

    public void cancelAll() {
        FabricPlatformScheduler.cancelAllTasks(taskMap);
    }
}
