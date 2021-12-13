package it.pureorigins.pureclaims

import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.block.CakeBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.enums.DoubleBlockHalf
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.TallBlockItem
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

object Events {
  fun registerEvents() {
    AttackEntityCallback.EVENT.register { player, _, _, entity, _ ->
      if (entity is LivingEntity && entity !is ArmorStandEntity) {
        PureClaims.checkPermissions(player, entity.blockPos, ClaimPermissions.DAMAGE_MOBS)
      } else {
        PureClaims.checkPermissions(player, entity.blockPos, ClaimPermissions.EDIT)
      }.toActionResult()
    }
    UseEntityCallback.EVENT.register { player, _, _, entity, _ ->
      PureClaims.checkPermissions(player, entity.blockPos, ClaimPermissions.INTERACT).toActionResult()
    }
    UseBlockCallback.EVENT.register { player, world, hand, hit ->
      if (player !is ServerPlayerEntity) return@register ActionResult.PASS
      if (PureClaims.checkPermissions(player, hit.blockPos, ClaimPermissions.INTERACT)) {
        ActionResult.PASS
      } else {
        val blockState = world.getBlockState(hit.blockPos)
        when (blockState.block) {
          is DoorBlock -> if (blockState[DoorBlock.HALF] == DoubleBlockHalf.LOWER) {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(world, hit.blockPos.up()))
          } else {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(world, hit.blockPos.down()))
          }
          is CakeBlock -> player.networkHandler.sendPacket(BlockUpdateS2CPacket(world, hit.blockPos))
        }
        when (player.getStackInHand(hand).item) {
          is TallBlockItem -> player.networkHandler.sendPacket(
            BlockUpdateS2CPacket(
              world,
              hit.blockPos.offset(hit.side).up()
            )
          )
        }
        ActionResult.FAIL
      }
    }
  }

  private fun Boolean.toActionResult() = if (this) ActionResult.PASS else ActionResult.FAIL
}