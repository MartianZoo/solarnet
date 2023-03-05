package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
internal data class DependencySet(private val deps: Set<Dependency>) : Hierarchical<DependencySet> {
  init {
    Dependency.validate(deps)
  }

  val asSet: Set<TypeDependency> = deps.filterIsInstance<TypeDependency>().toSet() // TODO

  val keys: Set<Key> = deps.map { it.key }.toSetStrict()
  val expressions: List<Expression> = deps.map { it.expression }
  val expressionsFull: List<Expression> = deps.map { it.expressionFull }

  fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")

  fun getIfPresent(key: Key): Dependency? = deps.firstOrNull { it.key == key }

  // HIERARCHY

  override val abstract = deps.any { it.abstract }

  override fun isSubtypeOf(that: DependencySet) =
      that.deps.all { thatDep: Dependency -> this.get(thatDep.key).isSubtypeOf(thatDep) }

  override fun glb(that: DependencySet) = merge(that) { a, b -> (a glb b)!! }

  override fun lub(that: DependencySet): DependencySet {
    val keys = keys.intersect(that.keys)
    return copy(keys.map { this.get(it) lub that.get(it) }.toSetStrict())
  }

  // OTHER OPERATIONS

  fun merge(that: DependencySet, merger: (Dependency, Dependency) -> Dependency): DependencySet {
    val merged = (this.keys + that.keys).map {
      setOfNotNull(this.getIfPresent(it), that.getIfPresent(it)).reduce(merger)
    }
    return copy(merged.toSetStrict())
  }

  fun overlayOn(that: DependencySet) = merge(that) { ours, _ -> ours }

  fun minus(that: DependencySet) = copy(this.deps - that.deps)

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  fun subMapInOrder(keysInOrder: Iterable<Key>) =
      copy(keysInOrder.mapNotNull(::getIfPresent).toSetStrict())

  fun getClassForClassType() = Dependency.getClassForClassType(deps)

  override fun toString() = "$deps"
}
