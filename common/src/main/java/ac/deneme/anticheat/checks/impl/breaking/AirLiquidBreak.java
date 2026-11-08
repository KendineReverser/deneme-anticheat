package ac.deneme.anticheat.checks.impl.breaking;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.BlockBreakListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AirLiquidBreak", stableKey = "deneme.breaking.air_liquid_break", description = "Breaking a block that cannot be broken")
public class AirLiquidBreak extends Check implements BlockBreakListener {
    private static final Verbose V = Verbose.of("block={block}, type={digging}");

    public final boolean noFireHitbox = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2);
    private int lastTick;
    private boolean didLastFlag;

    private @NotNull Vector3i lastBreakLoc = new Vector3i();
    private @NotNull StateType lastBlockType = StateTypes.AIR;

    public AirLiquidBreak(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.START_DIGGING && blockBreak.action != DiggingAction.FINISHED_DIGGING)
            return;

        final StateType block = blockBreak.block.getType();

        int newTick = DenemeAPI.INSTANCE.getTickManager().currentTick;
        if (lastTick == newTick
                && lastBreakLoc.equals(blockBreak.position)
                && !didLastFlag
                && lastBlockType.getHardness() == 0.0F
                && lastBlockType.getBlastResistance() == 0.0F
                && block == StateTypes.WATER
        ) return;
        lastTick = newTick;
        lastBreakLoc = blockBreak.position;
        lastBlockType = block;

        boolean invalid = (block == StateTypes.LIGHT && !(player.inventory.getHeldItem().is(ItemTypes.LIGHT) || player.inventory.getOffHand().is(ItemTypes.LIGHT)))
                || block.isAir()
                || block == StateTypes.WATER
                || block == StateTypes.LAVA
                || block == StateTypes.BUBBLE_COLUMN
                || block == StateTypes.MOVING_PISTON
                || block == StateTypes.FIRE && noFireHitbox

                || block.getHardness() == -1.0f && blockBreak.action == DiggingAction.FINISHED_DIGGING

                || player.inventory.getHeldItem().hasComponent(ComponentTypes.PIERCING_WEAPON) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);

        if (invalid && flag(V.write(verbose())
                .sint(VerboseCodecs.block(block, player.getClientVersion()))
                .uint(VerboseCodecs.enumId(blockBreak.action))) && shouldModifyPackets()) {
            didLastFlag = true;
            blockBreak.cancel();
        } else {
            didLastFlag = false;
        }
    }
}
