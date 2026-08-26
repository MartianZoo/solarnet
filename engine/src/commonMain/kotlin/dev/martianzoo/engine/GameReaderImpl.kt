package dev.martianzoo.engine

import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Or
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
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset

internal class GameReaderImpl(
    private val classTable: ClassTable,
    private val components: ComponentGraph,
    internal val transformers: Transformers,
    private val customClasses: CustomClassRuntime,
    private val premise: GamePremise,
) : GameReader {
  override val actors = premise.actors

  override val catalog = premise.catalog

  override fun resolve(expression: Expression) = classTable.resolve(expression)

  internal fun matchesConstraint(candidate: Type, constraint: Expression, domain: Type) =
      classTable.matchesConstraint(candidate, constraint, domain, this)

  // Next 3 are for TypeInfo interface

  override fun isAbstract(e: Expression) = resolve(e).isAbstract(this)

  override fun ensureNarrows(wide: Expression, narrow: Expression) =
      resolve(narrow).ensureNarrows(resolve(wide), this)

  override fun has(requirement: Requirement): Boolean = requirement.isMetBy(::count)

  override fun count(metric: Metric): Int =
      metric.evaluate({ countExpression(it.expression) }, ::readProperty, ::countUnion)

  private fun readProperty(property: Property): Int {
    val receiver =
        property.receiver
            ?: throw ExpressionException("Property `${property.propertyName}` has no receiver")
    val receiverType = classTable.resolve(receiver)
    val propertyType =
        if (receiverType.rootClass === classTable.classClass) {
          classTable.resolve(receiverType.expressionFull.arguments.single())
        } else {
          receiverType
        }
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
        if (!classTable.isActive(type)) return@let HashMultiset<Component>()
        if (type.rootClass.declaration.custom) {
          throw ExpressionException(
              "Custom metrics cannot be alternatives in an OR metric: ${type.expressionFull}"
          )
        }
        components.getAll(type, this)
      }

  private fun countExpression(expression: Expression): Int {
    val type = classTable.resolve(expression)
    if (!classTable.isActive(type)) return 0
    if (!type.rootClass.declaration.custom) return components.count(type, this)

    return customClasses.count(type, this)
  }

  override fun count(type: Type) = components.count(type, this)

  override fun containsAny(type: Type) = components.containsAny(type, this)

  override fun countComponent(concreteType: Type) =
      if (!classTable.isActive(concreteType)) 0
      else components.countComponent(concreteType.toComponent(this))

  override fun getComponents(type: Type) = components.getAll(type, this).map { it.type }
}
