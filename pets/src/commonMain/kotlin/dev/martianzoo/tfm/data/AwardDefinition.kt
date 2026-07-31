package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.TfmClasses.AWARD
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class AwardDefinition(
    val id: String,
    @SerialName("metric") val metricText: String,
    @SerialName("setupRequirement") private val setupRequirementText: String? = null,
) : Definition {

  init {
    require(metricText.isNotEmpty())
    require(setupRequirementText?.isNotBlank() != false)
  }

  @Transient override val shortName: ClassName = cn(id)

  @Transient override val setupRequirement: Requirement? = setupRequirementText?.let(::parse)

  @Transient public val metric: Metric = parse(metricText)

  @Transient override val className: ClassName = englishHack(id)

  internal fun withSetupRequirement(setupRequirement: String): AwardDefinition {
    require(setupRequirementText == null) { "$id has both file and definition setup requirements" }
    return copy(setupRequirementText = setupRequirement)
  }

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        shortName,
        kind = CONCRETE,
        supertypes = setOf(AWARD.expression),
        effects =
            listOf(
                parse<Effect>(
                    "MeasureAward<Owner>:: " +
                        "(AwardTally<Owner, This> / ($metricText)) " +
                        "THEN AwardMeasured<Owner, This>"
                ),
            ),
    )
  }
}
