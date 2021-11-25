package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimPermissions;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
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
   * @reason Hard to inject
   */
  @Overwrite
  public Explosion createExplosion(Entity entity, DamageSource damageSource, ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType) {
    var instance = (ServerWorld) (Object) this;
    var explosion = new Explosion(instance, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
    explosion.collectBlocksAndDamageEntities();
  
    if (entity != null) {
      var causingPlayer = PureClaims.INSTANCE.inferPlayer(entity);
      if (causingPlayer != null) {
        explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.hasPermissions(causingPlayer, block, ClaimPermissions.EDIT));
      }
    } else explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.isClaimed(instance, block));

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
