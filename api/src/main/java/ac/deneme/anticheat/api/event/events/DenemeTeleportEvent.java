package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Deneme sends a teleport packet to the client.
 *
 * <p>Exists to help maintain compatibility with packet-based plugins and other
 * anticheats that track inbound/outbound teleport packets to build a
 * pending-teleport deque.
 *
 * <p>Fires on the Netty thread associated with the PacketEvents user.
 * Observational, not cancellable.
 */
public final class DenemeTeleportEvent extends DenemeEvent<DenemeTeleportEvent.Channel> {
    private DenemeTeleportEvent() {

    }

    @FunctionalInterface
    public interface Handler {
        void onTeleport(@NotNull DenemeUser user, int teleportId, long timestamp);
    }

    public static final class Channel extends EventChannel<DenemeTeleportEvent, Handler> {
        public Channel() {
            super(DenemeTeleportEvent.class, Handler.class);
        }

        public void onTeleport(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onTeleport(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onTeleport(@NotNull Object pluginContext, @NotNull Handler handler) {
            onTeleport(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onTeleport(Object, Handler)}. */
        @Deprecated
        public void onTeleport(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onTeleport(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user, int teleportId, long timestamp) {
            Entry<Handler>[] entries = entries();
            for (Entry<Handler> e : entries) {
                try {
                    e.handler.onTeleport(user, teleportId, timestamp);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeTeleportEvent event, @NotNull Handler handler, boolean cancelled) {

            throw new UnsupportedOperationException("DenemeTeleportEvent has no legacy representation");
        }

        @org.jetbrains.annotations.ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return (user, id, ts) -> abstractHandler.onAnyEvent(DenemeTeleportEvent.class, false);
        }
    }
}
