package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.associateByStrict
import dev.martianzoo.util.mergeMaps
import dev.martianzoo.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
internal data class DependencySet(private val deps: Set<Dependency>) {
  init {
    Dependency.validate(deps)
  }

  val dependencies: Set<TypeDependency> = deps.filterIsInstance<TypeDependency>().toSet()

  val keys: Set<Key> = deps.map { it.key }.toSetStrict()
  val expressions: List<Expression> = deps.map { it.expression }
  val expressionsFull: List<Expression> = deps.map { it.expressionFull }

  val abstract = deps.any { it.abstract }

  fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")
  fun getIfPresent(key: Key): Dependency? = deps.firstOrNull { it.key == key }

  // used by PType.isSubtypeOf()
  fun specializes(that: DependencySet) =
      that.deps.all { thatDep: Dependency -> this.get(thatDep.key).isSubtypeOf(thatDep) }

  private fun asMap() = deps.associateByStrict { it.key } // TODO get rid

  fun merge(that: DependencySet, merger: (Dependency, Dependency) -> Dependency) =
      copy(mergeMaps(asMap(), that.asMap(), merger).values.toSetStrict()) // TODO fix

  // Combines all entries, using the glb when both maps have the same key
  fun intersect(that: DependencySet) = merge(that) { a, b -> a.glb(b)!! }

  fun lub(that: DependencySet): DependencySet {
    val keys = keys.intersect(that.keys)
    return copy(keys.map { get(it).lub(that.get(it)) }.toSetStrict())
  }

  fun overlayOn(that: DependencySet) = merge(that) { ours, _ -> ours }

  fun minus(that: DependencySet) = copy(this.deps - that.deps)

  fun keyToExpression() = deps.associate { it.key to it.expression }

  companion object {
    fun intersect(maps: Collection<DependencySet>): DependencySet {
      var map = DependencySet(setOf())
      maps.forEach { map = map.intersect(it) }
      return map
    }
  }

  override fun toString() = "$deps"

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  fun subMap(keysInOrder: Iterable<Key>) = copy(keysInOrder.mapNotNull(::getIfPresent).toSetStrict())

  fun getClassForClassType() = Dependency.getClassForClassType(deps)
}
