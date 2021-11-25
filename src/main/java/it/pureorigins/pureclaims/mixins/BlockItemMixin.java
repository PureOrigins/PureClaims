package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimPermissions;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.LilyPadItem;
import net.minecraft.item.TallBlockItem;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
  @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
  private void place(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> callback) {
    if (context.getPlayer() instanceof ServerPlayerEntity player && !PureClaims.INSTANCE.checkPermissions(context.getPlayer(), context.getBlockPos(), ClaimPermissions.EDIT)) {
      if (context.getStack().getItem() instanceof TallBlockItem) {
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(context.getWorld(), context.getBlockPos().up()));
      }
      if (context.getStack().getItem() instanceof LilyPadItem) {
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(context.getWorld(), context.getBlockPos()));
      }
      callback.setReturnValue(ActionResult.FAIL);
    }
  }
}
