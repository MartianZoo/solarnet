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
import dev.martianzoo.tfm.data.TfmClasses.AWARD
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public data class AwardDefinition(
    val id: String,
    val replaces: String? = null,
    @SerialName("metric") val metricText: String,
    @SerialName("setupRequirement") private val setupRequirementText: String? = null,
) : Definition {

  init {
    require(metricText.isNotEmpty())
    require(replaces?.isNotEmpty() != false)
    require(setupRequirementText?.isNotBlank() != false)
  }

  @Transient
  override val setupRequirement: Requirement =
      parse(listOf(MULTIPLAYER_ONLY, setupRequirementText).filterNotNull().joinToString())

  @Transient public val metric: Metric = parse(metricText)

  @Transient override val className: ClassName = cn("Award$id")

  internal fun withSetupRequirement(setupRequirement: String): AwardDefinition {
    return copy(
        setupRequirementText =
            listOfNotNull(setupRequirement, setupRequirementText).joinToString().ifEmpty { null }
    )
  }

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        kind = CONCRETE,
        supertypes = setOf(AWARD.expression),
        effects =
            listOf(parse<Effect>("EndPhase:: MeasureAward<This> THEN AssignAwardPlaces<This>")),
    )
  }

  private companion object {
    private const val MULTIPLAYER_ONLY = "MAX 0 SoloMode"
  }
}
