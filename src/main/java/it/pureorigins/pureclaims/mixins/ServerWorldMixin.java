package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
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
    ServerWorld instance = (ServerWorld) (Object) this;
    Explosion explosion = new Explosion(instance, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
    explosion.collectBlocksAndDamageEntities();

    LivingEntity causingEntity = explosion.getCausingEntity();
    ServerPlayerEntity causingPlayer = null;

    if (causingEntity instanceof ServerPlayerEntity player) causingPlayer = player;
    else if (causingEntity instanceof MobEntity mob)
      if (mob.getTarget() instanceof ServerPlayerEntity player) causingPlayer = player;

    if (causingPlayer != null) {
      ServerPlayerEntity finalCausingPlayer = causingPlayer;
      explosion.getAffectedBlocks().removeIf(block -> !PureClaims.INSTANCE.checkEditPermissions(finalCausingPlayer, block));
    } else explosion.getAffectedBlocks().removeIf(blockPos -> PureClaims.INSTANCE.isClaimed(instance, new ChunkPos(blockPos)));

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
