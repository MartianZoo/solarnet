package dev.martianzoo.tfm.data

import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.TfmClasses.MILESTONE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class MilestoneDefinition(
    val id: String,
    val replaces: String? = null,
    @SerialName("requirement") val requirementText: String,
    private val requiredBundles: String? = null,
) : Definition {

  init {
    require(requirementText.isNotEmpty())
    require(replaces?.isNotEmpty() != false)
    require(requiredBundles?.isNotBlank() != false)
  }

  @Transient override val shortName: ClassName = cn(id)

  @Transient public val requiredBundleNames: Set<ClassName> = parseBundleNames(requiredBundles)

  @Transient val requirement: Requirement = parse(requirementText)

  @Transient override val className: ClassName = englishHack(id)

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
