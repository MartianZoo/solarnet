package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.TfmClasses.MILESTONE
import kotlinx.serialization.SerialName

public data class MilestoneDefinition(
    override val className: ClassName,
    @SerialName("requirement") val requirementText: String,
    @SerialName("automaticSelectionRequirement")
    private val automaticSelectionRequirementText: String? = null,
    override val selectionGroup: ClassName? = null,
) : Definition {

  init {
    require(requirementText.isNotEmpty())
    require(automaticSelectionRequirementText?.isNotBlank() != false)
  }

  override val automaticSelectionRequirement: Requirement? =
      automaticSelectionRequirementText?.let(::parse)

  public val requirement: Requirement = parse(requirementText)

  private val executableRequirement: Requirement =
      FollowModeNeutralizer.transformRequirement(requirement)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        kind = CONCRETE,
        supertypes = setOf(MILESTONE.expression),
        properties = mapOf(REQUIREMENT_PROPERTY to RequirementValue(executableRequirement)),
    )
  }

  public companion object {
    private val REQUIREMENT_PROPERTY = PropertyName("requirement")

    /** Derives milestone metadata; direct invariants also gate automatic pool selection. */
    public fun fromClassDeclaration(
        declaration: ClassDeclaration,
        selectionGroup: ClassName? = null,
    ): MilestoneDefinition {
      val requirement =
          (declaration.properties[REQUIREMENT_PROPERTY] as? RequirementValue)?.value
              ?: error("Milestone ${declaration.className} must declare a requirement property")
      val automatic =
          declaration.invariants
              .fold<Requirement, Requirement?>(null, Requirement::join)
              ?.toString()
      return MilestoneDefinition(
          declaration.className,
          requirementText = requirement.toString(),
          automaticSelectionRequirementText = automatic,
          selectionGroup = selectionGroup,
      )
    }
  }
}
