package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_ACTION
import dev.martianzoo.tfm.data.SpecialClassNames.STANDARD_PROJECT
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.PureTransformers.actionListToEffects
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.raw
import dev.martianzoo.util.toSetStrict

data class StandardActionDefinition(
    override val shortName: ClassName,
    override val bundle: String,
    val project: Boolean,
    val action: String,
) : Definition {
  init {
    require(bundle.isNotEmpty())
  }

  override val className = englishHack(shortName.toString())

  override val asClassDeclaration by lazy {
    val kind = if (project) STANDARD_PROJECT else STANDARD_ACTION
    ClassDeclaration(
        className = className,
        shortName = shortName,
        abstract = false,
        supertypes = setOf(kind.expr),
        effectsIn = actionListToEffects(listOf(parseAsIs(action))).toSetStrict { it.raw() },
    )
  }
}
