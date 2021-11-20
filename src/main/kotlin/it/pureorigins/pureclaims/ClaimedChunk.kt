package it.pureorigins.pureclaims

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val worldId: Identifier,
    val chunkPos: ChunkPos
) {
    constructor(player:ServerPlayerEntity, chunkPos: ChunkPos) : this(
        player.uuid,
        player.world.registryKey.value,
        chunkPos
    )
}
