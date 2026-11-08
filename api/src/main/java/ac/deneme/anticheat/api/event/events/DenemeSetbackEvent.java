package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.AbstractEventChannel;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Deneme executes a setback against a player — the semantic
 * anticheat action, independent of the specific packet type used.
 *
 * <p>Dispatches through two concrete subtypes:
 * <ul>
 *   <li>{@link DenemePlayerSetbackEvent} — player-on-foot setback, sent as a
 *       {@code ServerPlayerPositionAndLook} / teleport packet.</li>
 *   <li>{@link DenemeVehicleSetbackEvent} — player-in-vehicle setback, sent as
 *       a {@code ServerVehicleMove} packet.</li>
 * </ul>
 *
 * <p>Abstract-level subscribers receive a bridged dispatch for every fire
 * of either concrete child — the hot path goes through the child's channel
 * with no extra indirection. Observational, not cancellable.
 *
 * <p>Fires on the Netty thread associated with the user.
 */
public abstract class DenemeSetbackEvent<CHANNEL extends EventChannel<?, ?>> extends DenemeEvent<CHANNEL> {
    protected DenemeSetbackEvent() {
        super(true);
    }

    /**
     * Abstract-level setback handler. Fires for every concrete
     * {@code DenemeSetbackEvent} subtype (player + vehicle) and any addon
     * subtypes that opt into bridging.
     *
     * <p>Carries only the fields both children share: the user and the
     * timestamp at which the setback packet was emitted. Subscribers that
     * need the destination position, teleport id, or packet type should
     * subscribe at the concrete child level.
     */
    @FunctionalInterface
    public interface Handler {
        void onAnySetback(@NotNull DenemeUser user, long timestamp);
    }

    public static final class Channel extends AbstractEventChannel<DenemeSetbackEvent<?>, Handler> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Channel() {
            super((Class<DenemeSetbackEvent<?>>) (Class) DenemeSetbackEvent.class, Handler.class);
        }

        public void onAnySetback(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribeAbstract(handler, ListenerPriority.NORMAL, false, plugin);
        }

        public void onAnySetback(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribeAbstract(handler, priority, false, plugin);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onAnySetback(@NotNull Object pluginContext, @NotNull Handler handler) {
            subscribeAbstractResolving(pluginContext, handler, ListenerPriority.NORMAL, false);
        }

        /** @deprecated see {@link #onAnySetback(Object, Handler)}. */
        @Deprecated
        public void onAnySetback(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            subscribeAbstractResolving(pluginContext, handler, priority, false);
        }
    }
}
