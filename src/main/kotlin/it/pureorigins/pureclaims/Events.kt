package it.pureorigins.pureclaims

object Events {
    fun registerEvents() {
        /*PlayerBlockBreakEvents.BEFORE.register { _, player, blockPos, _, _ ->
            val pos = ChunkPos(blockPos)
            return@register PureClaims.checkPermissions(player, pos, ClaimPermissions::canEdit)
        }

        AttackEntityCallback.EVENT.register { player, _, _, entity, _ ->
            val pos = ChunkPos(entity?.blockPos)
            if (PureClaims.checkPermissions(player, pos, ClaimPermissions::canDamageMobs))
                return@register ActionResult.FAIL
            else return@register ActionResult.PASS
        }


        UseBlockCallback.EVENT.register { player, world, _, block ->
            val pos = ChunkPos(block.blockPos)
            val blockState = world.getBlockState(block.blockPos)
            if (blockState.block == Blocks.CHEST || blockState.block == Blocks.TRAPPED_CHEST) {
                if (PureClaims.checkPermissions(player, pos, ClaimPermissions::canOpenChests))
                    return@register ActionResult.PASS
            }
            if (PureClaims.checkPermissions(player, pos, ClaimPermissions::canInteract))
                return@register ActionResult.PASS
            return@register ActionResult.FAIL
        }*/
    }
}