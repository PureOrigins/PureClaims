package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimPermissions;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin extends Entity {
  @Shadow private Entity owner;
  
  public ProjectileEntityMixin(EntityType<?> type, World world) {
    super(type, world);
  }
  
  @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
  private void onCollision(HitResult hit, CallbackInfo callback) {
    if (hit.getType() == HitResult.Type.ENTITY) {
      var entity = ((EntityHitResult) hit).getEntity();
      if (entity.getType() != EntityType.PLAYER) {
        if (entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity)) {
          if (!PureClaims.INSTANCE.checkIndirectPermissions(owner, entity.getBlockPos(), ClaimPermissions.DAMAGE_MOBS)) {
            callback.cancel();
          }
        } else {
          if (!PureClaims.INSTANCE.checkIndirectPermissions(owner, entity.getBlockPos(), ClaimPermissions.EDIT)) {
            callback.cancel();
          }
        }
      }
    }
  }
}
