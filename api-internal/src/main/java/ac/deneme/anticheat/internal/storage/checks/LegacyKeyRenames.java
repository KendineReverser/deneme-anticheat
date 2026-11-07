package ac.deneme.anticheat.internal.storage.checks;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical {@code deneme.legacy.* → deneme.<category>.<descriptive>} rename map.
 * Consumed by:
 *
 * <ul>
 *   <li>{@link StableKeyMapping} when assigning stable_keys during V0→V1 migration.</li>
 *   <li>Each backend's schema migration step that runs an
 *       {@code UPDATE deneme_checks SET stable_key = ? WHERE stable_key = ?} pass
 *       to rewrite already-persisted rows on existing operator installs.</li>
 * </ul>
 *
 * <p>Order matters only for readability — every entry's old key is unique and
 * the migration applies them all idempotently. Entries omitted here keep their
 * {@code deneme.legacy.*} key forever (V0-only historical checks with no live V2
 * class — aim-fold/gold/hold, looka, clientbrand — stay legacy because there
 * is no source-of-truth class to rename).
 */
@ApiStatus.Internal
public final class LegacyKeyRenames {

    private LegacyKeyRenames() {}

    public static final Map<String, String> OLD_TO_NEW;

    static {
        Map<String, String> m = new LinkedHashMap<>();

        m.put("deneme.legacy.badpacketsb", "deneme.badpackets.ignored_rotation");
        m.put("deneme.legacy.badpacketsc", "deneme.badpackets.wake_not_sleeping");
        m.put("deneme.legacy.badpacketsh", "deneme.badpackets.unexpected_sequence");
        m.put("deneme.legacy.badpacketsj", "deneme.badpackets.use_item_rotation_mismatch");
        m.put("deneme.legacy.badpacketsr", "deneme.badpackets.position_starvation");
        m.put("deneme.legacy.badpacketss", "deneme.badpackets.window_confirmation_not_accepted");
        m.put("deneme.legacy.badpacketst", "deneme.badpackets.invalid_interact_vector");
        m.put("deneme.legacy.badpacketsw", "deneme.badpackets.invalid_entity_target");
        m.put("deneme.legacy.badpacketsx", "deneme.badpackets.extra_input_actions");
        m.put("deneme.legacy.badpacketsz", "deneme.badpackets.duplicate_player_input");

        m.put("deneme.legacy.chatc", "deneme.chat.moving_while_chatting");
        m.put("deneme.legacy.exploita", "deneme.exploit.anvil_name_length");
        m.put("deneme.legacy.groundspoof", "deneme.groundspoof.fake");
        m.put("deneme.legacy.timerlimit", "deneme.timer.limit");

        m.put("deneme.legacy.elytraa", "deneme.elytra.already_gliding");
        m.put("deneme.legacy.elytrab", "deneme.elytra.no_jump");
        m.put("deneme.legacy.elytrac", "deneme.elytra.too_frequent");
        m.put("deneme.legacy.elytrad", "deneme.elytra.no_elytra");
        m.put("deneme.legacy.elytrae", "deneme.elytra.flying");
        m.put("deneme.legacy.elytraf", "deneme.elytra.grounded");
        m.put("deneme.legacy.elytrag", "deneme.elytra.levitation");
        m.put("deneme.legacy.elytrah", "deneme.elytra.vehicle");
        m.put("deneme.legacy.elytrai", "deneme.elytra.water");

        m.put("deneme.legacy.multiactionsa", "deneme.multiactions.attack_while_using");
        m.put("deneme.legacy.multiactionsb", "deneme.multiactions.break_while_using");
        m.put("deneme.legacy.multiactionsc", "deneme.multiactions.inventory_click_while_moving");
        m.put("deneme.legacy.multiactionsd", "deneme.multiactions.inventory_close_while_moving");
        m.put("deneme.legacy.multiactionse", "deneme.multiactions.swing_while_using");
        m.put("deneme.legacy.multiactionsf", "deneme.multiactions.block_and_entity_interact");
        m.put("deneme.legacy.multiactionsg", "deneme.multiactions.action_while_rowing");

        m.put("deneme.legacy.multiinteracta", "deneme.multiinteract.multiple_targets");
        m.put("deneme.legacy.multiinteractb", "deneme.multiinteract.interact_at_position_changed");

        m.put("deneme.legacy.packetordera", "deneme.packetorder.window_click_order");
        m.put("deneme.legacy.packetorderb", "deneme.packetorder.noswing");
        m.put("deneme.legacy.packetorderc", "deneme.packetorder.interact_order");
        m.put("deneme.legacy.packetorderd", "deneme.packetorder.interact_hand_order");
        m.put("deneme.legacy.packetordere", "deneme.packetorder.slot_order");
        m.put("deneme.legacy.packetorderf", "deneme.packetorder.input_tick_to_sneak_sprint_order");
        m.put("deneme.legacy.packetorderg", "deneme.packetorder.hotbar_inventory_manage_order");
        m.put("deneme.legacy.packetorderh", "deneme.packetorder.sneak_sprint_order");
        m.put("deneme.legacy.packetorderi", "deneme.packetorder.input_tick_order");
        m.put("deneme.legacy.packetorderj", "deneme.packetorder.attack_interact_use_order");
        m.put("deneme.legacy.packetorderk", "deneme.packetorder.inventory_open_order");
        m.put("deneme.legacy.packetorderl", "deneme.packetorder.drop_item_order");
        m.put("deneme.legacy.packetorderm", "deneme.packetorder.interact_use_order");
        m.put("deneme.legacy.packetordern", "deneme.packetorder.place_use_order");
        m.put("deneme.legacy.packetordero", "deneme.packetorder.tick_end_order");
        m.put("deneme.legacy.packetorderp", "deneme.packetorder.transaction_response_order");

        m.put("deneme.legacy.sprinta", "deneme.sprint.hunger");
        m.put("deneme.legacy.sprintb", "deneme.sprint.sneaking");
        m.put("deneme.legacy.sprintc", "deneme.sprint.using_item");
        m.put("deneme.legacy.sprintd", "deneme.sprint.blindness");
        m.put("deneme.legacy.sprinte", "deneme.sprint.wall");
        m.put("deneme.legacy.sprintf", "deneme.sprint.gliding");
        m.put("deneme.legacy.sprintg", "deneme.sprint.water");

        m.put("deneme.legacy.vehiclea", "deneme.vehicle.impossible_input");
        m.put("deneme.legacy.vehicleb", "deneme.vehicle.spoofed_vehicle");
        m.put("deneme.legacy.vehiclec", "deneme.vehicle.vehicle_control");
        m.put("deneme.legacy.vehicled", "deneme.vehicle.spoofed_jump");
        m.put("deneme.legacy.vehiclee", "deneme.vehicle.spoofed_boat");
        m.put("deneme.legacy.vehiclef", "deneme.vehicle.boat_input_mismatch");

        OLD_TO_NEW = Collections.unmodifiableMap(m);
    }
}
