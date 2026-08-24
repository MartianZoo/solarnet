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
    @SerialName("metric") val metricText: String,
    @SerialName("automaticSelectionRequirement")
    private val automaticSelectionRequirementText: String? = null,
    override val selectionGroup: ClassName? = null,
) : Definition {

  init {
    require(metricText.isNotEmpty())
    require(automaticSelectionRequirementText?.isNotBlank() != false)
  }

  override val automaticSelectionRequirement: Requirement? =
      Requirement.join(MULTIPLAYER_ONLY, automaticSelectionRequirementText?.let(::parse))

  override val compatibilityRequirement: Requirement = MULTIPLAYER_ONLY

  public val metric: Metric = parse(metricText)

  private val executableMetric: Metric = FollowModeNeutralizer.transformMetric(metric)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        kind = CONCRETE,
        supertypes = setOf(AWARD.expression),
        properties = mapOf(METRIC_PROPERTY to MetricValue(executableMetric)),
    )
  }

  public companion object {
    private val MULTIPLAYER_ONLY: Requirement = parse("MultiplayerMode")
    private val METRIC_PROPERTY = PropertyName("metric")

    /** Derives award metadata; direct invariants also gate automatic pool selection. */
    public fun fromClassDeclaration(
        declaration: ClassDeclaration,
        selectionGroup: ClassName? = null,
    ): AwardDefinition {
      val metric =
          (declaration.properties[METRIC_PROPERTY] as? MetricValue)?.value
              ?: error("Award ${declaration.className} must declare a metric property")
      val automatic =
          declaration.invariants
              .fold<Requirement, Requirement?>(null, Requirement::join)
              ?.toString()
      return AwardDefinition(
          declaration.className,
          metricText = metric.toString(),
          automaticSelectionRequirementText = automatic,
          selectionGroup = selectionGroup,
      )
    }
  }
}
