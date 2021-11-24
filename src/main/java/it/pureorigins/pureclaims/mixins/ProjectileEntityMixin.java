package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {
  @Shadow private Entity owner;
  
  @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
  private void onCollision(HitResult hit, CallbackInfo callback) {
    if (owner instanceof ServerPlayerEntity player) {
      if (hit.getType() == HitResult.Type.ENTITY) {
        var entity = ((EntityHitResult) hit).getEntity();
        if (entity.getType() != EntityType.PLAYER && entity instanceof LivingEntity) {
          if (!PureClaims.INSTANCE.checkDamageMobPermissions(player, entity.getChunkPos())) {
            callback.cancel();
          }
        }
      }
    }
  }
}
