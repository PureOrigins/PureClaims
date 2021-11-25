package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimPermissions;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PressurePlateBlock.class)
public abstract class PressurePlateBlockMixin extends AbstractPressurePlateBlock {
  @Shadow @Final private PressurePlateBlock.ActivationRule type;
  
  protected PressurePlateBlockMixin(Settings settings) {
    super(settings);
  }
  
  /**
   * @author AgeOfWar
   * @reason Hard to inject
   */
  @Overwrite
  public int getRedstoneOutput(World world, BlockPos pos) {
    var box = AbstractPressurePlateBlock.BOX.offset(pos);
    var entities = switch (type) {
      case EVERYTHING -> world.getOtherEntities(null, box, EntityPredicates.EXCEPT_SPECTATOR.and(e -> accept(e, pos)));
      case MOBS -> world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.EXCEPT_SPECTATOR.and(e -> accept(e, pos)));
    };
  
    for (var entity : entities) {
      if (!entity.canAvoidTraps() && accept(entity, pos)) {
        return 15;
      }
    }
    return 0;
  }
  
  private boolean accept(Entity entity, BlockPos pos) {
    return PureClaims.INSTANCE.checkIndirectPermissions(entity, pos, ClaimPermissions.INTERACT);
  }
}
