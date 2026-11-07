package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Deneme sends a player-in-vehicle setback — the
 * {@code ServerVehicleMove} packet branch of {@link DenemeSetbackEvent}.
 *
 * <p>Unlike {@link DenemePlayerSetbackEvent}, vehicle-move packets carry no
 * teleport id, so there is nothing for a packet-tracking consumer to
 * correlate against an incoming confirm; this event exists primarily for
 * the semantic "Deneme did a setback" audience (admin tools, stats,
 * anticheat-test harnesses).
 *
 * <p>Fires on the Netty thread associated with the user. Observational,
 * not cancellable.
 */
public final class DenemeVehicleSetbackEvent extends DenemeSetbackEvent<DenemeVehicleSetbackEvent.Channel> {
    private DenemeVehicleSetbackEvent() {

    }

    @FunctionalInterface
    public interface Handler {
        void onVehicleSetback(@NotNull DenemeUser user,
                              double x, double y, double z, long timestamp);
    }

    public static final class Channel extends EventChannel<DenemeVehicleSetbackEvent, Handler> {
        public Channel() {
            super(DenemeVehicleSetbackEvent.class, Handler.class);
        }

        public void onVehicleSetback(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onVehicleSetback(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onVehicleSetback(@NotNull Object pluginContext, @NotNull Handler handler) {
            onVehicleSetback(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onVehicleSetback(Object, Handler)}. */
        @Deprecated
        public void onVehicleSetback(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onVehicleSetback(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user,
                         double x, double y, double z, long timestamp) {
            Entry<Handler>[] entries = entries();
            for (Entry<Handler> e : entries) {
                try {
                    e.handler.onVehicleSetback(user, x, y, z, timestamp);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeVehicleSetbackEvent event, @NotNull Handler handler, boolean cancelled) {

            throw new UnsupportedOperationException("DenemeVehicleSetbackEvent has no legacy representation");
        }

        /** Bridge from {@link DenemeSetbackEvent.Handler} — used by the abstract channel when a setback-level subscriber registers. */
        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromSetback(@NotNull DenemeSetbackEvent.Handler abstractHandler) {
            return (user, x, y, z, ts) -> abstractHandler.onAnySetback(user, ts);
        }

        /** Bridge from root-level {@link ac.deneme.anticheat.api.event.DenemeEvent.Handler}. */
        @ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return (user, x, y, z, ts) -> abstractHandler.onAnyEvent(DenemeVehicleSetbackEvent.class, false);
        }
    }
}
