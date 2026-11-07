package ac.deneme.anticheat.platform.fabric.mixins;

import ac.deneme.anticheat.platform.api.world.PlatformChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
@Implements(@Interface(iface = PlatformChunk.class, prefix = "denemeanticheat$"))
abstract class FabricIntermediaryLevelChunkMixin {
    @Unique
    private static final BlockPos.MutableBlockPos denemeanticheat$sharedPos = new BlockPos.MutableBlockPos();

    public int denemeanticheat$getBlockID(int x, int y, int z) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        denemeanticheat$sharedPos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
        return Block.getId(chunk.getBlockState(denemeanticheat$sharedPos));
    }
}
