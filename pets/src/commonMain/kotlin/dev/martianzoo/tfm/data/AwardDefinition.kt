package dev.martianzoo.tfm.data

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.TfmClasses.AWARD
import kotlinx.serialization.SerialName

public data class AwardDefinition(
    override val className: ClassName,
    val replaces: ClassName? = null,
    @SerialName("metric") val metricText: String,
    @SerialName("setupRequirement") private val setupRequirementText: String? = null,
) : Definition {

  init {
    require(metricText.isNotEmpty())
    require(setupRequirementText?.isNotBlank() != false)
  }

  override val setupRequirement: Requirement =
      parse(listOf(MULTIPLAYER_ONLY, setupRequirementText).filterNotNull().joinToString())

  public val metric: Metric = parse(metricText)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        kind = CONCRETE,
        supertypes = setOf(AWARD.expression),
        properties = mapOf(METRIC_PROPERTY to MetricValue(metric)),
    )
  }

  private companion object {
    private const val MULTIPLAYER_ONLY = "MAX 0 SoloMode"
    private val METRIC_PROPERTY = PropertyName("metric")
  }
}
