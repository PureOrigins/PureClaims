package it.pureorigins.pureclaims

import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val world: World,
    val chunkPos: ChunkPos
)
