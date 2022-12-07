package dev.martianzoo.tfm.petaform

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
    val min: Int = 0,
    val max: Int? = null,
    val complete: Boolean = true,
) : PetaformNode() {
  init {
    require(min >= 0)
    require(max == null || max >= min)
  }
  override val children = supertypes + actions + effects + expression
}
