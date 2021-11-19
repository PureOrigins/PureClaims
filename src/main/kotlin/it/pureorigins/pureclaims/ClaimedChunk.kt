package it.pureorigins.pureclaims

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.WorldChunk
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val world: String,
    val chunkPos: ChunkPos
) {
    constructor(owner: PlayerEntity, chunkPos: ChunkPos) : this(
        owner.uuid,
        owner.world.registryKey.value.toString(),
        chunkPos
    )

    fun getChunk(): WorldChunk? = getWorld()?.getChunk(chunkPos.x, chunkPos.z)
    fun getWorld(): ServerWorld? = PureClaims.getWorld(world)

}