package ac.deneme.anticheat.utils.nmsutil;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.collisions.CollisionData;
import ac.deneme.anticheat.utils.collisions.blocks.DoorHandler;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FluidTypeFlowing {

    public static Vector3dm getFlow(DenemePlayer player, int originalX, int originalY, int originalZ) {
        ClientVersion version = player.getClientVersion();
        return version.isOlderThan(ClientVersion.V_1_13) ? legacy$getFlow(player, version, originalX, originalY, originalZ) : modern$getFlow(player, version, originalX, originalY, originalZ);
    }

    private static float ownHeight(DenemePlayer player, int x, int y, int z) {
        WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
        if (state.getType() == StateTypes.LAVA) {
            return state.getLevel() >= 8 ? 8 / 9f : (8 - state.getLevel()) / 9f;
        }
        if (!Materials.isWater(player.getClientVersion(), state)) return 0f;
        if (state.getType() == StateTypes.WATER) {
            int level = state.getLevel();
            return (level & 0x8) == 8 ? 8 / 9f : (8 - level) / 9f;
        }
        return 8 / 9f;
    }

    private static Vector3dm modern$getFlow(DenemePlayer player, ClientVersion version, int originalX, int originalY, int originalZ) {
        float fluidLevel = ownHeight(player, originalX, originalY, originalZ);
        if (fluidLevel == 0) return new Vector3dm();

        double modX = 0.0D;
        double modZ = 0.0D;
        for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modifiedX = originalX + direction.getModX();
            int modifiedZ = originalZ + direction.getModZ();

            if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY, modifiedZ)) {
                float adjacentLevel = ownHeight(player, modifiedX, originalY, modifiedZ);
                float flow = 0.0F;
                if (adjacentLevel == 0.0F) {
                    StateType mat = player.compensatedWorld.getBlockType(modifiedX, originalY, modifiedZ);

                    if (Materials.isSolidBlockingBlacklist(mat, version)) {
                        if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY - 1, modifiedZ)) {
                            adjacentLevel = ownHeight(player, modifiedX, originalY - 1, modifiedZ);
                            if (adjacentLevel > 0.0F) {
                                flow = fluidLevel - (adjacentLevel - 0.8888889F);
                            }
                        }
                    }
                } else if (adjacentLevel > 0.0F) {
                    flow = fluidLevel - adjacentLevel;
                }

                if (flow != 0.0F) {
                    modX += direction.getModX() * flow;
                    modZ += direction.getModZ() * flow;
                }
            }
        }

        Vector3dm vec3d = new Vector3dm(modX, 0.0D, modZ);

        WrappedBlockState state = player.compensatedWorld.getBlock(originalX, originalY, originalZ);
        if ((state.getType() == StateTypes.WATER || state.getType() == StateTypes.LAVA) && state.getLevel() >= 8) {
            for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isSolidFace(player, originalX, originalY, originalZ, direction) || isSolidFace(player, originalX, originalY + 1, originalZ, direction)) {
                    vec3d = VectorUtils.normalize(player, vec3d).add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return VectorUtils.normalize(player, vec3d);
    }

    private static Vector3dm legacy$getFlow(DenemePlayer player, ClientVersion version, int originalX, int originalY, int originalZ) {
        WrappedBlockState state = player.compensatedWorld.getBlock(originalX, originalY, originalZ);
        boolean water = Materials.isWater(player.getClientVersion(), state);
        if (!water && state.getType() != StateTypes.LAVA) return new Vector3dm();

        int fluidLevel = legacy$getLiquidDepth(player, originalX, originalY, originalZ, water);
        if (fluidLevel < 0) return new Vector3dm();

        double modX = 0.0D;
        double modZ = 0.0D;
        for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modifiedX = originalX + direction.getModX();
            int modifiedZ = originalZ + direction.getModZ();
            int adjacentLevel = legacy$getLiquidDepth(player, modifiedX, originalY, modifiedZ, water);

            if (adjacentLevel < 0) {
                StateType mat = player.compensatedWorld.getBlockType(modifiedX, originalY, modifiedZ);
                if (Materials.isSolidBlockingBlacklist(mat, version)) {
                    adjacentLevel = legacy$getLiquidDepth(player, modifiedX, originalY - 1, modifiedZ, water);
                    if (adjacentLevel >= 0) {
                        int flow = adjacentLevel - (fluidLevel - 8);
                        modX += direction.getModX() * flow;
                        modZ += direction.getModZ() * flow;
                    }
                }
            } else {
                int flow = adjacentLevel - fluidLevel;
                modX += direction.getModX() * flow;
                modZ += direction.getModZ() * flow;
            }
        }

        Vector3dm vec3d = new Vector3dm(modX, 0.0D, modZ);
        if (state.getLevel() >= 8) {
            for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isSolidFace(player, originalX, originalY, originalZ, direction) || isSolidFace(player, originalX, originalY + 1, originalZ, direction)) {
                    vec3d = VectorUtils.normalize(player, vec3d).add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return VectorUtils.normalize(player, vec3d);
    }

    private static int legacy$getLiquidDepth(DenemePlayer player, int x, int y, int z, boolean water) {
        WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
        if (water ? !Materials.isWater(player.getClientVersion(), state) : state.getType() != StateTypes.LAVA) {
            return -1;
        }

        int level = state.getLevel();
        return level >= 8 ? 0 : level;
    }

    private static boolean affectsFlow(DenemePlayer player, int originalX, int originalY, int originalZ, int x2, int y2, int z2) {
        return isEmpty(player, x2, y2, z2) || isSame(player, originalX, originalY, originalZ, x2, y2, z2);
    }

    private static boolean isSolidFace(DenemePlayer player, int originalX, int y, int originalZ, BlockFace direction) {
        int x = originalX + direction.getModX();
        int z = originalZ + direction.getModZ();

        WrappedBlockState data = player.compensatedWorld.getBlock(x, y, z);
        StateType type = data.getType();

        if (isSame(player, x, y, z, originalX, y, originalZ)) return false;
        if (type == StateTypes.ICE) return false;

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12)) {
            if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON) {
                return data.getFacing().getOppositeFace() == direction ||
                        CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, 0, 0, 0).isFullBlock();
            } else if (type == StateTypes.PISTON_HEAD) {
                return data.getFacing() == direction;
            }
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_12)) {

            return !Materials.isSolidBlockingBlacklist(type, player.getClientVersion());
        } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_13_2)) {

            if (Materials.isStairs(type) || Materials.isLeaves(type)
                    || Materials.isShulker(type) || Materials.isGlassBlock(type)
                    || BlockTags.TRAPDOORS.contains(type))
                return false;

            if (type == StateTypes.BEACON || BlockTags.CAULDRONS.contains(type)
                    || type == StateTypes.GLOWSTONE || type == StateTypes.SEA_LANTERN || type == StateTypes.CONDUIT)
                return false;

            if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON || type == StateTypes.PISTON_HEAD)
                return false;

            return type == StateTypes.SOUL_SAND || (CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isFullBlock());
        } else {
            if (Materials.isLeaves(type)) {

                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2);
            } else if (type == StateTypes.SNOW) {
                return data.getLayers() == 8;
            } else if (Materials.isStairs(type)) {
                return data.getFacing() == direction;
            } else if (type == StateTypes.COMPOSTER) {
                return true;
            } else if (type == StateTypes.SOUL_SAND) {
                return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16);
            } else if (type == StateTypes.MUD) {

                return true;
            } else if (type == StateTypes.LADDER) {
                return data.getFacing().getOppositeFace() == direction;
            } else if (BlockTags.TRAPDOORS.contains(type)) {
                return data.getFacing().getOppositeFace() == direction && data.isOpen();
            } else if (BlockTags.DOORS.contains(type)) {
                CollisionData collisionData = CollisionData.getData(type);

                if (collisionData.dynamic instanceof DoorHandler) {
                    BlockFace dir = ((DoorHandler) collisionData.dynamic).fetchDirection(player, player.getClientVersion(), data, x, y, z);
                    return dir.getOppositeFace() == direction;
                }
            }

            return (CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isFullBlock());
        }
    }

    public static boolean isEmpty(DenemePlayer player, int x, int y, int z) {
        return player.compensatedWorld.getFluidLevelAt(x, y, z) == 0;
    }

    public static boolean isSame(DenemePlayer player, int x1, int y1, int z1, int x2, int y2, int z2) {
        return player.compensatedWorld.getWaterFluidLevelAt(x1, y1, z1) > 0 &&
                player.compensatedWorld.getWaterFluidLevelAt(x2, y2, z2) > 0 ||
                player.compensatedWorld.getLavaFluidLevelAt(x1, y1, z1) > 0 &&
                        player.compensatedWorld.getLavaFluidLevelAt(x2, y2, z2) > 0;
    }
}
