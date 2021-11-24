package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin extends Block {
  private FarmlandBlockMixin(Settings settings) {
    super(settings);
  }
  
  @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
  private void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo callback) {
    if (entity instanceof ServerPlayerEntity player && !PureClaims.INSTANCE.checkInteractPermissions(player, pos)) {
      super.onLandedUpon(world, state, pos, entity, fallDistance);
      callback.cancel();
    }
  }
}
