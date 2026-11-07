package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class DenemeReloadEvent extends DenemeEvent<DenemeReloadEvent.Channel> {
    private boolean success;

    /** Pool constructor — fields populated via {@link #init}. */
    public DenemeReloadEvent() {
        super(true);
    }

    public DenemeReloadEvent(boolean success) {
        super(true);
        this.success = success;
    }

    @ApiStatus.Internal
    public void init(boolean success) {
        resetForReuse();
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    @FunctionalInterface
    public interface Handler {
        void onReload(boolean success);
    }

    public static final class Channel extends EventChannel<DenemeReloadEvent, Handler> {
        private final ThreadLocal<DenemeReloadEvent> legacyPool = ThreadLocal.withInitial(DenemeReloadEvent::new);

        public Channel() {
            super(DenemeReloadEvent.class, Handler.class);
        }

        public void onReload(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onReload(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onReload(@NotNull Object pluginContext, @NotNull Handler handler) {
            onReload(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onReload(Object, Handler)}. */
        @Deprecated
        public void onReload(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onReload(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(boolean success) {
            Entry<Handler>[] entries = entries();
            if (entries.length == 0) return;
            if (!hasLegacy()) {
                for (Entry<Handler> e : entries) {
                    try {
                        e.handler.onReload(success);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                return;
            }
            DenemeReloadEvent pooled = legacyPool.get();
            pooled.init(success);
            for (Entry<Handler> e : entries) {
                try {
                    if (e.legacyListener != null) {
                        e.<DenemeReloadEvent>legacyListenerAs().handle(pooled);
                    } else {
                        e.handler.onReload(success);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeReloadEvent event, @NotNull Handler handler, boolean cancelled) {
            handler.onReload(event.isSuccess());
            return false;
        }

        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return success -> abstractHandler.onAnyEvent(DenemeReloadEvent.class, false);
        }
    }
}
