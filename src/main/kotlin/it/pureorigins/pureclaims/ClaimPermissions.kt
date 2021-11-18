package it.pureorigins.pureclaims

data class ClaimPermissions(
    val canEdit: Boolean = false,
    val canInteract: Boolean = false,
    val canOpenChests: Boolean = false,
    val canDamageMobs: Boolean = false
) {
    init {
        if (canEdit) check(canOpenChests && canInteract)
        if (canInteract) check(canOpenChests)
    }

    fun withCanEdit(canEdit: Boolean) = copy(
        canEdit = canEdit,
        canInteract = canEdit,
        canOpenChests = canEdit
    )

    fun withCanInteract(canInteract: Boolean) = if (canInteract) copy(
        canInteract = canInteract,
        canOpenChests = canInteract
    ) else copy(
        canEdit = canInteract,
        canInteract = canInteract
    )

    fun withCanOpenChests(canOpenChests: Boolean) = copy(
        canOpenChests = canOpenChests
    )

    fun withCanDamageMobs(canDamageMobs: Boolean) = copy(
        canDamageMobs = canDamageMobs
    )

    companion object {
        fun all() = ClaimPermissions(
            canEdit = true,
            canInteract = true,
            canOpenChests = true,
            canDamageMobs = true
        )

        fun none() = ClaimPermissions()
    }
}