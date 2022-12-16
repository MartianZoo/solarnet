package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.data.Definition

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY.
 */
data class ComponentDef( // TODO not sure abt data class after complete is gone
    val name: String,
    val abstract: Boolean = false,
    val supertypes: Set<TypeExpression> = setOf(),
    val dependencies: List<TypeExpression> = listOf(),
    val effects: Set<Effect> = setOf(),
    val defaults: Set<Instruction> = setOf(),
) : PetsNode(), Definition {
  init {
    if (name == rootName) {
      require(supertypes.isEmpty())
      require(dependencies.isEmpty())
    }
  }
  // TODO: this should really enforce rules
  override val children = supertypes + dependencies + effects + defaults

  override val toComponentDef = this
}
