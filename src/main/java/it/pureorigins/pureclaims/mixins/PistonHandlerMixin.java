package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimedChunk;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(PistonHandler.class)
public abstract class PistonHandlerMixin {
  @Shadow @Final private World world;
  @Shadow @Final private BlockPos posFrom;
  @Shadow public abstract List<BlockPos> getMovedBlocks();
  @Shadow public abstract List<BlockPos> getBrokenBlocks();

  private static Predicate<BlockPos> checkBlock(ClaimedChunk chunk, World world) {
    return (BlockPos b) -> {
      var c = PureClaims.INSTANCE.getClaim(world, b);
      return c != null && (chunk == null || !chunk.getOwner().equals(c.getOwner()));
    };
  }

  @Inject(method = "calculatePush", at = @At("TAIL"), cancellable = true)
  private void calculatePush(CallbackInfoReturnable<Boolean> callback) {
    if (callback.getReturnValue()) {
      var chunk = PureClaims.INSTANCE.getClaim(this.world, this.posFrom);
      var bol = checkBlock(chunk, world);
      if (getBrokenBlocks().stream().anyMatch(bol) || getMovedBlocks().stream().anyMatch(bol)) {
        for (BlockPos pos : getMovedBlocks()) {
          world.setBlockState(pos, world.getBlockState(pos), Block.NOTIFY_ALL);
        }
        for (BlockPos pos : getBrokenBlocks()) {
          world.setBlockState(pos, world.getBlockState(pos), Block.NOTIFY_ALL);
        }
        getMovedBlocks().clear();
        getBrokenBlocks().clear();
        callback.setReturnValue(false);
      }
    }
  }
}
