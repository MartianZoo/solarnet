package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.engine.Limiter.RangeRestriction.SimpleRangeRestriction
import dev.martianzoo.engine.Limiter.RangeRestriction.UnboundRangeRestriction
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.types.MClass
import dev.martianzoo.types.MClassTable
import dev.martianzoo.types.MType
import javax.inject.Inject
import kotlin.Int.Companion.MAX_VALUE

@GameScoped
internal class Limiter
@Inject
constructor(private val table: MClassTable, private val components: ComponentGraph) {
  // visible for testing
  internal val rangeRestrictionsByClass: Map<MClass, List<RangeRestriction>> by lazy {
    val multimap = mutableMapOf<MClass, MutableList<RangeRestriction>>()

    table
        .allClasses()
        .flatMap { mclass ->
          mclass.invariants().map { toRangeRestriction(it as Counting, mclass) }
        }
        .forEach { restriction ->
          restriction.mclass.getAllSubclasses().forEach {
            val list = multimap.computeIfAbsent(it) { mutableListOf() }
            list += restriction
          }
        }
    multimap
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
      SimpleRangeRestriction(table.resolve(expr), it.range)
    }
  }

  fun findLimit(gaining: Component?, removing: Component?): Int {
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

  fun applicableRangeRestrictions(component: Component?): Set<SimpleRangeRestriction> {
    val mtype = component?.type?.let { table.resolve(it) } ?: return setOf()
    val allRestrictions = rangeRestrictionsByClass[mtype.root] ?: listOf()
    val ourRestrictions =
        allRestrictions.mapNotNull {
          val simple = it.bindThisTo(mtype)
          if (mtype.narrows(simple.mtype)) simple else null
        }
    return ourRestrictions.toSet() + SimpleRangeRestriction(mtype, 0..MAX_VALUE)
  }

  internal sealed class RangeRestriction {
    abstract val range: IntRange
    abstract val mclass: MClass

    internal abstract fun bindThisTo(mtype: MType): SimpleRangeRestriction

    internal data class SimpleRangeRestriction(val mtype: MType, override val range: IntRange) :
        RangeRestriction() {
      override val mclass = mtype.root

      override fun bindThisTo(mtype: MType) = this

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
        override val mclass: MClass,
        override val range: IntRange
    ) : RangeRestriction() {
      override fun bindThisTo(mtype: MType): SimpleRangeRestriction {
        val expr = replaceThisExpressionsWith(mtype.expression).transform(expression)
        return SimpleRangeRestriction(mclass.loader.resolve(expr), range)
      }

      override fun toString() = "$expression $mclass $range"
    }
  }
}
