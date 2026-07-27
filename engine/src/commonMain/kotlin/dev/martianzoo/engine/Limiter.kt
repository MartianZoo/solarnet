package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.TypeInfo.NoGameState
import dev.martianzoo.engine.Limiter.RangeRestriction.SimpleRangeRestriction
import dev.martianzoo.engine.Limiter.RangeRestriction.UnboundRangeRestriction
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Companion.split
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.types.Class
import dev.martianzoo.types.Type
import dev.martianzoo.types.TypeUniverse
import kotlin.Int.Companion.MAX_VALUE

internal class Limiter(
    private val typeUniverse: TypeUniverse,
    private val components: ComponentGraph,
) {
  // visible for testing
  internal val rangeRestrictionsByClass: Map<Class, List<RangeRestriction>> by lazy {
    val multimap = mutableMapOf<Class, MutableList<RangeRestriction>>()

    typeUniverse
        .allClasses()
        .flatMap { klass ->
          klass.invariants().map { toRangeRestriction(it as Counting, klass) }
        }
        .forEach { restriction ->
          restriction.root.allSubclasses().forEach {
            val list = multimap.getOrPut(it) { mutableListOf() }
            list += restriction
          }
        }
    multimap
  }

  init {
    val invalidDependencies =
        typeUniverse.allClasses().mapNotNull { dependent ->
          dependent.dependencies
              .concreteDependencyTargets()
              .firstOrNull { target ->
                applicableRangeRestrictions(target).all { it.range.last > 1 }
              }
              ?.let { dependent to it }
        }

    check(invalidDependencies.isEmpty()) {
      "Dependencies must target types with maximum multiplicity 1; first violation per class:\n" +
          invalidDependencies.joinToString("\n") { (dependent, target) ->
            "  ${dependent.className} -> ${target.expressionFull}"
          }
    }
  }

  private fun toRangeRestriction(it: Counting, klass: Class): RangeRestriction {
    var expr = it.scaledEx.expression

    // Simplify it if we can
    if (klass.concreteTypes().drop(1).none()) {
      expr = replaceThisExpressionsWith(klass.className.expression).transform(expr)
    }

    return if (THIS in expr.descendantsOfType<ClassName>()) {
      UnboundRangeRestriction(expr, klass, it.range)
    } else {
      SimpleRangeRestriction(typeUniverse.resolve(expr), it.range)
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

  internal fun findAbstractGainLimit(type: Type): Int {
    val restrictions =
        rangeRestrictionsByClass[type.rootClass].orEmpty().mapNotNull {
          val simple = it.bindThisTo(type) ?: return@mapNotNull null
          if (type.isSubtypeOf(simple.type)) simple else null
        }
    return restrictions.minOfOrNull { it.range.last - components.count(it.type, NoGameState) }
        ?: MAX_VALUE
  }

  internal fun applicableRangeRestrictions(component: Component?): Set<SimpleRangeRestriction> {
    val type = component?.type ?: return setOf()
    return applicableRangeRestrictions(type)
  }

  private fun applicableRangeRestrictions(type: Type): Set<SimpleRangeRestriction> {
    val allRestrictions = rangeRestrictionsByClass[type.rootClass] ?: listOf()
    val ourRestrictions = allRestrictions.mapNotNull {
      val simple = it.bindThisTo(type) ?: return@mapNotNull null
      if (type.isSubtypeOf(simple.type)) simple else null
    }
    return ourRestrictions.toSet() + SimpleRangeRestriction(type, 0..MAX_VALUE)
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
        internal override val range: IntRange,
    ) : RangeRestriction() {
      internal override val root =
          if (expression.className == THIS) declaringClass
          else declaringClass.typeUniverse.getClass(expression.className)

      internal override fun bindThisTo(type: Type): SimpleRangeRestriction? {
        val thisType =
            (listOf(type) + type.typeDependencies.map { it.boundType }).singleOrNull {
              it.rootClass.isSubtypeOf(declaringClass)
            } ?: return null
        val expr = replaceThisExpressionsWith(thisType.expression).transform(expression)
        return SimpleRangeRestriction(declaringClass.typeUniverse.resolve(expr), range)
      }

      override fun toString() = "$expression $declaringClass $range"
    }
  }
}

internal fun Class.invariants(): Set<Requirement> =
    if (abstract) {
      setOf()
    } else {
      allSuperclasses().flatMap { split(it.declaration.invariants) }.toSet()
    }

internal fun Class.isSingletonType(): Boolean =
    invariants().any {
      (it as Counting).range.first == 1 && it.scaledEx.expression == THIS.expression
    }
