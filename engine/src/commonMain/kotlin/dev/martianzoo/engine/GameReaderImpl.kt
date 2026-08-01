package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.CustomCodeException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.GamePremise
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Metric.Or
import dev.martianzoo.pets.ast.Metric.Scaled
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset
import kotlin.math.min

internal class GameReaderImpl(
    private val classTable: ClassTable,
    private val components: ComponentGraph,
    internal val transformers: Transformers,
    private val premise: GamePremise,
) : GameReader {
  override val ruleset = premise.ruleset

  override fun resolve(expression: Expression) = classTable.resolve(expression)

  internal fun matchesConstraint(candidate: Type, constraint: Expression, domain: Type) =
      classTable.matchesConstraint(candidate, constraint, domain, this)

  // Next 3 are for TypeInfo interface

  override fun isAbstract(e: Expression) = resolve(e).abstract

  override fun ensureNarrows(wide: Expression, narrow: Expression) =
      resolve(narrow).ensureNarrows(resolve(wide), this)

  override fun has(requirement: Requirement): Boolean = requirement.isMetBy(::count)

  override fun count(metric: Metric): Int =
      when (metric) {
        is Count -> countExpression(metric.expression)
        is Scaled -> count(metric.inner) / metric.unit
        is Metric.Max -> min(count(metric.inner), metric.maximum)
        is Or -> countUnion(metric)
        is Metric.Transform -> throw ExpressionException("unhandled metric transform: $metric")
      }

  private fun countUnion(metric: Or): Int {
    val union = mutableMapOf<Component, Int>()
    metric.metrics.forEach { alternative ->
      if (alternative !is Count) {
        throw ExpressionException(
            "OR metric alternatives must count components, but found: $alternative"
        )
      }
      componentsMatching(alternative.expression).entries.forEach { (component, count) ->
        union[component] = maxOf(union[component] ?: 0, count)
      }
    }
    return union.values.sum()
  }

  private fun componentsMatching(expression: Expression) =
      if (classTable.isUnresolvedClassLiteral(expression)) {
        HashMultiset<Component>()
      } else {
        val type = classTable.resolve(expression)
        if (type.rootClass.declaration.custom) {
          throw ExpressionException(
              "Custom metrics cannot be alternatives in an OR metric: ${type.expressionFull}"
          )
        }
        components.getAll(type, this)
      }

  private fun countExpression(expression: Expression): Int {
    if (classTable.isUnresolvedClassLiteral(expression)) return 0
    val type = classTable.resolve(expression)
    if (!type.rootClass.declaration.custom) return components.count(type, this)

    val implementation =
        ruleset.customMetric(type.className)
            ?: throw CustomCodeException(
                "Custom class `${type.className}` has no metric implementation"
            )
    if (type.abstract)
        throw AbstractException("custom metric type is abstract: ${type.expressionFull}")

    val count =
        try {
          implementation.count(this, type)
        } catch (e: RuntimeException) {
          throw CustomCodeException("Custom metric failed for ${type.expressionFull}", e)
        }
    if (count < 0) {
      throw CustomCodeException("Custom metric `${type.expressionFull}` returned $count")
    }
    return count
  }

  override fun count(type: Type) = components.count(type, this)

  override fun containsAny(type: Type) = components.containsAny(type, this)

  override fun countComponent(concreteType: Type) =
      components.countComponent(concreteType.toComponent(this))

  override fun getComponents(type: Type) = components.getAll(type, this).map { it.type }
}
