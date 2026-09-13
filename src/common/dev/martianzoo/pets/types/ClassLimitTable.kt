package dev.martianzoo.pets.types

import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement.Counting
import kotlin.Int.Companion.MAX_VALUE

/**
 * Immutable component-count limits for one active class-table view. The type system uses these
 * limits to enforce that every dependency can identify a unique component, as specified by
 * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies),
 * and the engine applies the same compiled limits to live component counts.
 */
public class ClassLimitTable private constructor(private val classTable: ClassTable) {
  /**
   * One component domain and the invariant range that constrains its multiplicity under
   * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   *
   * @constructor Associates a component [type] with one inclusive multiplicity [range] for the
   *   invariant used by
   *   [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public data class Limit(
      /**
       * The component type constrained by this limit under
       * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
       */
      public val type: Type,

      /**
       * The inclusive number of matching components permitted by the invariant used in
       * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
       */
      public val range: IntRange,
  )

  private val restrictionsByClass: Map<Class, List<Restriction>> = compileRestrictions()

  init {
    val invalidDependencies =
        classTable
            .allClasses()
            .filterNot { it.abstract }
            .mapNotNull { dependent ->
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

  /**
   * Returns every strongest component-count invariant applicable to [type], plus an unconstrained
   * fallback. Dependency validation accepts a target only if at least one returned upper bound is
   * at most one, implementing
   * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
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

  /**
   * Returns every positive-lower-bound limit that must hold for a completed component set.
   * Self-counts apply to every active concrete specialization; a dependent count containing `This`
   * applies only to the declaring types present in [liveTypes].
   */
  public fun requiredLimits(liveTypes: Collection<Type>): Set<Limit> {
    liveTypes.forEach { require(classTable.knows(it)) { "$it belongs to a different Catalog" } }
    val liveTypeSet = liveTypes.toSet()
    return restrictionsByClass.values
        .asSequence()
        .flatten()
        .distinct()
        .filter { it.range.first > 0 }
        .flatMap { it.requiredLimits(liveTypeSet) }
        .toSet()
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
    val expression =
        (invariant.metric as? Metric.Count)?.expression
            ?: throw invalidPetDefinition(
                "Class invariant on ${klass.className} must count one component expression: $invariant"
            )
    if (THIS !in expression.descendantsOfType<ClassName>()) {
      return BoundRestriction(classTable.resolve(expression), invariant.range)
    }
    if (classTable.allConcreteSubtypes(klass.baseType).drop(1).none()) {
      val bound =
          replaceThisExpressionsWith(klass.className.expression).transformExpression(expression)
      return ScopedBoundRestriction(
          classTable.resolve(bound),
          klass,
          expression.className == THIS,
          invariant.range,
      )
    }
    return UnboundRestriction(expression, klass, classTable, invariant.range)
  }

  private sealed interface Restriction {
    val range: IntRange
    val root: Class

    fun bindThisTo(type: Type): Limit?

    fun requiredLimits(liveTypes: Set<Type>): Sequence<Limit>
  }

  private data class BoundRestriction(
      val type: Type,
      override val range: IntRange,
  ) : Restriction {
    override val root: Class = type.rootClass

    override fun bindThisTo(type: Type): Limit = Limit(this.type, range)

    override fun requiredLimits(liveTypes: Set<Type>): Sequence<Limit> =
        sequenceOf(Limit(type, range))
  }

  private data class ScopedBoundRestriction(
      val type: Type,
      val declaringClass: Class,
      val selfCount: Boolean,
      override val range: IntRange,
  ) : Restriction {
    override val root: Class = type.rootClass

    override fun bindThisTo(type: Type): Limit = Limit(this.type, range)

    override fun requiredLimits(liveTypes: Set<Type>): Sequence<Limit> =
        if (selfCount || liveTypes.any { it.rootClass.isSubtypeOf(declaringClass) }) {
          sequenceOf(Limit(type, range))
        } else {
          emptySequence()
        }
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
      return bindThisToScope(thisType)
    }

    override fun requiredLimits(liveTypes: Set<Type>): Sequence<Limit> {
      val scopes =
          if (expression.className == THIS) {
            classTable.allConcreteSubtypes(declaringClass.baseType)
          } else {
            liveTypes.asSequence().filter { it.rootClass.isSubtypeOf(declaringClass) }
          }
      return scopes.distinct().map(::bindThisToScope)
    }

    private fun bindThisToScope(thisType: Type): Limit {
      val bound =
          replaceThisExpressionsWith(thisType.expressionFull).transformExpression(expression)
      return Limit(classTable.resolve(bound), range)
    }
  }

  internal companion object {
    internal fun create(classTable: ClassTable): ClassLimitTable = ClassLimitTable(classTable)
  }
}
