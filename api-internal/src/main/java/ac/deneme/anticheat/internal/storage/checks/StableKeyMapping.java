package ac.deneme.anticheat.internal.storage.checks;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bridges legacy {@code deneme_history_check_names} display strings to the v1
 * schema's {@code stable_key} column.
 * <p>
 * Two consumers:
 * <ul>
 *   <li>v0 → v1 migration: {@code LegacyMigrator} resolves every distinct
 *       {@code check_name_string} through this map so historical violations
 *       land on the new schema with a meaningful stable_key.</li>
 *   <li>Live-write fallback: {@code LiveWriteHooks} consults this map when a
 *       Check hasn't declared a {@code stableKey} on its {@code @CheckData}
 *       / {@code CheckInfo}.</li>
 * </ul>
 * <p>
 * Unknown names hit the {@link #legacyFallback} path so migration always
 * completes; operators can rename the fallback keys later through the check
 * registry if they want.
 */
@ApiStatus.Internal
public final class StableKeyMapping {

    private static final Map<String, String> MAPPINGS = buildMappings();

    private StableKeyMapping() {}

    public static Optional<String> stableKeyFor(String legacyDisplayName) {
        if (legacyDisplayName == null) return Optional.empty();
        return Optional.ofNullable(MAPPINGS.get(legacyDisplayName.toLowerCase(Locale.ROOT)));
    }

    public static String legacyFallback(String legacyDisplayName) {
        return "deneme.legacy." + legacyDisplayName.toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> buildMappings() {

        Map<String, String> m = new HashMap<>(160);

        m.put("badpacketsa", "deneme.badpackets.duplicate_slot");
        m.put("badpacketsd", "deneme.badpackets.invalid_pitch");
        m.put("badpacketse", "deneme.badpackets.invalid_position");
        m.put("badpacketsf", "deneme.badpackets.duplicate_sprint");
        m.put("badpacketsg", "deneme.badpackets.duplicate_sneak");
        m.put("badpacketsi", "deneme.badpackets.spoofed_abilities");
        m.put("badpacketsk", "deneme.badpackets.invalid_spectate");
        m.put("badpacketsl", "deneme.badpackets.invalid_dig");
        m.put("badpacketsm", "deneme.badpackets.respawn_alive");
        m.put("badpacketsn", "deneme.badpackets.invalid_teleport");
        m.put("badpacketso", "deneme.badpackets.invalid_keepalive");
        m.put("badpacketsp", "deneme.badpackets.invalid_click");
        m.put("badpacketsq", "deneme.badpackets.invalid_horse_jump");
        m.put("badpacketsu", "deneme.badpackets.invalid_block_placement");
        m.put("badpacketsv", "deneme.badpackets.slow_move");
        m.put("badpacketsy", "deneme.badpackets.oob_slot");
        m.put("badpacketsb", "deneme.badpackets.ignored_rotation");
        m.put("badpacketsc", "deneme.badpackets.wake_not_sleeping");
        m.put("badpacketsh", "deneme.badpackets.unexpected_sequence");
        m.put("badpacketsj", "deneme.badpackets.use_item_rotation_mismatch");
        m.put("badpacketsr", "deneme.badpackets.position_starvation");
        m.put("badpacketss", "deneme.badpackets.window_confirmation_not_accepted");
        m.put("badpacketst", "deneme.badpackets.invalid_interact_vector");
        m.put("badpacketsw", "deneme.badpackets.invalid_entity_target");
        m.put("badpacketsx", "deneme.badpackets.extra_input_actions");
        m.put("badpacketsz", "deneme.badpackets.duplicate_player_input");
        m.put("selfinteract", "deneme.badpackets.self_hit");

        m.put("crasha", "deneme.crash.large_position");
        m.put("crashb", "deneme.crash.creative_while_not_creative");
        m.put("crashc", "deneme.crash.nan_position");
        m.put("crashd", "deneme.crash.lectern");
        m.put("crashe", "deneme.crash.low_view_distance");
        m.put("crashf", "deneme.crash.button_crash");
        m.put("crashg", "deneme.crash.negative_sequence");
        m.put("crashh", "deneme.crash.invalid_tab_complete");
        m.put("crashi", "deneme.crash.invalid_bundle_slot");

        m.put("hitboxes", "deneme.combat.hitboxes");
        m.put("reach", "deneme.combat.reach");

        m.put("aimduplicatelook", "deneme.aim.duplicate_look");
        m.put("aimmodulo360", "deneme.aim.modulo_360");
        m.put("aimfold", "deneme.legacy.aimfold");
        m.put("aimgold", "deneme.legacy.aimgold");
        m.put("aimhold", "deneme.legacy.aimhold");

        m.put("airliquidbreak", "deneme.breaking.air_liquid_break");
        m.put("farbreak", "deneme.breaking.far_break");
        m.put("fastbreak", "deneme.breaking.fast_break");
        m.put("invalidbreak", "deneme.breaking.invalid_break");
        m.put("multibreak", "deneme.breaking.multi_break");
        m.put("noswingbreak", "deneme.breaking.no_swing_break");
        m.put("positionbreaka", "deneme.breaking.position_break_a");
        m.put("positionbreakb", "deneme.breaking.position_break_b");
        m.put("rotationbreak", "deneme.breaking.rotation_break");
        m.put("wrongbreak", "deneme.breaking.wrong_break");

        m.put("airliquidplace", "deneme.scaffolding.air_liquid_place");
        m.put("duplicaterotplace", "deneme.scaffolding.duplicate_rot_place");
        m.put("fabricatedplace", "deneme.scaffolding.fabricated_place");
        m.put("farplace", "deneme.scaffolding.far_place");
        m.put("invalidplacea", "deneme.scaffolding.invalid_place_a");
        m.put("invalidplaceb", "deneme.scaffolding.invalid_place_b");
        m.put("multiplace", "deneme.scaffolding.multi_place");
        m.put("positionplace", "deneme.scaffolding.position_place");
        m.put("rotationplace", "deneme.scaffolding.rotation_place");

        m.put("chatc", "deneme.chat.moving_while_chatting");

        m.put("chata", "deneme.exploit.blank_tab_complete");
        m.put("chatb", "deneme.exploit.spigot_antispam_bypass");
        m.put("chatd", "deneme.exploit.chat_while_hidden");
        m.put("exploita", "deneme.exploit.anvil_name_length");
        m.put("exploitb", "deneme.exploit.invalid_book_edit");
        m.put("exploitc", "deneme.legacy.exploitc");

        m.put("phase", "deneme.prediction.phase");

        m.put("noslow", "deneme.movement.noslow");

        m.put("groundspoof", "deneme.groundspoof.fake");
        m.put("nofall", "deneme.groundspoof.no_fall");

        m.put("post", "deneme.post.invalid_order");

        m.put("transactionorder", "deneme.ping.invalid_transaction_order");

        m.put("baritone", "deneme.baritone.baritone");

        m.put("negativetimer", "deneme.timer.negative");
        m.put("ticktimer", "deneme.timer.tick");
        m.put("timer", "deneme.timer.timer");
        m.put("timerlimit", "deneme.timer.limit");
        m.put("vehicletimer", "deneme.timer.vehicle");

        m.put("elytraa", "deneme.elytra.already_gliding");
        m.put("elytrab", "deneme.elytra.no_jump");
        m.put("elytrac", "deneme.elytra.too_frequent");
        m.put("elytrad", "deneme.elytra.no_elytra");
        m.put("elytrae", "deneme.elytra.flying");
        m.put("elytraf", "deneme.elytra.grounded");
        m.put("elytrag", "deneme.elytra.levitation");
        m.put("elytrah", "deneme.elytra.vehicle");
        m.put("elytrai", "deneme.elytra.water");

        m.put("sprinta", "deneme.sprint.hunger");
        m.put("sprintb", "deneme.sprint.sneaking");
        m.put("sprintc", "deneme.sprint.using_item");
        m.put("sprintd", "deneme.sprint.blindness");
        m.put("sprinte", "deneme.sprint.wall");
        m.put("sprintf", "deneme.sprint.gliding");
        m.put("sprintg", "deneme.sprint.water");

        m.put("vehiclea", "deneme.vehicle.impossible_input");
        m.put("vehicleb", "deneme.vehicle.spoofed_vehicle");
        m.put("vehiclec", "deneme.vehicle.vehicle_control");
        m.put("vehicled", "deneme.vehicle.spoofed_jump");
        m.put("vehiclee", "deneme.vehicle.spoofed_boat");
        m.put("vehiclef", "deneme.vehicle.boat_input_mismatch");

        m.put("multiactionsa", "deneme.multiactions.attack_while_using");
        m.put("multiactionsb", "deneme.multiactions.break_while_using");
        m.put("multiactionsc", "deneme.multiactions.inventory_click_while_moving");
        m.put("multiactionsd", "deneme.multiactions.inventory_close_while_moving");
        m.put("multiactionse", "deneme.multiactions.swing_while_using");
        m.put("multiactionsf", "deneme.multiactions.block_and_entity_interact");
        m.put("multiactionsg", "deneme.multiactions.action_while_rowing");

        m.put("multiinteracta", "deneme.multiinteract.multiple_targets");
        m.put("multiinteractb", "deneme.multiinteract.interact_at_position_changed");

        m.put("packetordera", "deneme.packetorder.window_click_order");
        m.put("packetorderb", "deneme.packetorder.noswing");
        m.put("packetorderc", "deneme.packetorder.interact_order");
        m.put("packetorderd", "deneme.packetorder.interact_hand_order");
        m.put("packetordere", "deneme.packetorder.slot_order");
        m.put("packetorderf", "deneme.packetorder.input_tick_to_sneak_sprint_order");
        m.put("packetorderg", "deneme.packetorder.hotbar_inventory_manage_order");
        m.put("packetorderh", "deneme.packetorder.sneak_sprint_order");
        m.put("packetorderi", "deneme.packetorder.input_tick_order");
        m.put("packetorderj", "deneme.packetorder.attack_interact_use_order");
        m.put("packetorderk", "deneme.packetorder.inventory_open_order");
        m.put("packetorderl", "deneme.packetorder.drop_item_order");
        m.put("packetorderm", "deneme.packetorder.interact_use_order");
        m.put("packetordern", "deneme.packetorder.place_use_order");
        m.put("packetordero", "deneme.packetorder.tick_end_order");
        m.put("packetorderp", "deneme.packetorder.transaction_response_order");

        m.put("looka", "deneme.legacy.looka");
        m.put("clientbrand", "deneme.legacy.clientbrand");
        m.put("inventorya", "deneme.legacy.inventorya");
        m.put("inventoryb", "deneme.legacy.inventoryb");
        m.put("inventoryc", "deneme.legacy.inventoryc");
        m.put("inventoryd", "deneme.legacy.inventoryd");
        m.put("inventorye", "deneme.legacy.inventorye");
        m.put("inventoryf", "deneme.legacy.inventoryf");
        m.put("inventoryg", "deneme.legacy.inventoryg");

        return Map.copyOf(m);
    }
}
