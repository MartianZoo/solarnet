package dev.martianzoo.tfm.data

import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.TfmClasses.MILESTONE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MilestoneDefinition(
    val id: String,
    override val bundle: String,
    val replaces: String? = null,
    @SerialName("requirement") val requirementText: String,
    @SerialName("loadRequirement") val loadRequirementText: String? = null,
) : Definition {

  init {
    require(bundle.isNotEmpty())
    require(requirementText.isNotEmpty())
    require(replaces?.isEmpty() != true)
  }

  @Transient override val shortName = cn(id)

  @Transient override val definitionId: String = id

  @Transient override val replacesId: String? = replaces

  @Transient
  override val loadRequirement: Requirement? =
      loadRequirementText?.removePrefix("HAS ")?.let(::parse)

  @Transient val requirement: Requirement = parse(requirementText)

  @Transient override val className = englishHack(id)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        shortName,
        kind = CONCRETE,
        supertypes = setOf(MILESTONE.expression),
        effects = listOf(parse("This:: ($requirementText: $OK)")),
    )
  }
}
