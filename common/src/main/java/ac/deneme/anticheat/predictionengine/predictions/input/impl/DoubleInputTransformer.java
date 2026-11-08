package ac.deneme.anticheat.predictionengine.predictions.input.impl;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.predictionengine.predictions.input.DoubleInput;
import ac.deneme.anticheat.predictionengine.predictions.input.Input;
import ac.deneme.anticheat.predictionengine.predictions.input.InputTransformer;
import ac.deneme.anticheat.utils.math.DenemeMath;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.math.VectorUtils;

public class DoubleInputTransformer implements InputTransformer<DoubleInput> {
    @Override
    public DoubleInput transformInputsToVector(DenemePlayer player, int sideways, int vertical, int forward) {
        float bestPossibleX;
        float bestPossibleZ;

        if (player.isSlowMovement) {
            bestPossibleX = sideways * player.sneakingSpeedMultiplier;
            bestPossibleZ = forward * player.sneakingSpeedMultiplier;
        } else {
            bestPossibleX = DenemeMath.clamp(sideways, -1, 1);
            bestPossibleZ = DenemeMath.clamp(forward, -1, 1);
        }

        if (player.packetStateData.isSlowedByUsingItem()) {
            bestPossibleX *= 0.2F;
            bestPossibleZ *= 0.2F;
        }

        Vector3dm inputVector = new Vector3dm(bestPossibleX, 0, bestPossibleZ);
        inputVector.multiply(0.98F);

        inputVector = new Vector3dm((float) inputVector.getX(), (float) inputVector.getY(), (float) inputVector.getZ());

        if (inputVector.lengthSquared() > 1) {
            double d0 = VectorUtils.getVanillaLength(player.getClientVersion(), inputVector);
            inputVector = new Vector3dm(inputVector.getX() / d0, inputVector.getY() / d0, inputVector.getZ() / d0);
        }

        return new DoubleInput(inputVector.getX(), inputVector.getY(), inputVector.getZ());
    }

    @Override
    public Vector3dm getMovementResultFromInput(DenemePlayer player, Input inputVector, float speed, float yaw) {
        if (!(inputVector instanceof DoubleInput input)) {
            throw new IllegalStateException("Expected input vector of type DoubleInput, but got " + inputVector.getClass().getSimpleName());
        }

        float yawRadians = DenemeMath.radians(yaw);
        float sin = player.trigHandler.sin(yawRadians);
        float cos = player.trigHandler.cos(yawRadians);

        double xResult = input.sideways() * cos - input.forward() * sin;
        double zResult = input.forward() * cos + input.sideways() * sin;

        return new Vector3dm(xResult * speed, input.vertical() * speed, zResult * speed);
    }
}
