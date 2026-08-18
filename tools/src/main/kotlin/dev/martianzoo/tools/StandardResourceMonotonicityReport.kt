package dev.martianzoo.tools

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.SystemClasses.USE_ACTION
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.AwardDefinition
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.tfm.engine.Prod
import dev.martianzoo.types.Class as PetsClass
import dev.martianzoo.types.ClassTable

/**
 * Conservative static report of rules that may make a solo player's resource quantity nonmonotonic.
 */
internal object StandardResourceMonotonicityReport {
  enum class QuantityKind {
    RESOURCE,
    PRODUCTION,
  }

  data class Quantity(
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
    val deprodifier = Prod.deprodify(table)
    val quantities = quantities(table)
    val findings = linkedSetOf<Finding>()
    val opaqueUsages = linkedSetOf<OpaqueUsage>()
    val playRequirements =
        premise.authority.allDefinitions.filterIsInstance<CardDefinition>().associate {
          it.className to it.requirement
        }

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
              deprodifier.transformElement(authoredRoot)
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

    premise.authority.allDefinitions
        .filterIsInstance<AwardDefinition>()
        .filter { table.isActive(it.className) }
        .forEach { award ->
          val subjectName = displayName(premise, award.className)
          val subjectClass = table.getClass(award.className)
          val metric = deprodifier.transformMetric(award.metric)
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
          .getClass(STANDARD_RESOURCE)
          .allSubclasses()
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
    val actionIndex =
        (effect.trigger as? Effect.Trigger.OnGainOf)
            ?.expression
            ?.className
            ?.toString()
            ?.removePrefix(USE_ACTION.toString())
            ?.toIntOrNull()
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
      is Metric.Count -> expressionCouldCount(metric.expression, quantity, subjectClass, table)
      is Metric.Scaled ->
          metricCouldCount(
              metric.inner,
              quantity,
              subjectClass,
              table,
              scaledUpperBound(upperBound, metric.unit),
          )
      is Metric.Max ->
          (upperBound == null || metric.maximum > upperBound) &&
              metricCouldCount(metric.inner, quantity, subjectClass, table, upperBound)
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
      is Metric.Count ->
          baseExpressionCouldCount(
              metric.expression,
              quantity.resourceClass,
              subjectClass,
              table,
          )
      is Metric.Scaled ->
          metricCouldCountAsResource(
              metric.inner,
              quantity,
              subjectClass,
              table,
              scaledUpperBound(upperBound, metric.unit),
          )
      is Metric.Max ->
          (upperBound == null || metric.maximum > upperBound) &&
              metricCouldCountAsResource(
                  metric.inner,
                  quantity,
                  subjectClass,
                  table,
                  upperBound,
              )
      is Metric.Or ->
          metric.metrics.any {
            metricCouldCountAsResource(it, quantity, subjectClass, table)
          }
      is Metric.Transform ->
          metricCouldCountAsResource(metric.inner, quantity, subjectClass, table, upperBound)
    }
  }

  private fun scaledUpperBound(upperBound: Int?, unit: Int): Int? = upperBound?.let {
    (((it.toLong() + 1) * unit) - 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
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
      premise.authority.displayNamesByLanguage["en"]?.get(className) ?: className.toString()

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
                          "MilestonesAwardsExpansion",
                          "ColonyTile01",
                          "ColonyTile02",
                          "ColonyTile11",
                      )
                      .map(::cn),
              playerNames = listOf(cn("SoloPlayer")),
          )
      )
}

public fun main(): Unit =
    println(StandardResourceMonotonicityReport.render(StandardResourceMonotonicityReport.analyze()))
