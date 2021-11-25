package it.pureorigins.pureclaims.mixins;

import it.pureorigins.pureclaims.ClaimPermissions;
import it.pureorigins.pureclaims.PureClaims;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.WeightedPressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WeightedPressurePlateBlock.class)
public abstract class WeightedPressurePlateBlockMixin extends AbstractPressurePlateBlock {
  @Shadow @Final private int weight;
  
  protected WeightedPressurePlateBlockMixin(Settings settings) {
    super(settings);
  }
  
  /**
   * @author AgeOfWar
   * @reason Too hard to inject
   */
  @Overwrite
  public int getRedstoneOutput(World world, BlockPos pos) {
    var i = world.getEntitiesByClass(Entity.class, BOX.offset(pos), EntityPredicates.EXCEPT_SPECTATOR.and(e -> accept(e, pos))).size();
    if (i > 0) {
      float f = (float) Math.min(weight, i) / (float) weight;
      return MathHelper.ceil(f * 15.0F);
    } else {
      return 0;
    }
  }
  
  private boolean accept(Entity entity, BlockPos pos) {
    return PureClaims.INSTANCE.checkIndirectPermissions(entity, pos, ClaimPermissions.INTERACT);
  }
}
