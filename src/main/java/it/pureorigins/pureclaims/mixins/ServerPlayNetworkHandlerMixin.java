package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
  @Shadow public ServerPlayerEntity player;
  
  @Inject(method = "onPlayerMove", at = @At("HEAD"))
  private void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo callback) {
    var oldPosition = player.getChunkPos();
    var newPosition = new ChunkPos(new BlockPos(packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ())));
    try {
      var oldClaim = PureClaims.INSTANCE.getClaim(player.world, oldPosition);
      var newClaim = PureClaims.INSTANCE.getClaim(player.world, newPosition);
      PureClaims.INSTANCE.sendClaimChangeMessage(player, oldClaim, newClaim);
    } catch (NullPointerException ignored) {
    }
  }
}
