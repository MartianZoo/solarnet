package dev.martianzoo.tools

import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.SystemClasses.USE_ACTION
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.types.Class as PetsClass
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.TfmClasses.PROD
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.canon.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.tfm.canon.cardRequirement

/**
 * Conservative static report of rules that may make a solo player's resource quantity nonmonotonic.
 */
internal object StandardResourceMonotonicityReport {
  private enum class QuantityKind {
    RESOURCE,
    PRODUCTION,
  }

  private data class Quantity(
      val label: String,
      val resourceClass: PetsClass,
      val kind: QuantityKind,
  )

  data class Finding(
      val quantity: String,
      val subject: String,
      val subjectClass: String,
      val location: RuleLocation,
      val kind: String,
      val evidence: String,
  )

  data class RuleLocation(
      val kind: RuleLocationKind,
      val index: Int? = null,
  ) {
    override fun toString(): String = kind.label + if (index == null) "" else " $index"
  }

  enum class RuleLocationKind(val label: String) {
    PLAY_REQUIREMENT("play requirement"),
    INVARIANT("invariant"),
    ACTION("action"),
    EFFECT("effect"),
    DECLARATION("declaration"),
    AWARD_METRIC("award metric"),
  }

  data class OpaqueUsage(
      val subject: String,
      val subjectClass: String,
      val kind: String,
      val evidence: String,
  )

  data class Analysis(
      val quantities: List<String>,
      val findings: List<Finding>,
      val opaqueUsages: List<OpaqueUsage>,
  )

  fun analyze(premise: GamePremise = maximalSoloPremise()): Analysis {
    val table = ClassTable.forPremise(premise)
    val productionLowerer = table.transformDispatcher(setOf(PROD))
    val quantities = quantities(table)
    val findings = linkedSetOf<Finding>()
    val opaqueUsages = linkedSetOf<OpaqueUsage>()
    val tfmCatalog = premise.catalog as TfmCatalog
    val playRequirements =
        tfmCatalog.cards.associate { card -> card.className to cardRequirement(card) }

    table.allClasses().sortedBy(PetsClass::className).forEach { subjectClass ->
      val declaration = subjectClass.declaration
      val subjectName = displayName(premise, declaration.className)
      val playRequirement = playRequirements[declaration.className]

      quantities
          .filter { it.kind == QuantityKind.RESOURCE }
          .forEach { quantity ->
            if (quantity.resourceClass.isSubtypeOf(subjectClass)) {
              declaration.effects.forEachIndexed { index, effect ->
                findings +=
                    finding(
                        quantity,
                        subjectName,
                        subjectClass,
                        effectLocation(effect, index + 1),
                        "resource instances carry a live effect",
                        effect.toString(),
                    )
              }
            }
          }

      declaration.allNodes.forEach { authoredRoot ->
        val location = ruleLocation(authoredRoot, declaration, playRequirement)
        val root =
            if (authoredRoot is PetElement) {
              productionLowerer.transformElement(authoredRoot)
            } else {
              authoredRoot
            }
        root.visitDescendants { node ->
          quantities.forEach { quantity ->
            inspectNode(
                    node,
                    quantity,
                    subjectClass,
                    subjectName,
                    location,
                    table,
                )
                ?.let(findings::add)
          }
          inspectOpaqueNode(node, subjectName, subjectClass, table)?.let(opaqueUsages::add)
          true
        }
      }
    }

    val awardClass = table.getClass(cn("Award"))
    table
        .allClasses()
        .filter { !it.abstract && it.isSubtypeOf(awardClass) }
        .forEach { subjectClass ->
          val subjectName = displayName(premise, subjectClass.className)
          val metric =
              productionLowerer.transformMetric(
                  (subjectClass.properties.getValue(AWARD_METRIC_PROPERTY) as MetricValue).value
              )
          quantities.forEach { quantity ->
            if (metricCouldCount(metric, quantity, subjectClass, table)) {
              findings +=
                  finding(
                      quantity,
                      subjectName,
                      subjectClass,
                      RuleLocation(RuleLocationKind.AWARD_METRIC),
                      "award metric",
                      metric.toString(),
                  )
            }
          }
          metric.visitDescendants { node ->
            inspectOpaqueNode(node, subjectName, subjectClass, table)?.let(opaqueUsages::add)
            true
          }
        }

    return Analysis(
        quantities = quantities.map(Quantity::label),
        findings =
            findings.sortedWith(
                compareBy(
                    Finding::quantity,
                    Finding::subject,
                    { it.location.kind },
                    { it.location.index },
                    Finding::kind,
                    Finding::evidence,
                )
            ),
        opaqueUsages =
            opaqueUsages.sortedWith(
                compareBy(OpaqueUsage::subject, OpaqueUsage::kind, OpaqueUsage::evidence)
            ),
    )
  }

  private val AWARD_METRIC_PROPERTY = PropertyName("metric")

  fun render(analysis: Analysis): String = buildString {
    appendLine("Solo resource and production monotonicity suspicion report")
    appendLine(
        "Scope: maximal supported TR63 solo premise; resource stocks and production are separate"
    )
    appendLine("Minimum requirements and protective removal triggers are not treated as hazards.")
    appendLine("A finding is a reason to investigate, not proof of nonmonotonicity.")

    analysis.quantities.forEach { quantity ->
      appendLine()
      val quantityFindings = analysis.findings.filter { it.quantity == quantity }
      appendLine("$quantity (${quantityFindings.size})")
      if (quantityFindings.isEmpty()) {
        appendLine("  no declarative suspicions found")
      } else {
        quantityFindings
            .groupBy { it.subject to it.subjectClass }
            .forEach { (subject, group) ->
              appendLine("  ${subject.first} [${subject.second}]")
              group.forEach { finding ->
                appendLine("    - ${finding.location}: ${finding.kind}: ${finding.evidence}")
              }
            }
      }
    }

    appendLine()
    appendLine("Opaque custom usages (${analysis.opaqueUsages.size})")
    appendLine("  These need contracts or simulation; they are not assigned to one quantity.")
    analysis.opaqueUsages
        .groupBy { it.subject to it.subjectClass }
        .forEach { (subject, group) ->
          appendLine("  ${subject.first} [${subject.second}]")
          group.forEach { usage -> appendLine("    - ${usage.kind}: ${usage.evidence}") }
        }
  }

  private fun quantities(table: ClassTable): List<Quantity> =
      table
          .allSubclasses(table.getClass(STANDARD_RESOURCE))
          .filterNot(PetsClass::abstract)
          .sortedBy(PetsClass::className)
          .flatMap { resourceClass ->
            val name = resourceClass.className.toString()
            listOf(
                Quantity(name, resourceClass, QuantityKind.RESOURCE),
                Quantity("$name production", resourceClass, QuantityKind.PRODUCTION),
            )
          }

  private fun inspectNode(
      node: PetNode,
      quantity: Quantity,
      subjectClass: PetsClass,
      subjectName: String,
      location: RuleLocation,
      table: ClassTable,
  ): Finding? =
      when (node) {
        is Requirement.Counting ->
            inspectRequirement(
                node,
                quantity,
                subjectClass,
                subjectName,
                location,
                table,
            )
        is Instruction.Per ->
            inspectPerInstruction(node, quantity, subjectClass, subjectName, location, table)
        is Instruction.Change ->
            inspectAmapTransmutation(node, quantity, subjectClass, subjectName, location, table)
        else -> null
      }

  private fun inspectRequirement(
      requirement: Requirement.Counting,
      quantity: Quantity,
      subjectClass: PetsClass,
      subjectName: String,
      location: RuleLocation,
      table: ClassTable,
  ): Finding? {
    val kind =
        requirementKind(requirement, location.kind == RuleLocationKind.INVARIANT) ?: return null
    if (!metricCouldCount(requirement.metric, quantity, subjectClass, table, requirement.target)) {
      return null
    }
    return finding(quantity, subjectName, subjectClass, location, kind, requirement.toString())
  }

  private fun inspectPerInstruction(
      instruction: Instruction.Per,
      quantity: Quantity,
      subjectClass: PetsClass,
      subjectName: String,
      location: RuleLocation,
      table: ClassTable,
  ): Finding? =
      if (metricCouldCount(instruction.metric, quantity, subjectClass, table)) {
        finding(
            quantity,
            subjectName,
            subjectClass,
            location,
            "count-scaled instruction",
            instruction.toString(),
        )
      } else {
        null
      }

  private fun inspectAmapTransmutation(
      instruction: Instruction.Change,
      quantity: Quantity,
      subjectClass: PetsClass,
      subjectName: String,
      location: RuleLocation,
      table: ClassTable,
  ): Finding? {
    if (instruction.intensity != AMAP) return null
    val removing = instruction.removing ?: return null
    val gaining = instruction.gaining ?: return null
    val sourceMatches = expressionCouldCount(removing, quantity, subjectClass, table)
    if (!sourceMatches) return null
    val sameDestination = expressionCouldCount(gaining, quantity, subjectClass, table)
    if (sameDestination) return null
    return finding(
        quantity,
        subjectName,
        subjectClass,
        location,
        "AMAP transmutation to another type",
        instruction.toString(),
    )
  }

  private fun requirementKind(
      requirement: Requirement.Counting,
      invariant: Boolean,
  ): String? =
      when {
        invariant && requirement !is Requirement.Min -> "class invariant"
        requirement is Requirement.Max -> "maximum requirement"
        requirement is Requirement.Exact -> "exact requirement"
        else -> null
      }

  private fun ruleLocation(
      root: PetNode,
      declaration: ClassDeclaration,
      playRequirement: Requirement?,
  ): RuleLocation {
    if (root == playRequirement) return RuleLocation(RuleLocationKind.PLAY_REQUIREMENT)
    if (root in declaration.invariants) return RuleLocation(RuleLocationKind.INVARIANT)
    val effectIndex = declaration.effects.indexOf(root)
    return if (effectIndex >= 0) {
      effectLocation(declaration.effects[effectIndex], effectIndex + 1)
    } else {
      RuleLocation(RuleLocationKind.DECLARATION)
    }
  }

  private fun effectLocation(effect: Effect, index: Int): RuleLocation {
    val action =
        (effect.trigger as? Effect.Trigger.OnGainOf)?.expression?.takeIf {
          it.className == USE_ACTION
        }
    val actionIndex =
        when (action?.arguments?.lastOrNull()?.className?.toString()) {
          "Action1" -> 1
          "Action2" -> 2
          "Action3" -> 3
          else -> null
        }
    return if (actionIndex == null) {
      RuleLocation(RuleLocationKind.EFFECT, index)
    } else {
      RuleLocation(RuleLocationKind.ACTION, actionIndex)
    }
  }

  private fun inspectOpaqueNode(
      node: PetNode,
      subjectName: String,
      subjectClass: PetsClass,
      table: ClassTable,
  ): OpaqueUsage? {
    val kind: String
    val evidence: String
    when (node) {
      is Metric.Count -> {
        val countedClass = table.findClass(node.expression.className) ?: return null
        if (!countedClass.declaration.custom) return null
        kind = "custom metric"
        evidence = node.toString()
      }
      is Instruction.Gain -> {
        val gainedClass = table.findClass(node.gaining.className) ?: return null
        if (!gainedClass.declaration.custom) return null
        kind = "custom instruction"
        evidence = node.toString()
      }
      else -> return null
    }
    return OpaqueUsage(subjectName, subjectClass.className.toString(), kind, evidence)
  }

  private fun metricCouldCount(
      metric: Metric,
      quantity: Quantity,
      subjectClass: PetsClass,
      table: ClassTable,
      upperBound: Int? = null,
  ): Boolean {
    if (upperBound == Int.MAX_VALUE) return false
    return when (metric) {
      is Metric.Eval -> true
      is Metric.Constant -> false
      is Metric.Count -> expressionCouldCount(metric.expression, quantity, subjectClass, table)
      is Property -> false
      is Metric.Scaled ->
          metricCouldCount(
              metric.inner,
              quantity,
              subjectClass,
              table,
              scaledUpperBound(upperBound, metric.unit),
          )
      is Metric.Max ->
          (maximumCanExceed(metric.maximum, upperBound) &&
              metricCouldCount(metric.inner, quantity, subjectClass, table, upperBound)) ||
              metricCouldCount(metric.maximum, quantity, subjectClass, table)
      is Metric.Subtract ->
          metricCouldCount(metric.minuend, quantity, subjectClass, table) ||
              metricCouldCount(metric.subtrahend, quantity, subjectClass, table)
      is Metric.Or -> metric.metrics.any { metricCouldCount(it, quantity, subjectClass, table) }
      is Metric.Transform ->
          metricCouldCount(metric.inner, quantity, subjectClass, table, upperBound)
    }
  }

  private fun metricCouldCountAsResource(
      metric: Metric,
      quantity: Quantity,
      subjectClass: PetsClass,
      table: ClassTable,
      upperBound: Int? = null,
  ): Boolean {
    if (upperBound == Int.MAX_VALUE) return false
    return when (metric) {
      is Metric.Eval -> true
      is Metric.Constant -> false
      is Metric.Count ->
          baseExpressionCouldCount(
              metric.expression,
              quantity.resourceClass,
              subjectClass,
              table,
          )
      is Property -> false
      is Metric.Scaled ->
          metricCouldCountAsResource(
              metric.inner,
              quantity,
              subjectClass,
              table,
              scaledUpperBound(upperBound, metric.unit),
          )
      is Metric.Max ->
          (maximumCanExceed(metric.maximum, upperBound) &&
              metricCouldCountAsResource(
                  metric.inner,
                  quantity,
                  subjectClass,
                  table,
                  upperBound,
              )) || metricCouldCountAsResource(metric.maximum, quantity, subjectClass, table)
      is Metric.Subtract ->
          metricCouldCountAsResource(metric.minuend, quantity, subjectClass, table) ||
              metricCouldCountAsResource(metric.subtrahend, quantity, subjectClass, table)
      is Metric.Or ->
          metric.metrics.any { metricCouldCountAsResource(it, quantity, subjectClass, table) }
      is Metric.Transform ->
          metricCouldCountAsResource(metric.inner, quantity, subjectClass, table, upperBound)
    }
  }

  private fun scaledUpperBound(upperBound: Int?, unit: Int): Int? = upperBound?.let {
    (((it.toLong() + 1) * unit) - 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
  }

  private fun maximumCanExceed(maximum: Metric, upperBound: Int?): Boolean {
    if (upperBound == null) return true
    val constant = maximum as? Metric.Constant ?: return true
    return constant.value > upperBound
  }

  private fun expressionCouldCount(
      expression: Expression,
      quantity: Quantity,
      subjectClass: PetsClass,
      table: ClassTable,
  ): Boolean =
      when (quantity.kind) {
        QuantityKind.RESOURCE ->
            baseExpressionCouldCount(expression, quantity.resourceClass, subjectClass, table)
        QuantityKind.PRODUCTION ->
            productionExpressionCouldCount(expression, quantity.resourceClass, subjectClass, table)
      }

  private fun baseExpressionCouldCount(
      expression: Expression,
      resourceClass: PetsClass,
      subjectClass: PetsClass,
      table: ClassTable,
  ): Boolean {
    if (expression.className == CLASS) {
      val refinement = expression.refinement ?: return false
      return refinement.requirement.descendantsOfType<Requirement.Counting>().any {
        metricCouldCountAsResource(
            it.metric,
            Quantity(resourceClass.className.toString(), resourceClass, QuantityKind.RESOURCE),
            subjectClass,
            table,
        )
      }
    }
    if (expression.className == THIS) return resourceClass.isSubtypeOf(subjectClass)
    val countedClass = table.findClass(expression.className) ?: return false
    return resourceClass.isSubtypeOf(countedClass)
  }

  private fun productionExpressionCouldCount(
      expression: Expression,
      resourceClass: PetsClass,
      subjectClass: PetsClass,
      table: ClassTable,
  ): Boolean {
    if (expression.className != PRODUCTION) return false
    val resourceConstraint =
        expression.arguments
            .filter { it.className == CLASS }
            .flatMap(Expression::arguments)
            .singleOrNull() ?: return true
    return baseExpressionCouldCount(resourceConstraint, resourceClass, subjectClass, table)
  }

  private fun finding(
      quantity: Quantity,
      subjectName: String,
      subjectClass: PetsClass,
      location: RuleLocation,
      kind: String,
      evidence: String,
  ): Finding =
      Finding(
          quantity.label,
          subjectName,
          subjectClass.className.toString(),
          location,
          kind,
          evidence,
      )

  private fun displayName(premise: GamePremise, className: ClassName): String =
      premise.catalog.displayNamesByLanguage["en"]?.get(className)
          ?: defaultEnglishDisplayName(className)

  fun maximalSoloPremise(): GamePremise =
      Canon.gamePremise(
          GameConfig.create(
              included =
                  listOf(
                          "Tr63SoloVariant",
                          "VenusNextExpansion",
                          "PreludeExpansion",
                          "ColoniesExpansion",
                          "TurmoilCardPack",
                          "PromoCardPack",
                          "Callisto",
                          "Ceres",
                          "Luna",
                          "Triton",
                      )
                      .map(::cn),
              playerNames = listOf(cn("SoloPlayer")),
          )
      )
}

public fun main(): Unit =
    println(StandardResourceMonotonicityReport.render(StandardResourceMonotonicityReport.analyze()))
