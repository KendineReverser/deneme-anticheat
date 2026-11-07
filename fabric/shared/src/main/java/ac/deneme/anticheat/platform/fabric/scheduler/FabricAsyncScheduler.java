package ac.deneme.anticheat.platform.fabric.scheduler;

import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.platform.api.scheduler.AsyncScheduler;
import ac.deneme.anticheat.platform.api.scheduler.PlatformScheduler;
import ac.deneme.anticheat.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FabricAsyncScheduler implements AsyncScheduler {

    private final ScheduledExecutorService executor;

    private final Map<Object, Tracked> tasks = new ConcurrentHashMap<>();

    private static final class Tracked {
        private final DenemePlugin plugin;
        private volatile Future<?> future;

        private Tracked(DenemePlugin plugin) {
            this.plugin = plugin;
        }
    }

    public FabricAsyncScheduler() {
        AtomicInteger threadCount = new AtomicInteger();
        this.executor = Executors.newScheduledThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() / 2),
                runnable -> {
                    Thread thread = new Thread(runnable, "Deneme-Async-" + threadCount.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @Override
    public TaskHandle runNow(@NotNull DenemePlugin plugin, @NotNull Runnable task) {
        return schedule(plugin, task, true, executor::submit);
    }

    @Override
    public TaskHandle runDelayed(@NotNull DenemePlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return schedule(plugin, task, true, body -> executor.schedule(body, delay, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull DenemePlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return schedule(plugin, task, false, body -> executor.scheduleAtFixedRate(body, delay, period, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull DenemePlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task,
                PlatformScheduler.convertTicksToTime(initialDelayTicks, TimeUnit.MILLISECONDS),
                PlatformScheduler.convertTicksToTime(periodTicks, TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS);
    }

    private TaskHandle schedule(DenemePlugin plugin, Runnable task, boolean oneShot, Function<Runnable, Future<?>> submit) {
        Object token = new Object();
        Tracked tracked = new Tracked(plugin);
        tasks.put(token, tracked);

        Runnable body = oneShot ? () -> {
            try {
                task.run();
            } finally {
                tasks.remove(token);
            }
        } : task;

        tracked.future = submit.apply(body);

        return new FabricTaskHandle(() -> {
            Tracked removed = tasks.remove(token);
            if (removed != null && removed.future != null) {
                removed.future.cancel(true);
            }
        }, false);
    }

    @Override
    public void cancel(@NotNull DenemePlugin plugin) {
        tasks.values().removeIf(tracked -> {
            if (tracked.plugin.equals(plugin)) {
                Future<?> future = tracked.future;
                if (future != null) {
                    future.cancel(true);
                }
                return true;
            }
            return false;
        });
    }

    public void cancelAll() {
        tasks.values().forEach(tracked -> {
            Future<?> future = tracked.future;
            if (future != null) {
                future.cancel(true);
            }
        });
        tasks.clear();
    }
}
