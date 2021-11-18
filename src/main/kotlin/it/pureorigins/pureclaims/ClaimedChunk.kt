package it.pureorigins.pureclaims

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.WorldChunk
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val world: String,
    val chunkPos: ChunkPos
) {

    fun getChunk(): WorldChunk? = getWorld()?.getChunk(chunkPos.x, chunkPos.z)
    fun getWorld(): ServerWorld? = PureClaims.getWorld(world)

}