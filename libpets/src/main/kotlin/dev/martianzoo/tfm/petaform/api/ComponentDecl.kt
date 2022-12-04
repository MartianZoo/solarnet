package dev.martianzoo.tfm.petaform.api

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided.
 */
data class ComponentDecl(
    val expression: Expression,
    val abstract: Boolean = false,
    val supertypes: Set<Expression> = setOf(),
    val actions: Set<Action> = setOf(),
    val effects: Set<Effect> = setOf(),
    val defaults: Set<Instruction> = setOf(),
    val complete: Boolean = true,
) : PetaformNode() {
  override val hasProd = (actions + effects).any { it.hasProd }
  override val children = supertypes + actions + effects + expression
}

data class ComponentDecls(val decls: Set<ComponentDecl> = setOf()) : PetaformNode() {
  override val hasProd = decls.any { it.hasProd }
  override val children = decls
}
