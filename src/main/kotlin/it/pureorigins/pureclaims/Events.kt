package it.pureorigins.pureclaims

object Events {
    fun registerEvents() {
        /*
        PlayerBlockBreakEvents.BEFORE.register { world, player, blockPos, _, _ ->
            val pos = ChunkPos(blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]?.get(pos)
                if (claimedChunk != null)
                    if (claimedChunk.owner == player.uuid)
                        return@register true
                    else if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canEdit == true)
                        return@register true
                player.sendMessage(Text.of(PureClaims.settings.no_perm),false)
                return@register false
            }
            return@register true
        }

        AttackEntityCallback.EVENT.register { player, world, _, entity, _ ->
            val pos = ChunkPos(entity?.blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]!![pos]
                if (claimedChunk != null)
                    if (claimedChunk.owner == player?.uuid)
                        return@register ActionResult.PASS
                    else if (permsCache[claimedChunk.owner]?.get(player?.uuid)?.canDamageMobs == true)
                        return@register ActionResult.PASS
                player.sendMessage(Text.of(PureClaims.settings.no_perm),false)
                return@register ActionResult.FAIL
            }
            return@register ActionResult.PASS
        }


        UseBlockCallback.EVENT.register { player, world, _, block ->
            val pos = ChunkPos(block.blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]!![pos]
                if (claimedChunk != null)
                    when {
                        claimedChunk.owner == player.uuid -> return@register ActionResult.PASS
                        permsCache[claimedChunk.owner]?.get(player.uuid)?.canInteract == true -> return@register ActionResult.PASS
                        else -> {
                            val blockState = world.getBlockState(block.blockPos)
                            if (blockState.block == Blocks.CHEST || blockState.block == Blocks.TRAPPED_CHEST) {
                                if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canOpenChests == true)
                                    return@register ActionResult.PASS
                            }
                        }
                    }
                player.sendMessage(Text.of(PureClaims.settings.no_perm),false)
                return@register ActionResult.FAIL
            }
            return@register ActionResult.PASS
        }
        */
    }
}