package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Dependency.Companion.depsForClassType
import dev.martianzoo.types.Dependency.Companion.getClassForClassType
import dev.martianzoo.types.Dependency.Companion.isForClassType
import dev.martianzoo.types.Dependency.ComplementDependency
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
public class DependencySet private constructor(private val deps: Set<Dependency>) :
    Hierarchical<DependencySet> {

  internal companion object {
    internal fun of(deps: Set<Dependency>): DependencySet {
      Dependency.validate(deps)
      return DependencySet(deps)
    }

    internal fun of() = of(emptySet())

    internal fun of(deps: Iterable<Dependency>) = of(deps.toSetStrict())
  }

  public fun flatten(): Map<DependencyPath, Class> {
    return deps
        .flatMap { dep: Dependency ->
          // Throwing away refinements & links...
          buildList {
            add(DependencyPath(dep.key) to dep.boundClass)
            if (dep is TypeDependency) {
              dep.boundType.dependencies.flatten().forEach { (depPath, boundClass) ->
                add(depPath.prepend(dep.key) to boundClass)
              }
            }
          }
        }
        .toMap()
  }

  public fun at(path: DependencyPath): Dependency {
    val x: Dependency = get(path.keyList.first())
    if ((path.keyList.size) == 1) return x
    val type =
        when (x) {
          is TypeDependency -> x.boundType
          is ComplementDependency -> x.domainType
          else -> error("unexpected dependency: $x")
        }
    return type.dependencies.at(path.drop(1))
  }

  public fun typeDependencies(): Set<TypeDependency> =
      deps.filterIsInstance<TypeDependency>().toSet()

  private fun complementDependencies(): Set<ComplementDependency> =
      deps.filterIsInstance<ComplementDependency>().toSet()

  public fun concreteDependencyTargets(): Sequence<Type> =
      (typeDependencies().asSequence() + complementDependencies().asSequence())
          .flatMap {
            when (it) {
              is TypeDependency -> it.allConcreteSpecializations()
              is ComplementDependency -> it.allConcreteSpecializations()
              else -> error("Unexpected dependency: $it")
            }
          }
          .map { it.boundType }

  public val keys: Set<Key> = deps.toSetStrict { it.key }

  internal val classTable: ClassTable? = deps.firstOrNull()?.boundClass?.classTable

  internal fun expressions(): List<Expression> = deps.map { it.expression }

  internal fun expressionsFull(): List<Expression> = deps.map { it.expressionFull }

  internal inline fun expressionsFull(function: (Dependency) -> Expression): List<Expression> =
      deps.map(function)

  internal fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")

  internal fun getIfPresent(key: Key): Dependency? = deps.firstOrNull { it.key == key }

  // HIERARCHY

  override val abstract: Boolean = deps.any { it.abstract }

  override fun isSubtypeOf(that: DependencySet): Boolean {
    requireSameClassTable(that)
    return that.deps.all { get(it.key).isSubtypeOf(it) }
  }

  override fun glb(that: DependencySet): DependencySet? {
    requireSameClassTable(that)
    return merge(that) { a, b -> (a glb b) ?: return@glb null }
  }

  override fun lub(that: DependencySet): DependencySet {
    requireSameClassTable(that)
    val keys = keys.intersect(that.keys)
    return of(keys.map { this.get(it) lub that.get(it) })
  }

  override fun ensureNarrows(that: DependencySet, info: TypeInfo) {
    requireSameClassTable(that)
    that.deps.forEach { get(it.key).ensureNarrows(it, info) }
  }

  internal fun narrows(that: DependencySet, info: TypeInfo): Boolean {
    requireSameClassTable(that)
    return that.deps.all { get(it.key).narrows(it, info) }
  }

  // OTHER OPERATORS

  internal inline fun merge(
      that: DependencySet,
      merger: (Dependency, Dependency) -> Dependency,
  ): DependencySet {
    requireSameClassTable(that)
    val merged =
        (this.keys + that.keys).map {
          setOfNotNull(this.getIfPresent(it), that.getIfPresent(it)).reduce(merger)
        }
    return of(merged)
  }

  internal fun minus(that: DependencySet): DependencySet {
    requireSameClassTable(that)
    return of(this.deps - that.deps)
  }

  @PublishedApi
  internal fun requireSameClassTable(that: DependencySet) {
    if (classTable != null && that.classTable != null) {
      require(classTable === that.classTable) {
        "dependencies belong to different class tables"
      }
    }
  }

  // OTHER

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  internal fun subMapInOrder(keysInOrder: Iterable<Key>) =
      of(keysInOrder.mapNotNull(::getIfPresent))

  internal inline fun map(function: (Type) -> Type) =
      DependencySet(deps.toSetStrict { if (it is TypeDependency) it.map(function) else it })

  internal inline fun mapWithKey(function: (Key, Type) -> Type) =
      DependencySet(
          deps.toSetStrict {
            if (it is TypeDependency) it.map { type -> function(it.key, type) } else it
          }
      )

  internal fun specialize(specs: List<Expression>): DependencySet {
    // This has been a bit optimized
    val partial = matchPartial(specs)
    return of(keys.map { partial.getIfPresent(it) ?: get(it) })
  }

  internal fun replaceAt(path: DependencyPath, replacement: Dependency): DependencySet {
    val firstKey = path.keyList.first()
    if (path.keyList.size == 1) {
      require(replacement.key == firstKey)
      return of(deps.map { if (it.key == firstKey) replacement else it })
    }

    fun Type.replaceNested(): Type =
        rootClass
            .withAllDependencies(dependencies.replaceAt(path.drop(1), replacement))
            .refine(refinement)

    val first = get(firstKey)
    val narrowed =
        when (first) {
          is TypeDependency -> first.copy(boundType = first.boundType.replaceNested())
          is ComplementDependency -> {
            val domain = first.domainType.replaceNested()
            val excluded = first.excludedType glb domain
            if (excluded == null) TypeDependency(first.key, domain)
            else first.copy(domainType = domain, excludedType = excluded)
          }
          else -> error("unexpected dependency: $first")
        }
    return replaceAt(DependencyPath(firstKey), narrowed)
  }

  /**
   * For an example expression like `Foo<Bar, Qux>`, pass in `[Bar, Qux]` and Foo's base dependency
   * set. This method decides which dependencies in the dependency set each of these args should be
   * matched with. The returned dependency set will have [TypeDependency]s in the corresponding
   * order to the input expressions.
   */
  public fun matchPartial(args: List<Expression>): DependencySet {
    return of(matchPartialInOrder(args))
  }

  internal fun matchPartialInOrder(args: List<Expression>): List<Dependency> {
    val alreadyMatchedDeps = mutableSetOf<Dependency>()

    fun tryMatch(arg: Expression, dep: Dependency): Dependency? {
      if (dep in alreadyMatchedDeps) return null
      val intersection: Dependency = dep.intersect(arg) ?: return null
      alreadyMatchedDeps += dep
      return intersection
    }

    fun matchToDependency(arg: Expression): Dependency =
        deps.firstNotNullOfOrNull { tryMatch(arg, it) }
            ?: throw Exceptions.badExpression(arg, toString())

    return args.map(::matchToDependency)
  }

  internal fun concreteSubtypesSameClass(type: Type): Sequence<Type> {
    return if (isForClassType(deps)) {
      type.concreteSubclasses(getClassForClassType(deps)).map { it.classType }
    } else {
      keys.fold(sequenceOf(type)) { types, key ->
        types.flatMap { type ->
          val dependency = type.dependencies.get(key)
          if (!dependency.abstract) return@flatMap sequenceOf(type)
          when (dependency) {
            is TypeDependency -> dependency.allConcreteSpecializations()
            is ComplementDependency -> dependency.allConcreteSpecializations()
            else -> error("unexpected")
          }.map { concrete ->
            type.rootClass.withAllDependencies(
                type.dependencies.replaceAt(DependencyPath(key), concrete)
            )
          }
        }
      }
    }
  }

  internal fun singleConcreteSubtype(): DependencySet? {
    return if (isForClassType(deps)) {
      val abs: Class = getClassForClassType(deps)
      val conc = abs.allSubclasses().singleOrNull { !it.abstract }
      conc?.let { depsForClassType(it) }
    } else {
      map { it.singleConcreteSubtype() ?: return null }
    }
  }

  override fun equals(other: Any?): Boolean = other is DependencySet && deps == other.deps

  override fun hashCode(): Int = deps.hashCode()

  override fun toString(): String = "$deps"

  public data class DependencyPath(public val keyList: List<Key>) {
    internal constructor(key: Key) : this(listOf(key))

    init {
      require(keyList.any())
    }

    internal fun prepend(key: Key) = DependencyPath(listOf(key) + keyList)

    internal fun drop(i: Int) = DependencyPath(keyList.drop(i))
  }
}
