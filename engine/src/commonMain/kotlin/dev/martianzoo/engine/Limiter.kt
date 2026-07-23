package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.engine.Limiter.RangeRestriction.SimpleRangeRestriction
import dev.martianzoo.engine.Limiter.RangeRestriction.UnboundRangeRestriction
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.types.MClass
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType
import kotlin.Int.Companion.MAX_VALUE

internal class Limiter(private val classes: MClassTable, private val components: ComponentGraph) {
  // visible for testing
  internal val rangeRestrictionsByClass: Map<MClass, List<RangeRestriction>> by lazy {
    val multimap = mutableMapOf<MClass, MutableList<RangeRestriction>>()

    classes
        .allClasses()
        .flatMap { mclass ->
          mclass.invariants().map { toRangeRestriction(it as Counting, mclass) }
        }
        .forEach { restriction ->
          restriction.mclass.allSubclasses().forEach {
            val list = multimap.getOrPut(it) { mutableListOf() }
            list += restriction
          }
        }
    multimap
  }

  init {
    val invalidDependencies =
        classes.allClasses().mapNotNull { dependent ->
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

  private fun toRangeRestriction(it: Counting, mclass: MClass): RangeRestriction {
    var expr = it.scaledEx.expression

    // Simplify it if we can
    if (mclass.concreteTypes().drop(1).none()) {
      expr = replaceThisExpressionsWith(mclass.className.expression).transform(expr)
    }

    return if (THIS in expr.descendantsOfType<ClassName>()) {
      UnboundRangeRestriction(expr, mclass, it.range)
    } else {
      SimpleRangeRestriction(classes.resolve(expr), it.range)
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

    fun count(mtype: MType) = components.count(mtype, StubTypeInfo)

    val headroom = gainInvars.map { it.range.last - count(it.mtype) }
    val footroom = removeInvars.map { count(it.mtype) - it.range.first }
    return (headroom + footroom).minOrNull() ?: MAX_VALUE
  }

  internal fun findAbstractGainLimit(type: MType): Int {
    val restrictions =
        rangeRestrictionsByClass[type.root].orEmpty().mapNotNull {
          val simple = it.bindThisTo(type) ?: return@mapNotNull null
          if (type.narrows(simple.mtype)) simple else null
        }
    return restrictions.minOfOrNull { it.range.last - components.count(it.mtype, StubTypeInfo) }
        ?: MAX_VALUE
  }

  internal fun applicableRangeRestrictions(component: Component?): Set<SimpleRangeRestriction> {
    val mtype = component?.type?.let { classes.resolve(it) } ?: return setOf()
    return applicableRangeRestrictions(mtype)
  }

  private fun applicableRangeRestrictions(mtype: MType): Set<SimpleRangeRestriction> {
    val allRestrictions = rangeRestrictionsByClass[mtype.root] ?: listOf()
    val ourRestrictions = allRestrictions.mapNotNull {
      val simple = it.bindThisTo(mtype) ?: return@mapNotNull null
      if (mtype.narrows(simple.mtype)) simple else null
    }
    return ourRestrictions.toSet() + SimpleRangeRestriction(mtype, 0..MAX_VALUE)
  }

  internal sealed class RangeRestriction {
    internal abstract val range: IntRange
    internal abstract val mclass: MClass

    internal abstract fun bindThisTo(mtype: MType): SimpleRangeRestriction?

    internal data class SimpleRangeRestriction(
        internal val mtype: MType,
        internal override val range: IntRange,
    ) : RangeRestriction() {
      internal override val mclass = mtype.root

      internal override fun bindThisTo(mtype: MType) = this

      override fun toString() = buildString {
        append(mtype.expression)
        append(" ")
        append(range.first)
        append("..")
        if (range.last == MAX_VALUE) append("*") else append(range.last)
      }
    }

    internal data class UnboundRangeRestriction(
        private val expression: Expression,
        private val declaringClass: MClass,
        internal override val range: IntRange,
    ) : RangeRestriction() {
      internal override val mclass =
          if (expression.className == THIS) declaringClass
          else declaringClass.loader.getClass(expression.className)

      internal override fun bindThisTo(mtype: MType): SimpleRangeRestriction? {
        val thisType =
            (listOf(mtype) + mtype.typeDependencies.map { it.boundType }).singleOrNull {
              it.root.isSubtypeOf(declaringClass)
            } ?: return null
        val expr = replaceThisExpressionsWith(thisType.expression).transform(expression)
        return SimpleRangeRestriction(declaringClass.loader.resolve(expr), range)
      }

      override fun toString() = "$expression $declaringClass $range"
    }
  }
}
