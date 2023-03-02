package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_ACTION
import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_PROJECT
import dev.martianzoo.tfm.pets.AstTransforms.actionListToEffects
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr.Companion.scaledType

data class StandardActionDefinition(
    override val id: ClassName,
    override val bundle: String,
    val project: Boolean,
    val action: Action,
) : Definition {
  init {
    require(bundle.isNotEmpty())
  }

  override val className = englishHack(id)

  override val asClassDeclaration by lazy {
    val kind = if (project) STANDARD_PROJECT else STANDARD_ACTION
    ClassDeclaration(
        className = className,
        id = id,
        abstract = false,
        supertypes = setOf(kind.type),
        otherInvariants = setOf(invariant),
        effectsRaw = actionListToEffects(listOf(action)),
    )
  }
}

private val invariant = Exact(scaledType(1, THIS.type))
