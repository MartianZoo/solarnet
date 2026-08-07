package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_ACTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_PROJECT

public data class StandardActionDefinition(
    override val className: ClassName,
    val project: Boolean,
    val actions: List<String>,
    private val setupRequirementText: String? = null,
) : Definition {
  init {
    require(setupRequirementText?.isNotBlank() != false)
  }

  override val shortName: ClassName by ::className

  override val setupRequirement: Requirement? = setupRequirementText?.let(::parse)

  override val asClassDeclaration: ClassDeclaration by lazy {
    val kind = if (project) STANDARD_PROJECT else STANDARD_ACTION
    // TODO can share some of this across Definitions?
    ClassDeclaration(
        className = className,
        shortName = shortName,
        kind = CONCRETE,
        supertypes = setOf(kind.expression),
        effects = actionListToEffects(actions.map(::parse)),
    )
  }
}
