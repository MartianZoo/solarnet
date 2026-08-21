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
    val replaces: ClassName? = null,
    @SerialName("requirement") val requirementText: String,
    @SerialName("setupRequirement") private val setupRequirementText: String? = null,
) : Definition {

  init {
    require(requirementText.isNotEmpty())
    require(setupRequirementText?.isNotBlank() != false)
  }

  override val setupRequirement: Requirement? = setupRequirementText?.let(::parse)

  public val requirement: Requirement = parse(requirementText)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        kind = CONCRETE,
        supertypes = setOf(MILESTONE.expression),
        properties = mapOf(REQUIREMENT_PROPERTY to RequirementValue(requirement)),
    )
  }

  private companion object {
    private val REQUIREMENT_PROPERTY = PropertyName("requirement")
  }
}
