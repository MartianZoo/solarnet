package dev.martianzoo.state

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Or
import dev.martianzoo.pets.ast.Metric.Rank
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.MetricType
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.NumberType
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.OptionalRequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementType
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset

/** State-owned implementation of the rich read model over a [GameWorld]. */
internal class GameReaderImpl(
    premise: GamePremise,
    private val gameWorld: GameWorld,
) : GameReader {
  override val classTable = premise.classTable
  override val actors = premise.actors
  override val catalog = premise.catalog

  private val elaborator = PetElaborator(classTable)
  private val customMetrics = CustomMetricRuntime(catalog, elaborator)

  override fun resolve(expression: Expression) = classTable.resolve(expression)

  override fun isAbstract(e: Expression) = resolve(e).isAbstract(this)

  override fun ensureNarrows(wide: Expression, narrow: Expression) =
      resolve(narrow).ensureNarrows(resolve(wide), this)

  override fun has(requirement: Requirement): Boolean = requirement.isMetBy(::count)

  override fun count(metric: Metric): Int =
      metric.evaluate({ countExpression(it.expression) }, ::readProperty, ::countUnion, ::rank)

  private fun rank(metric: Rank): Int {
    val candidateExpression =
        metric.candidate
            ?: throw ExpressionException(
                "RANK can only be evaluated while testing a concrete ${metric.selector}"
            )
    val candidate = classTable.resolve(candidateExpression)
    if (candidate.isAbstract(this)) {
      throw ExpressionException("RANK candidate is abstract: ${candidate.expressionFull}")
    }

    val peers = getComponents(classTable.resolve(metric.selector)).elements
    if (candidate !in peers) {
      throw ExpressionException(
          "RANK candidate ${candidate.expressionFull} is not a live ${metric.selector}"
      )
    }
    val candidateScore = rankScore(metric, candidate)
    return 1 + peers.count { compareRankScores(rankScore(metric, it), candidateScore) > 0 }
  }

  private fun rankScore(metric: Rank, candidate: Type): List<Int> {
    val owner = candidate.toComponent().owner
    val binding = chain(owner?.let(elaborator::contextualOwnerBinding))
    return metric.metricsFor(candidate.expressionFull).map { score ->
      val bound = binding.transformMetric(score)
      val evaluated =
          elaborator.evaluateProperties(
              bound,
              context = candidate.expressionFull,
              owner = owner,
          )
      count(evaluated)
    }
  }

  private fun compareRankScores(left: List<Int>, right: List<Int>): Int {
    check(left.size == right.size)
    left.indices.forEach { index ->
      val comparison = left[index].compareTo(right[index])
      if (comparison != 0) return comparison
    }
    return 0
  }

  private fun readProperty(property: Property): Int {
    val receiver =
        property.receiver
            ?: throw ExpressionException("Property `${property.propertyName}` has no receiver")
    val receiverType = classTable.resolve(receiver)
    val propertyType = receiverType.representedClass?.baseType ?: receiverType
    val propertyClass = propertyType.rootClass
    return when (val value = propertyClass.properties[property.propertyName]) {
      null ->
          throw ExpressionException(
              "Class `${propertyClass.className}` has no property `${property.propertyName}`"
          )
      MetricType,
      NumberType,
      OptionalRequirementType,
      RequirementType ->
          throw ExpressionException(
              "Property `${property.propertyName}` is abstract on `${propertyClass.className}`"
          )
      is NumberValue -> value.value
      is MetricValue ->
          throw ExpressionException(
              "Metric property `${property.propertyName}` must be evaluated in a class effect"
          )
      AbsentRequirementValue -> 0
      is RequirementValue -> 1
    }
  }

  private fun countUnion(metric: Or): Int {
    val union = mutableMapOf<Component, Int>()
    metric.metrics.forEach { alternative ->
      componentsMatching(alternative.expression).entries.forEach { (component, count) ->
        union[component] = maxOf(union[component] ?: 0, count)
      }
    }
    return union.values.sum()
  }

  private fun componentsMatching(expression: Expression) =
      classTable.resolve(expression).let { type ->
        if (!classTable.isInhabited(type)) return@let HashMultiset<Component>()
        if (type.rootClass.declaration.custom) {
          throw ExpressionException(
              "Custom metrics cannot be alternatives in an OR metric: ${type.expressionFull}"
          )
        }
        gameWorld.components.getAll(type, this)
      }

  private fun countExpression(expression: Expression): Int {
    val type = classTable.resolve(expression)
    if (!classTable.isInhabited(type)) return 0
    if (!type.rootClass.declaration.custom) return gameWorld.components.count(type, this)
    return customMetrics.count(type, this)
  }

  override fun count(type: Type) = gameWorld.components.count(type, this)

  override fun countComponent(concreteType: Type) =
      if (!classTable.isInhabited(concreteType)) 0
      else gameWorld.components.countComponent(concreteType.toComponent())

  override fun getComponents(type: Type) = gameWorld.components.getAll(type, this).map { it.type }

  override fun getDependents(component: Type): Set<Type> {
    require(!component.abstract)
    if (!classTable.isInhabited(component)) return emptySet()
    return gameWorld.components.dependentsOf(component.toComponent()).mapTo(linkedSetOf()) {
      it.type
    }
  }

  internal fun matchingComponentTypes(type: Type): Sequence<Type> =
      gameWorld.components.matchingTypes(type, this)
}
