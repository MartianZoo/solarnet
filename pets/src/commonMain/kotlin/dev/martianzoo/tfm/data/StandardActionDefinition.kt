package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_ACTION

public data class StandardActionDefinition(
    override val className: ClassName,
    val actions: List<String>,
    private val setupRequirementText: String? = null,
    val effects: List<String> = emptyList(),
) : Definition {
  init {
    require(setupRequirementText?.isNotBlank() != false)
  }

  override val setupRequirement: Requirement? = setupRequirementText?.let(::parse)

  override val asClassDeclaration: ClassDeclaration by lazy {
    // TODO can share some of this across Definitions?
    ClassDeclaration(
        className = className,
        kind = CONCRETE,
        supertypes = setOf(STANDARD_ACTION.expression),
        effects = actionListToEffects(actions.map(::parse)) + effects.map(::parse),
    )
  }
}
