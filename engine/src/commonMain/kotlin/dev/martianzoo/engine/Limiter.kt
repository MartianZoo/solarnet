package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.invalidPetDefinition
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.api.TypeInfo.NoGameState
import dev.martianzoo.engine.Limiter.RangeRestriction.SimpleRangeRestriction
import dev.martianzoo.engine.Limiter.RangeRestriction.UnboundRangeRestriction
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Companion.split
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import kotlin.Int.Companion.MAX_VALUE

internal class Limiter(
    private val classTable: ClassTable,
    private val components: ComponentGraph,
) {
  // visible for testing
  internal val rangeRestrictionsByClass: Map<Class, List<RangeRestriction>> by lazy {
    val multimap = mutableMapOf<Class, MutableList<RangeRestriction>>()

    classTable
        .allClasses()
        .flatMap { klass ->
          klass.invariants().map {
            val counting =
                it as? Counting
                    ?: throw invalidPetDefinition(
                        "Class invariant on ${klass.className} is not a counting requirement: $it"
                    )
            toRangeRestriction(counting, klass)
          }
        }
        .forEach { restriction ->
          classTable.allSubclasses(restriction.root).forEach {
            val list = multimap.getOrPut(it) { mutableListOf() }
            list += restriction
          }
        }
    multimap
  }

  init {
    rangeRestrictionsByClass
    val invalidDependencies =
        classTable.allClasses().mapNotNull { dependent ->
          dependent.dependencies
              .concreteDependencyTargets()
              .filter(classTable::isActive)
              .firstOrNull { target ->
                applicableRangeRestrictions(target).all { it.range.last > 1 }
              }
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

  private fun toRangeRestriction(it: Counting, klass: Class): RangeRestriction {
    var expr =
        (it.metric as? Metric.Count)?.expression
            ?: throw invalidPetDefinition(
                "Class invariant on ${klass.className} must count one component expression: $it"
            )

    // Simplify it if we can
    if (classTable.allConcreteSubtypes(klass.baseType).drop(1).none()) {
      expr = replaceThisExpressionsWith(klass.className.expression).transformExpression(expr)
    }

    return if (THIS in expr.descendantsOfType<ClassName>()) {
      UnboundRangeRestriction(expr, klass, classTable, it.range)
    } else {
      SimpleRangeRestriction(classTable.resolve(expr), it.range)
    }
  }

  internal fun findLimit(gaining: Component?, removing: Component?): Int {
    if (gaining != null) {
      val missingDeps = gaining.dependencyComponents.filter { it !in components }
      if (missingDeps.any()) throw DependencyException(missingDeps.map { it.type })
    }

    // We must ignore any that are in common; the transmutation must hold them constant
    val (gainInvars, removeInvars) =
        run {
          val g = applicableRangeRestrictions(gaining)
          val r = applicableRangeRestrictions(removing)
          (g - r) to (r - g)
        }

    fun count(type: Type) = components.count(type, NoGameState)

    val headroom = gainInvars.map { it.range.last - count(it.type) }
    val footroom = removeInvars.map { count(it.type) - it.range.first }
    return (headroom + footroom).minOrNull() ?: MAX_VALUE
  }

  internal fun hasExecutableConcreteGain(
      type: Type,
      minimum: Int,
      info: TypeInfo,
  ): Boolean {
    require(type.abstract)
    require(minimum > 0)
    return classTable.allConcreteSubtypes(type).any { candidate ->
      candidate.narrows(type, info) &&
          try {
            findLimit(candidate.toComponent(), null) >= minimum
          } catch (_: DependencyException) {
            false
          }
    }
  }

  internal fun hasExecutableConcreteRemoval(
      type: Type,
      minimum: Int,
      info: GameReader,
  ): Boolean {
    require(type.abstract)
    require(minimum > 0)
    return info.getComponents(type).elements.any { candidate ->
      findLimit(null, candidate.toComponent()) >= minimum
    }
  }

  internal fun applicableRangeRestrictions(component: Component?): Set<SimpleRangeRestriction> {
    val type = component?.type ?: return emptySet()
    return applicableRangeRestrictions(type)
  }

  private fun applicableRangeRestrictions(type: Type): Set<SimpleRangeRestriction> {
    val allRestrictions = rangeRestrictionsByClass[type.rootClass].orEmpty()
    val ourRestrictions = allRestrictions.mapNotNull {
      val simple = it.bindThisTo(type) ?: return@mapNotNull null
      if (type.isSubtypeOf(simple.type)) simple else null
    }
    val applicable = ourRestrictions.toSet() + SimpleRangeRestriction(type, 0..MAX_VALUE)
    return applicable.filterTo(linkedSetOf()) { candidate ->
      applicable.none { stronger ->
        stronger.type == candidate.type &&
            stronger.range != candidate.range &&
            stronger.range.first >= candidate.range.first &&
            stronger.range.last <= candidate.range.last
      }
    }
  }

  internal sealed class RangeRestriction {
    internal abstract val range: IntRange
    internal abstract val root: Class

    internal abstract fun bindThisTo(type: Type): SimpleRangeRestriction?

    internal data class SimpleRangeRestriction(
        internal val type: Type,
        internal override val range: IntRange,
    ) : RangeRestriction() {
      internal override val root = type.rootClass

      internal override fun bindThisTo(type: Type) = this

      override fun toString() = buildString {
        append(type.expression)
        append(" ")
        append(range.first)
        append("..")
        if (range.last == MAX_VALUE) append("*") else append(range.last)
      }
    }

    internal data class UnboundRangeRestriction(
        private val expression: Expression,
        private val declaringClass: Class,
        private val classTable: ClassTable,
        internal override val range: IntRange,
    ) : RangeRestriction() {
      internal override val root =
          if (expression.className == THIS) declaringClass
          else classTable.getClass(expression.className)

      internal override fun bindThisTo(type: Type): SimpleRangeRestriction? {
        val thisType =
            (listOf(type) + type.typeDependencies.map { it.boundType }).singleOrNull {
              it.rootClass.isSubtypeOf(declaringClass)
            } ?: return null
        val expr = replaceThisExpressionsWith(thisType.expression).transformExpression(expression)
        return SimpleRangeRestriction(classTable.resolve(expr), range)
      }

      override fun toString() = "$expression $declaringClass $range"
    }
  }
}

internal fun Class.invariants(): Set<Requirement> =
    if (abstract) {
      emptySet()
    } else {
      allSuperclasses().flatMap { split(it.declaration.invariants) }.toSet()
    }

internal fun Class.isSingletonType(): Boolean =
    invariants().any {
      val counting = it as? Counting ?: return@any false
      counting.range.first == 1 && (counting.metric as? Metric.Count)?.expression == THIS.expression
    }
