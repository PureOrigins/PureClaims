package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

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
    List<? extends Entity> entities;
    switch(this.type) {
      case EVERYTHING:
        entities = world.getOtherEntities(null, box);
        break;
      case MOBS:
        entities = world.getNonSpectatingEntities(LivingEntity.class, box);
        break;
      default:
        return 0;
    }
  
    for (var entity : entities) {
      if (!entity.canAvoidTraps() && (!(entity instanceof ServerPlayerEntity) || PureClaims.INSTANCE.checkInteractPermissions((ServerPlayerEntity) entity, pos))) {
        return 15;
      }
    }
    return 0;
  }
}
