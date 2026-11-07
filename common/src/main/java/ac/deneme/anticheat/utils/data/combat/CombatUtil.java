package ac.deneme.anticheat.utils.data.combat;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public final class CombatUtil {

    private CombatUtil() {
    }

    public static double[] look(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);

        return new double[]{
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch
        };
    }

    public static double[] look(DenemePlayer player) {
        return look(player.yaw, player.pitch);
    }

    public static double eyeY(DenemePlayer player) {
        return player.y + player.getEyeHeight();
    }

    public static double angle(double[] a, double[] b) {
        double lengthA = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        double lengthB = Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
        if (lengthA < 1.0E-6 || lengthB < 1.0E-6) {
            return 0.0;
        }
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        double cosine = dot / (lengthA * lengthB);
        cosine = Math.max(-1.0, Math.min(1.0, cosine));
        return Math.toDegrees(Math.acos(cosine));
    }

    public static double angularRadius(double radius, double distance) {
        if (distance < 1.0E-3) {
            return 90.0;
        }
        return Math.toDegrees(Math.atan(radius / distance));
    }

    public static double halfDiagonal(double sizeX, double sizeY, double sizeZ) {
        return Math.sqrt((sizeX * 0.5) * (sizeX * 0.5) + (sizeY * 0.5) * (sizeY * 0.5) + (sizeZ * 0.5) * (sizeZ * 0.5));
    }

    public static double entropy(int[] counts) {
        int total = 0;
        for (int count : counts) {
            total += count;
        }
        if (total <= 0) {
            return 0.0;
        }
        double entropy = 0.0;
        for (int count : counts) {
            if (count <= 0) {
                continue;
            }
            double probability = (double) count / (double) total;
            entropy -= probability * (Math.log(probability) / Math.log(2.0));
        }
        return entropy;
    }

    public static boolean usable(DenemePlayer player, PacketEntity target) {
        if (player.disableDeneme || player.gamemode == null) {
            return false;
        }
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) {
            return false;
        }
        if (player.inVehicle()) {
            return false;
        }
        if (player.packetStateData.lastPacketWasTeleport) {
            return false;
        }
        if (player.packetStateData.horseInteractCausedForcedRotation) {
            return false;
        }
        if (player.vehicleData.wasVehicleSwitch) {
            return false;
        }
        if (target == null || target.isDead) {
            return false;
        }
        if (target.riding != null) {
            return false;
        }
        return !GeyserUtil.isBedrockPlayer(player.user.getUUID());
    }

    public static boolean occluded(DenemePlayer player, double targetX, double targetY, double targetZ) {
        double eyeX = player.x;
        double eyeY = eyeY(player);
        double eyeZ = player.z;

        double dirX = targetX - eyeX;
        double dirY = targetY - eyeY;
        double dirZ = targetZ - eyeZ;
        double distance = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);

        if (distance < 1.0 || distance > 8.0) {
            return false;
        }

        int minX = (int) Math.floor(Math.min(eyeX, targetX));
        int minY = (int) Math.floor(Math.min(eyeY, targetY));
        int minZ = (int) Math.floor(Math.min(eyeZ, targetZ));
        int maxX = (int) Math.floor(Math.max(eyeX, targetX));
        int maxY = (int) Math.floor(Math.max(eyeY, targetY));
        int maxZ = (int) Math.floor(Math.max(eyeZ, targetZ));

        if (player.compensatedWorld.areChunksUnloadedAt(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
        }

        for (double travelled = 0.5; travelled < distance - 0.5; travelled += 0.2) {
            double factor = travelled / distance;
            int blockX = (int) Math.floor(eyeX + dirX * factor);
            int blockY = (int) Math.floor(eyeY + dirY * factor);
            int blockZ = (int) Math.floor(eyeZ + dirZ * factor);

            WrappedBlockState state = player.compensatedWorld.getBlock(blockX, blockY, blockZ);
            if (state == null || state.getType() == null) {
                continue;
            }
            if (state.getType().isBlocking()) {
                return true;
            }
        }
        return false;
    }

    public static boolean lookReachesBox(DenemePlayer player, double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ, double expand) {
        double[] look = look(player);
        double originX = player.x;
        double originY = eyeY(player);
        double originZ = player.z;

        double lowX = minX - expand;
        double lowY = minY - expand;
        double lowZ = minZ - expand;
        double highX = maxX + expand;
        double highY = maxY + expand;
        double highZ = maxZ + expand;

        double tMin = 0.0;
        double tMax = Double.MAX_VALUE;

        double[][] axes = {
                {originX, look[0], lowX, highX},
                {originY, look[1], lowY, highY},
                {originZ, look[2], lowZ, highZ}
        };

        for (double[] axis : axes) {
            double origin = axis[0];
            double direction = axis[1];
            double low = axis[2];
            double high = axis[3];

            if (Math.abs(direction) < 1.0E-8) {
                if (origin < low || origin > high) {
                    return false;
                }
                continue;
            }

            double t1 = (low - origin) / direction;
            double t2 = (high - origin) / direction;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        }
        return tMin <= tMax && tMax > 0.0;
    }
}
