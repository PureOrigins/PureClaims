package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimedChunk;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(PistonHandler.class)
public abstract class PistonHandlerMixin {
  @Mutable
  @Final
  @Shadow
  private final World world;
  @Mutable
  @Final
  @Shadow
  private final BlockPos posFrom;

  @Shadow
  public abstract List<BlockPos> getMovedBlocks();

  @Shadow
  public abstract List<BlockPos> getBrokenBlocks();

  public PistonHandlerMixin(World world, BlockPos posFrom) {
    this.world = world;
    this.posFrom = posFrom;
  }

  private static Predicate<BlockPos> checkBlock(ClaimedChunk chunk, World world) {
    return (BlockPos b) -> {
      var c = PureClaims.INSTANCE.getClaim(world, b);
      return c != null && (chunk == null || !chunk.getOwner().equals(c.getOwner()));
    };
  }

  @Inject(method = "calculatePush", at = @At("TAIL"), cancellable = true)
  private void calculatePush(CallbackInfoReturnable<Boolean> cir) {
    var chunk = PureClaims.INSTANCE.getClaim(this.world, this.posFrom);
    LogManager.getLogger().info(this.posFrom);
    var bol = checkBlock(chunk, this.world);
    LogManager.getLogger().info("Moved: " + getMovedBlocks());
    LogManager.getLogger().info("Broken: " + getBrokenBlocks());
    if (getBrokenBlocks().stream().anyMatch(bol) || getMovedBlocks().stream().anyMatch(bol)) {
      LogManager.getLogger().info("Cancelled");
      getMovedBlocks().clear();
      getBrokenBlocks().clear();
      cir.setReturnValue(false);
      cir.cancel();
    }
  }
}
