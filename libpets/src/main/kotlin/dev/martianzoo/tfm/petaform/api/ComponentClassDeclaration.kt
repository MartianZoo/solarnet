package dev.martianzoo.tfm.petaform.api

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided.
 */
data class ComponentClassDeclaration(
    val expression: Expression,
    val abstract: Boolean = false,
    val supertypes: Set<Expression> = setOf(),
    val actions: Set<Action> = setOf(),
    val effects: Set<Effect> = setOf(),
    val defaults: Set<Instruction> = setOf(),
    val complete: Boolean = true,
) : PetaformNode() {
  override val children = supertypes + actions + effects + expression
}

data class ComponentDecls(val decls: Set<ComponentClassDeclaration> = setOf()) : PetaformNode() {
  override val children = decls
}
