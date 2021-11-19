package it.pureorigins.pureclaims

data class ClaimPermissions(
    val canEdit: Boolean,
    val canInteract: Boolean,
    val canOpenChests: Boolean,
    val canDamageMobs: Boolean
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
        val ALL = ClaimPermissions(
            canEdit = true,
            canInteract = true,
            canOpenChests = true,
            canDamageMobs = true
        )

        val NONE = ClaimPermissions(
            canEdit = false,
            canInteract = false,
            canOpenChests = false,
            canDamageMobs = false
        )
    }
}