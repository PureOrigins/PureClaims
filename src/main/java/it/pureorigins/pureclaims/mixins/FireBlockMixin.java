package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(FireBlock.class)
public class FireBlockMixin {

  @Inject(method = "trySpreadingFire", at = @At("HEAD"), cancellable = true)
  public void trySpreadingFire(World world, BlockPos pos, int spreadFactor, Random rand, int currentAge, CallbackInfo ci) {
    if (PureClaims.INSTANCE.isClaimed(world, pos)) {
      var origin = (FireBlock) (Object) this;
      BlockPos originPos = null;
      for (Direction dir : Direction.values())
        if (world.getBlockState(pos.offset(dir)).getBlock() == origin) originPos = pos.offset(dir);
      if (originPos == null) LogManager.getLogger().info("porco troio");
      else if (!new ChunkPos(originPos).equals(new ChunkPos(pos))) {
        //TODO check if the fire is not spreading to the same chunk
      }
      ci.cancel();
    }
  }
}
