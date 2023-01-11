package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.actionToEffect
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte

data class ActionDefinition(
    val id: String,

    override val bundle: String,

    val project: Boolean,

    val actionText: String,
) : Definition {
  init {
    require(id.isNotEmpty())
    require(bundle.isNotEmpty())
  }

  override val className = englishHack(id)

  val action by lazy { parsePets<Action>(actionText) }

  override val asClassDeclaration by lazy {
    ClassDeclaration(
        className = className,
        abstract = false,
        supertypes =  setOf(gte(if (project) "StandardProject" else "StandardAction")),
        otherInvariants = invariants,
        effectsRaw = setOf(actionToEffect(action, 1)),
    )
  }
}

private val invariants = setOf(parsePets<Requirement>("=1 This"))
