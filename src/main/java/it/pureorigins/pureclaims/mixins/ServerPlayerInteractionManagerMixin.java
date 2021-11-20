package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimPermissions;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
  @Shadow protected ServerWorld world;
  @Shadow @Final protected ServerPlayerEntity player;
  
  @Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
  private void onProcessBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, CallbackInfo callback) {
    if (!PureClaims.INSTANCE.checkPermissions(player, pos, ClaimPermissions::getCanEdit)) {
      player.networkHandler.sendPacket(new PlayerActionResponseS2CPacket(pos, world.getBlockState(pos), action, false, "insufficient claim permissions"));
      callback.cancel();
    }
  }
  
  @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
  public void interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> info) {
    // ...
  }
}
