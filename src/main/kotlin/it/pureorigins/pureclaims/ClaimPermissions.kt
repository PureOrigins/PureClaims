package it.pureorigins.pureclaims

data class ClaimPermissions(
    val canEdit: Boolean,
    val canInteract: Boolean,
    val canDamageMobs: Boolean
) {
    init {
        if (canEdit) check(canInteract)
    }
    
    fun withCanEdit(canEdit: Boolean) = copy(
        canEdit = canEdit,
        canInteract = canEdit,
    )
    
    fun withCanInteract(canInteract: Boolean) = if (canInteract) copy(
        canInteract = canInteract,
    ) else copy(
        canEdit = canInteract,
        canInteract = canInteract
    )
    
    fun withCanDamageMobs(canDamageMobs: Boolean) = copy(
        canDamageMobs = canDamageMobs
    )
    
    companion object {
        val ALL = ClaimPermissions(
            canEdit = true,
            canInteract = true,
            canDamageMobs = true
        )
        
        val NONE = ClaimPermissions(
            canEdit = false,
            canInteract = false,
            canDamageMobs = false
        )
        
        @JvmField
        val EDIT = ClaimPermissions::canEdit
        @JvmField
        val INTERACT = ClaimPermissions::canInteract
        @JvmField
        val DAMAGE_MOBS = ClaimPermissions::canDamageMobs
    }
}