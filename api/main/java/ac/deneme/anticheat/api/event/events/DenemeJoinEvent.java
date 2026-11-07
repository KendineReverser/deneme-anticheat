package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class DenemeJoinEvent extends DenemeEvent<DenemeJoinEvent.Channel> implements DenemeUserEvent {
    private DenemeUser user;

    /** Pool constructor — fields populated via {@link #init}. */
    public DenemeJoinEvent() {
        super(true); // Async
    }

    public DenemeJoinEvent(DenemeUser user) {
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
        void onJoin(@NotNull DenemeUser user);
    }

    public static final class Channel extends EventChannel<DenemeJoinEvent, Handler> {
        private final ThreadLocal<DenemeJoinEvent> legacyPool = ThreadLocal.withInitial(DenemeJoinEvent::new);

        public Channel() {
            super(DenemeJoinEvent.class, Handler.class);
        }

        public void onJoin(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onJoin(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onJoin(@NotNull Object pluginContext, @NotNull Handler handler) {
            onJoin(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onJoin(Object, Handler)}. */
        @Deprecated
        public void onJoin(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onJoin(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user) {
            Entry<Handler>[] entries = entries();
            if (entries.length == 0) return;
            if (!hasLegacy()) {
                for (Entry<Handler> e : entries) {
                    try {
                        e.handler.onJoin(user);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                return;
            }
            DenemeJoinEvent pooled = legacyPool.get();
            pooled.init(user);
            for (Entry<Handler> e : entries) {
                try {
                    if (e.legacyListener != null) {
                        e.<DenemeJoinEvent>legacyListenerAs().handle(pooled);
                    } else {
                        e.handler.onJoin(user);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeJoinEvent event, @NotNull Handler handler, boolean cancelled) {
            handler.onJoin(event.getUser());
            return false;
        }

        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return user -> abstractHandler.onAnyEvent(DenemeJoinEvent.class, false);
        }
    }
}
