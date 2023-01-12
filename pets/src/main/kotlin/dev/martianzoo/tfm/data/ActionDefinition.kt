package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_ACTION
import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_PROJECT
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.actionToEffect
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement.Exact

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
    val kind = if (project) STANDARD_PROJECT else STANDARD_ACTION
    ClassDeclaration(
        className = className,
        abstract = false,
        supertypes =  setOf(kind.type),
        otherInvariants = setOf(invariant),
        effectsRaw = setOf(actionToEffect(action, 1)),
    )
  }
}

private val invariant = Exact(QuantifiedExpression(THIS.type, 1))
