package it.pureorigins.pureclaims

import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val world: Identifier,
    val chunkPos: ChunkPos
)
