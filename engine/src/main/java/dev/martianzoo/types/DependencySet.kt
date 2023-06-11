package dev.martianzoo.types

import dev.martianzoo.tfm.api.Exceptions
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.cartesianProduct
import dev.martianzoo.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
internal class DependencySet private constructor(private val deps: Set<Dependency>) :
    Hierarchical<DependencySet> {

  companion object {
    fun of(deps: Set<Dependency>): DependencySet {
      Dependency.validate(deps)
      return DependencySet(deps)
    }

    fun of() = of(setOf())
    fun of(deps: Iterable<Dependency>) = of(deps.toSetStrict())
  }

  val flattened: Map<DependencyPath, MClass> by lazy {
    deps
        .flatMap {
          // Throwing away refinements & links...
          val result = mutableListOf(DependencyPath(it.key) to it.boundClass)
          if (it is TypeDependency) {
            result +=
                it.boundType.dependencies.flattened.map { (depPath, boundClass) ->
                  depPath.prepend(it.key) to boundClass
                }
          }
          result
        }
        .toMap()
  }

  fun at(path: DependencyPath): Dependency {
    val x: Dependency = get(path.keyList.first())
    if ((path.keyList.size) == 1) return x
    val type = (x as TypeDependency).boundType
    return type.dependencies.at(path.drop(1))
  }

  val typeDependencies: Set<TypeDependency> = deps.filterIsInstance<TypeDependency>().toSet()

  val keys: Set<Key> = deps.toSetStrict { it.key }
  val expressions: List<Expression> by lazy { deps.map { it.expression } }
  val expressionsFull: List<Expression> by lazy { deps.map { it.expressionFull } }

  fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")

  fun getIfPresent(key: Key): Dependency? = deps.firstOrNull { it.key == key }

  // HIERARCHY

  override val abstract = deps.any { it.abstract }

  override fun isSubtypeOf(that: DependencySet) =
      that.deps.all { thatDep: Dependency -> this.get(thatDep.key).isSubtypeOf(thatDep) }

  override fun glb(that: DependencySet): DependencySet? =
      merge(that) { a, b -> (a glb b) ?: return@glb null }

  override fun lub(that: DependencySet): DependencySet {
    val keys = keys.intersect(that.keys)
    return of(keys.map { this.get(it) lub that.get(it) })
  }

  override fun ensureNarrows(that: DependencySet, info: TypeInfo) =
      that.deps.forEach { get(it.key).ensureNarrows(it, info) }

  fun narrows(that: DependencySet, info: TypeInfo) = that.deps.all { get(it.key).narrows(it, info) }

  // OTHER OPERATORS

  inline fun merge(
      that: DependencySet,
      merger: (Dependency, Dependency) -> Dependency,
  ): DependencySet {
    val merged =
        (this.keys + that.keys).map {
          setOfNotNull(this.getIfPresent(it), that.getIfPresent(it)).reduce(merger)
        }
    return of(merged)
  }

  fun overlayOn(that: DependencySet) = merge(that) { ours, _ -> ours }

  fun minus(that: DependencySet) = of(this.deps - that.deps)

  // OTHER

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  fun subMapInOrder(keysInOrder: Iterable<Key>) = of(keysInOrder.mapNotNull(::getIfPresent))

  fun getClassForClassType() = Dependency.getClassForClassType(deps)

  internal fun map(function: (MType) -> MType) =
      DependencySet(deps.toSetStrict { if (it is TypeDependency) it.map(function) else it })

  fun specialize(specs: List<Expression>): DependencySet {
    // This has been a bit optimized
    val partial = matchPartial(specs)
    return of(keys.map { partial.getIfPresent(it) ?: get(it) })
  }

  /**
   * For an example expression like `Foo<Bar, Qux>`, pass in `[Bar, Qux]` and Foo's base dependency
   * set. This method decides which dependencies in the dependency set each of these args should be
   * matched with. The returned dependency set will have [TypeDependency]s in the corresponding
   * order to the input expressions.
   */
  fun matchPartial(args: List<Expression>): DependencySet {
    val usedDeps = mutableSetOf<Dependency>()

    fun dependency(arg: Expression, it: Dependency): Dependency? {
      val intersectionType: Dependency? = it.intersect(arg)
      return if (intersectionType != null && usedDeps.add(it)) {
        intersectionType
      } else {
        null
      }
    }

    return of(
        args.map { arg ->
          deps.firstNotNullOfOrNull { dependency(arg, it) }
              ?: throw Exceptions.badExpression(arg, toString())
        })
  }

  /** Returns the subset of [allConcreteSubtypes] having the exact same [root] as ours. */
  public fun concreteSubtypesSameClass(mtype: MType): Sequence<MType> {
    // TODO do this without try/catch
    return try {
      mtype.concreteSubclasses(getClassForClassType()).map { it.classType }
    } catch (ignore: Exception) {
      val axes: List<Sequence<TypeDependency>> =
          typeDependencies.map { it.allConcreteSpecializations() }
      axes.cartesianProduct().map { mtype.root.withAllDependencies(DependencySet.of(it)) }
    }
  }

  override fun equals(other: Any?) = other is DependencySet && deps == other.deps
  override fun hashCode() = deps.hashCode()

  override fun toString() = "$deps"

  data class DependencyPath(val keyList: List<Key>) {
    constructor(key: Key) : this(listOf(key))

    init {
      require(keyList.any())
    }

    fun prepend(key: Key) = copy(listOf(key) + keyList)
    fun drop(i: Int) = DependencyPath(keyList.drop(i))

    fun isProperSuffixOf(other: DependencyPath): Boolean {
      val otherSize = other.keyList.size
      val smallerBy = keyList.size - otherSize
      return smallerBy > 0 && other.keyList.subList(smallerBy, otherSize) == keyList
    }
  }
}