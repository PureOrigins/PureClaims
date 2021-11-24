package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

  /**
   * @author Raymark
   */
  @Overwrite
  public Explosion createExplosion(Entity entity, DamageSource damageSource, ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType) {
    ServerWorld instance = (ServerWorld) (Object) this;
    Explosion explosion = new Explosion(instance, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
    explosion.collectBlocksAndDamageEntities();


    if (entity instanceof MobEntity mob) { //Creeper explosions
      if (mob.getTarget() instanceof ServerPlayerEntity player)
        explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.checkEditPermissions(player, block));
    } else if (entity instanceof FireballEntity fireball) { //Fireball explosions
      if (fireball.getOwner() instanceof ServerPlayerEntity player) {
        explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.checkEditPermissions(player, block));
      } else if (fireball.getOwner() instanceof MobEntity thrower) {
        if (thrower.getTarget() instanceof ServerPlayerEntity target)
          explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.checkEditPermissions(target, block));
      }
    } else if (entity instanceof TntEntity tnt) { //TNT explosions
      if (tnt.getCausingEntity() instanceof ServerPlayerEntity player)
        explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.checkEditPermissions(player, block));
    } else if (entity instanceof WitherSkullEntity skull) { //Wither Skull explosions
      if (skull.getOwner() instanceof MobEntity thrower) {
        if (thrower.getTarget() instanceof ServerPlayerEntity target)
          explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.checkEditPermissions(target, block));
      }
    }

    explosion.affectWorld(false);
    if (destructionType == Explosion.DestructionType.NONE) {
      explosion.clearAffectedBlocks();
    }

    for (ServerPlayerEntity serverPlayerEntity : instance.getPlayers()) {
      if (serverPlayerEntity.squaredDistanceTo(x, y, z) < 4096.0D) {
        serverPlayerEntity.networkHandler.sendPacket(new ExplosionS2CPacket(x, y, z, power, explosion.getAffectedBlocks(), explosion.getAffectedPlayers().get(serverPlayerEntity)));
      }
    }

    return explosion;

  }
}
