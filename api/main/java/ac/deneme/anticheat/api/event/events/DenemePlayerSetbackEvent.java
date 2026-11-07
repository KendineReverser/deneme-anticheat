package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Deneme sends a player-on-foot setback — the
 * {@code ServerPlayerPositionAndLook} / teleport packet branch of
 * {@link DenemeSetbackEvent}.
 *
 * <p>Carries the outbound teleport id so packet-tracking consumers
 * (e.g. sibling anticheats that need to dedupe the teleport-confirm the
 * client will emit in response) can correlate on id. See
 * {@link DenemeTeleportEvent} for the complementary
 * "every outbound teleport packet" signal — that event fires at this
 * site as well so either subscription sees the setback teleport.
 *
 * <p>Fires on the Netty thread associated with the user. Observational,
 * not cancellable.
 */
public final class DenemePlayerSetbackEvent extends DenemeSetbackEvent<DenemePlayerSetbackEvent.Channel> {
    private DenemePlayerSetbackEvent() {
        // Never instantiated — exists only as a Class key for bus.get(DenemePlayerSetbackEvent.class).
    }

    @FunctionalInterface
    public interface Handler {
        void onPlayerSetback(@NotNull DenemeUser user, int teleportId,
                             double x, double y, double z, long timestamp);
    }

    public static final class Channel extends EventChannel<DenemePlayerSetbackEvent, Handler> {
        public Channel() {
            super(DenemePlayerSetbackEvent.class, Handler.class);
        }

        public void onPlayerSetback(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onPlayerSetback(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onPlayerSetback(@NotNull Object pluginContext, @NotNull Handler handler) {
            onPlayerSetback(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onPlayerSetback(Object, Handler)}. */
        @Deprecated
        public void onPlayerSetback(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onPlayerSetback(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user, int teleportId,
                         double x, double y, double z, long timestamp) {
            Entry<Handler>[] entries = entries();
            for (Entry<Handler> e : entries) {
                try {
                    e.handler.onPlayerSetback(user, teleportId, x, y, z, timestamp);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemePlayerSetbackEvent event, @NotNull Handler handler, boolean cancelled) {
            // Unreachable — no public constructor, so no caller can post() one.
            throw new UnsupportedOperationException("DenemePlayerSetbackEvent has no legacy representation");
        }

        /** Bridge from {@link DenemeSetbackEvent.Handler} — used by the abstract channel when a setback-level subscriber registers. */
        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromSetback(@NotNull DenemeSetbackEvent.Handler abstractHandler) {
            return (user, id, x, y, z, ts) -> abstractHandler.onAnySetback(user, ts);
        }

        /** Bridge from root-level {@link ac.deneme.anticheat.api.event.DenemeEvent.Handler}. */
        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return (user, id, x, y, z, ts) -> abstractHandler.onAnyEvent(DenemePlayerSetbackEvent.class, false);
        }
    }
}
