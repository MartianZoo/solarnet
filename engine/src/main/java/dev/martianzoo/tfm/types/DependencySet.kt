package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
internal class DependencySet private constructor(val deps: Set<Dependency>) :
    Hierarchical<DependencySet> {

  companion object {
    fun of(deps: Set<Dependency>): DependencySet {
      Dependency.validate(deps)
      return DependencySet(deps)
    }

    fun of() = of(setOf())
    fun of(deps: Iterable<Dependency>) = of(deps.toSetStrict())
  }

  val flattened: Map<DependencyPath, PClass> by lazy {
    deps.flatMap {
      // Throwing away refinements & links...
      val result = mutableListOf(DependencyPath(it.key) to it.boundClass)
      if (it is TypeDependency) {
        result +=
            it.boundType.dependencies.flattened.map { (depPath, boundClass) ->
              depPath.prepend(it.key) to boundClass
            }
      }
      result
    }.toMap()
  }

  val asSet: Set<TypeDependency> = deps.filterIsInstance<TypeDependency>().toSet() // TODO

  val keys: Set<Key> = deps.map { it.key }.toSetStrict()
  val expressions: List<Expression> by lazy { deps.map { it.expression } }
  val expressionsFull: List<Expression> by lazy { deps.map { it.expressionFull } }

  fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")

  fun getIfPresent(key: Key): Dependency? = deps.firstOrNull { it.key == key }

  // HIERARCHY

  override val abstract = deps.any { it.abstract }

  override fun isSubtypeOf(that: DependencySet) =
      that.deps.all { thatDep: Dependency -> this.get(thatDep.key).isSubtypeOf(thatDep) }

  override fun glb(that: DependencySet) = merge(that) { a, b -> (a glb b) ?: error("$a $b") }

  override fun lub(that: DependencySet): DependencySet {
    val keys = keys.intersect(that.keys)
    return of(keys.map { this.get(it) lub that.get(it) })
  }

  // OTHER OPERATORS

  fun merge(that: DependencySet, merger: (Dependency, Dependency) -> Dependency): DependencySet {
    val merged =
        (this.keys + that.keys).map {
          setOfNotNull(this.getIfPresent(it), that.getIfPresent(it)).reduce(merger)
        }
    return of(merged)
  }

  fun overlayOn(that: DependencySet) = merge(that) { ours, _ -> ours } // TODO hmm

  fun minus(that: DependencySet) = of(this.deps - that.deps)

  // OTHER

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  fun subMapInOrder(keysInOrder: Iterable<Key>) = of(keysInOrder.mapNotNull(::getIfPresent))

  fun getClassForClassType() = Dependency.getClassForClassType(deps)

  override fun equals(other: Any?) = other is DependencySet && deps == other.deps
  override fun hashCode() = deps.hashCode()

  override fun toString() = "$deps"

  data class DependencyPath(val keyList: List<Key>) {
    constructor(key: Key) : this(listOf(key))

    fun prepend(key: Key) = copy(listOf(key) + keyList)

    fun isProperSuffixOf(other: DependencyPath): Boolean {
      val otherSize = other.keyList.size
      val smallerBy = keyList.size - otherSize
      return smallerBy > 0 && other.keyList.subList(smallerBy, otherSize) == keyList
    }
  }
}
