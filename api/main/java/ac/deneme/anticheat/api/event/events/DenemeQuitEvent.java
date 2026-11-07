package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class DenemeQuitEvent extends DenemeEvent<DenemeQuitEvent.Channel> implements DenemeUserEvent {
    private DenemeUser user;

    /** Pool constructor — fields populated via {@link #init}. */
    public DenemeQuitEvent() {
        super(true); // Async
    }

    public DenemeQuitEvent(DenemeUser user) {
        super(true); // Async
        this.user = user;
    }

    @ApiStatus.Internal
    public void init(DenemeUser user) {
        resetForReuse();
        this.user = user;
    }

    @Override
    public DenemeUser getUser() {
        return user;
    }

    @FunctionalInterface
    public interface Handler {
        void onQuit(@NotNull DenemeUser user);
    }

    public static final class Channel extends EventChannel<DenemeQuitEvent, Handler> {
        private final ThreadLocal<DenemeQuitEvent> legacyPool = ThreadLocal.withInitial(DenemeQuitEvent::new);

        public Channel() {
            super(DenemeQuitEvent.class, Handler.class);
        }

        public void onQuit(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onQuit(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onQuit(@NotNull Object pluginContext, @NotNull Handler handler) {
            onQuit(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onQuit(Object, Handler)}. */
        @Deprecated
        public void onQuit(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onQuit(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user) {
            Entry<Handler>[] entries = entries();
            if (entries.length == 0) return;
            if (!hasLegacy()) {
                for (Entry<Handler> e : entries) {
                    try {
                        e.handler.onQuit(user);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                return;
            }
            DenemeQuitEvent pooled = legacyPool.get();
            pooled.init(user);
            for (Entry<Handler> e : entries) {
                try {
                    if (e.legacyListener != null) {
                        e.<DenemeQuitEvent>legacyListenerAs().handle(pooled);
                    } else {
                        e.handler.onQuit(user);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeQuitEvent event, @NotNull Handler handler, boolean cancelled) {
            handler.onQuit(event.getUser());
            return false;
        }

        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return user -> abstractHandler.onAnyEvent(DenemeQuitEvent.class, false);
        }
    }
}
