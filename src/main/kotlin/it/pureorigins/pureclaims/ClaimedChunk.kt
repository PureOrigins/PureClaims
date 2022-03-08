package it.pureorigins.pureclaims

import org.bukkit.Chunk
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val chunk: Chunk
)
