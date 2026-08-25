package dev.martianzoo.pets.types

import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement.Counting
import kotlin.Int.Companion.MAX_VALUE

/** Immutable component-count limits for one active [ClassTable] projection. */
public class ClassLimitTable private constructor(private val classTable: ClassTable) {
  /** One concrete Type bound and its allowed multiplicity. */
  public data class Limit(public val type: Type, public val range: IntRange)

  private val restrictionsByClass: Map<Class, List<Restriction>> = compileRestrictions()

  init {
    val invalidDependencies =
        classTable.allClasses().mapNotNull { dependent ->
          dependent.dependencies
              .concreteDependencyTargets()
              .filter(classTable::isActive)
              .firstOrNull { target -> limitsFor(target).all { it.range.last > 1 } }
              ?.let { dependent to it }
        }

    if (invalidDependencies.isNotEmpty()) {
      throw invalidPetDefinition(
          "Dependencies must target types with maximum multiplicity 1; first violation per class:\n" +
              invalidDependencies.joinToString("\n") { (dependent, target) ->
                "  ${dependent.className} -> ${target.expressionFull}"
              }
      )
    }
  }

  /** Every strongest applicable limit for [type], including its unconstrained fallback. */
  public fun limitsFor(type: Type): Set<Limit> {
    require(classTable.knows(type)) { "$type belongs to a different Catalog" }
    val bound = restrictionsByClass[type.rootClass].orEmpty().mapNotNull { it.bindThisTo(type) }
    val applicable = bound.filter { type.isSubtypeOf(it.type) }.toSet() + Limit(type, 0..MAX_VALUE)
    return applicable.filterTo(linkedSetOf()) { candidate ->
      applicable.none { stronger ->
        stronger.type == candidate.type &&
            stronger.range != candidate.range &&
            stronger.range.first >= candidate.range.first &&
            stronger.range.last <= candidate.range.last
      }
    }
  }

  private fun compileRestrictions(): Map<Class, List<Restriction>> {
    val restrictions = mutableMapOf<Class, MutableList<Restriction>>()
    classTable
        .allClasses()
        .flatMap { klass ->
          klass.invariants.map { invariant ->
            val counting =
                invariant as? Counting
                    ?: throw invalidPetDefinition(
                        "Class invariant on ${klass.className} is not a counting requirement: $invariant"
                    )
            toRestriction(counting, klass)
          }
        }
        .forEach { restriction ->
          classTable.allSubclasses(restriction.root).forEach { subclass ->
            restrictions.getOrPut(subclass, ::mutableListOf) += restriction
          }
        }
    return restrictions
  }

  private fun toRestriction(invariant: Counting, klass: Class): Restriction {
    var expression =
        (invariant.metric as? Metric.Count)?.expression
            ?: throw invalidPetDefinition(
                "Class invariant on ${klass.className} must count one component expression: $invariant"
            )

    if (classTable.allConcreteSubtypes(klass.baseType).drop(1).none()) {
      expression =
          replaceThisExpressionsWith(klass.className.expression).transformExpression(expression)
    }

    return if (THIS in expression.descendantsOfType<ClassName>()) {
      UnboundRestriction(expression, klass, classTable, invariant.range)
    } else {
      BoundRestriction(classTable.resolve(expression), invariant.range)
    }
  }

  private sealed interface Restriction {
    val range: IntRange
    val root: Class

    fun bindThisTo(type: Type): Limit?
  }

  private data class BoundRestriction(
      val type: Type,
      override val range: IntRange,
  ) : Restriction {
    override val root: Class = type.rootClass

    override fun bindThisTo(type: Type): Limit = Limit(this.type, range)
  }

  private data class UnboundRestriction(
      val expression: Expression,
      val declaringClass: Class,
      val classTable: ClassTable,
      override val range: IntRange,
  ) : Restriction {
    override val root: Class =
        if (expression.className == THIS) declaringClass
        else classTable.getClass(expression.className)

    override fun bindThisTo(type: Type): Limit? {
      val thisType =
          (listOf(type) + type.typeDependencies.map { it.boundType }).singleOrNull {
            it.rootClass.isSubtypeOf(declaringClass)
          } ?: return null
      val bound = replaceThisExpressionsWith(thisType.expression).transformExpression(expression)
      return Limit(classTable.resolve(bound), range)
    }
  }

  internal companion object {
    internal fun create(classTable: ClassTable): ClassLimitTable = ClassLimitTable(classTable)
  }
}
