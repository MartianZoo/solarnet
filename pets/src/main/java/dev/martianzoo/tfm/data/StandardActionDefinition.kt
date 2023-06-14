package dev.martianzoo.tfm.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_ACTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_PROJECT
import dev.martianzoo.util.toSetStrict

data class StandardActionDefinition(
    override val shortName: ClassName,
    override val bundle: String,
    val project: Boolean,
    val actions: List<String>,
) : Definition {
  init {
    require(bundle.isNotEmpty())
  }

  override val className = englishHack(shortName.toString())

  override val asClassDeclaration by lazy {
    val kind = if (project) STANDARD_PROJECT else STANDARD_ACTION
    // TODO can share some of this across Definitions?
    ClassDeclaration(
        className = className,
        shortName = shortName,
        kind = CONCRETE,
        supertypes = setOf(kind.expression),
        effects = actionListToEffects(actions.map(::parse)).toSetStrict(),
    )
  }
}
