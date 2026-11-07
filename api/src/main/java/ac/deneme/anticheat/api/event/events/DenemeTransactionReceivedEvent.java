package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Deneme receives an inbound response for a transaction packet that
 * it previously sent.
 *
 * <p>Deneme cancels these inbound packets by default, controlled by the
 * {@code disable-pong-cancelling} option in {@code config.yml}. The
 * {@code packetCancelled} parameter reflects whether Deneme cancelled packet
 * handling; it is not an event-cancellation flag (this event is observational
 * and not cancellable).
 *
 * <p>Only fires for transactions initiated by Deneme, on the Netty thread
 * associated with the user.
 */
public final class DenemeTransactionReceivedEvent extends DenemeEvent<DenemeTransactionReceivedEvent.Channel> {
    private DenemeTransactionReceivedEvent() {

    }

    @FunctionalInterface
    public interface Handler {
        void onTransactionReceived(@NotNull DenemeUser user, int transactionId, boolean packetCancelled, long timestamp);
    }

    public static final class Channel extends EventChannel<DenemeTransactionReceivedEvent, Handler> {
        public Channel() {
            super(DenemeTransactionReceivedEvent.class, Handler.class);
        }

        public void onTransactionReceived(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onTransactionReceived(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onTransactionReceived(@NotNull Object pluginContext, @NotNull Handler handler) {
            onTransactionReceived(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onTransactionReceived(Object, Handler)}. */
        @Deprecated
        public void onTransactionReceived(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onTransactionReceived(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user, int transactionId, boolean packetCancelled, long timestamp) {
            Entry<Handler>[] entries = entries();
            for (Entry<Handler> e : entries) {
                try {
                    e.handler.onTransactionReceived(user, transactionId, packetCancelled, timestamp);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeTransactionReceivedEvent event, @NotNull Handler handler, boolean cancelled) {
            throw new UnsupportedOperationException("DenemeTransactionReceivedEvent has no legacy representation");
        }

        @org.jetbrains.annotations.ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return (user, id, c, ts) -> abstractHandler.onAnyEvent(DenemeTransactionReceivedEvent.class, false);
        }
    }
}
