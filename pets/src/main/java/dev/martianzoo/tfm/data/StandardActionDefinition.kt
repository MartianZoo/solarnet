package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_ACTION
import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_PROJECT
import dev.martianzoo.tfm.pets.AstTransforms.actionToEffect
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action.Companion.action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat

data class StandardActionDefinition(
    override val id: ClassName,
    override val bundle: String,
    val project: Boolean,
    val actionText: String,
) : Definition {
  init {
    require(bundle.isNotEmpty())
  }

  override val name = englishHack(id)

  val action by lazy { action(actionText) }

  override val asClassDeclaration by lazy {
    val kind = if (project) STANDARD_PROJECT else STANDARD_ACTION
    ClassDeclaration(
        name = name,
        id = id,
        abstract = false,
        supertypes = setOf(kind.type),
        otherInvariants = setOf(invariant),
        effectsRaw = setOf(actionToEffect(action, 1)),
    )
  }
}

private val invariant = Exact(sat(1, THIS.type))
