package ac.deneme.anticheat.utils.data.packetentity;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.VectorData;

import java.util.Set;

public interface JumpableEntity {

    boolean isJumping();

    void setJumping(boolean jumping);

    float getJumpPower();

    void setJumpPower(float jumpPower);

    boolean canPlayerJump(DenemePlayer player);

    boolean hasSaddle();

    void executeJump(DenemePlayer player, Set<VectorData> possibleVectors);

}
