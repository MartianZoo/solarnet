package dev.martianzoo.pets.types

import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Companion.depsForClassType
import dev.martianzoo.pets.types.Dependency.Companion.getClassForClassType
import dev.martianzoo.pets.types.Dependency.Companion.isForClassType
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Dependency.TypeDependency

/**
 * An immutable keyed set containing every dependency bound of one class or type, as defined by
 * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
 *
 * Equality is key-wise and order-insensitive, while [keys] preserves declaration order for type
 * rendering. The set models the contents of a type expression's angle brackets independently of the
 * root class outside them. Combining sets from different universes throws
 * [IllegalArgumentException], implementing the universe-mismatch failure in rule T1-2.
 */
public class DependencySet private constructor(private val deps: List<Dependency>) :
    Specification<DependencySet> {

  internal companion object {
    internal fun of(deps: Iterable<Dependency>): DependencySet {
      val ordered = if (deps is List<Dependency>) deps else deps.toList()
      Dependency.validate(ordered)
      return DependencySet(ordered)
    }

    internal fun of() = DependencySet(emptyList())
  }

  /**
   * Flattens every nested dependency to its full path and structural bound class, following
   * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public fun flatten(): Map<DependencyPath, Class> {
    return deps
        .flatMap { dep: Dependency ->
          // This structural Class projection intentionally omits refinements and variable identity.
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

  /**
   * Returns the dependency at [path], traversing nested type bounds as specified by
   * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public fun at(path: DependencyPath): Dependency {
    val x: Dependency = get(path.keyList.first())
    if ((path.keyList.size) == 1) return x
    val type = (x as TypeDependency).boundType
    return type.dependencies.at(path.drop(1))
  }

  /**
   * Returns only component-targeting dependencies, excluding a class literal's represented-class
   * slot under
   * [rule T4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#4-class-literals).
   */
  public fun typeDependencies(): List<TypeDependency> = deps.filterIsInstance<TypeDependency>()

  /**
   * Enumerates every concrete target admitted by every component-targeting dependency, using
   * master-universe enumeration from
   * [rule T11-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun concreteDependencyTargets(): Sequence<GroundType> =
      deps
          .asSequence()
          .filterIsInstance<TypeDependency>()
          .flatMap(TypeDependency::allConcreteSpecializations)
          .map { it.boundType }

  /**
   * The dependency identities in declaration order, as required for full rendering by
   * [rules T3-10 and T5-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public val keys: List<Key> = List(deps.size) { deps[it].key }

  internal val representedClass: Class? =
      if (isForClassType(deps)) getClassForClassType(deps) else null

  internal val classTable: ClassTable? = deps.firstOrNull()?.boundClass?.classTable

  internal fun expressions(): List<Expression> = deps.map { it.expression }

  internal fun expressionsFull(): List<Expression> = deps.map { it.expressionFull }

  internal inline fun expressionsFull(function: (Dependency) -> Expression): List<Expression> =
      deps.map(function)

  /**
   * Returns the dependency identified by [key], failing when the key is absent ([rule
   * T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
   */
  public fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")

  /**
   * Returns the dependency identified by [key], or null when absent ([rule
   * T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
   */
  public fun getIfPresent(key: Key): Dependency? {
    var index = 0
    while (index < deps.size) {
      val dependency = deps[index]
      if (dependency.key == key) return dependency
      index++
    }
    return null
  }

  // HIERARCHY

  /**
   * Whether any bound in this set is abstract, contributing to type abstractness under
   * [rule T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  public val abstract: Boolean = run {
    var index = 0
    while (index < deps.size) {
      if (deps[index].abstract) return@run true
      index++
    }
    false
  }

  /**
   * Returns [abstract]; dependency-set abstractness is structural and does not consult [info]
   * ([rule
   * T5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types)).
   */
  override fun isAbstract(info: TypeInfo): Boolean = abstract

  internal fun activeIn(table: ClassTable): Boolean = deps.all { dependency ->
    table.isActive(dependency.boundClass) &&
        when (dependency) {
          is TypeDependency -> table.isActive(dependency.boundType)
          else -> true
        }
  }

  /**
   * Tests componentwise context-free covariance against [that] according to
   * [rules T6-2 and T6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  public fun isSubtypeOf(that: DependencySet): Boolean {
    requireSameClassTable(that)
    return that.deps.all { get(it.key).isSubtypeOf(it) }
  }

  /**
   * Tests the converse of [isSubtypeOf], including equality ([rule
   * T6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping)).
   */
  public fun isSupertypeOf(that: DependencySet): Boolean = that.isSubtypeOf(this)

  /**
   * Intersects corresponding keyed bounds, returning null if any shared bound has no intersection,
   * as required by
   * [rule T7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds).
   */
  public infix fun glb(that: DependencySet): DependencySet? {
    requireSameClassTable(that)
    return merge(that) { a, b -> (a glb b) ?: return@glb null }
  }

  /**
   * Joins corresponding keys present in both sets, dropping unshared keys as required by
   * [rule T7-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#7-bounds).
   */
  public infix fun lub(that: DependencySet): DependencySet {
    requireSameClassTable(that)
    return of(
        deps.mapNotNull { dependency ->
          that.getIfPresent(dependency.key)?.let { dependency lub it }
        }
    )
  }

  /**
   * Asserts componentwise contextual covariance against [that], forwarding [info] to refinements
   * under
   * [rules T6-2 and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun ensureNarrows(that: DependencySet, info: TypeInfo) {
    requireSameClassTable(that)
    that.deps.forEach { get(it.key).ensureNarrows(it, info) }
  }

  /**
   * Tests componentwise contextual covariance against [that], forwarding [info] to refinements
   * under
   * [rules T6-2 and T8-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#6-subtyping).
   */
  override fun narrows(that: DependencySet, info: TypeInfo): Boolean {
    requireSameClassTable(that)
    return that.deps.all { get(it.key).narrows(it, info) }
  }

  // OTHER OPERATORS

  internal inline fun merge(
      that: DependencySet,
      merger: (Dependency, Dependency) -> Dependency,
  ): DependencySet {
    requireSameClassTable(that)
    val merged = buildList {
      this@DependencySet.deps.forEach { dependency ->
        add(that.getIfPresent(dependency.key)?.let { merger(dependency, it) } ?: dependency)
      }
      that.deps.forEach { dependency ->
        if (this@DependencySet.getIfPresent(dependency.key) == null) add(dependency)
      }
    }
    return DependencySet(merged)
  }

  internal fun minus(that: DependencySet): DependencySet {
    requireSameClassTable(that)
    return DependencySet(this.deps - that.deps)
  }

  @PublishedApi
  internal fun requireSameClassTable(that: DependencySet) {
    if (classTable != null && that.classTable != null) {
      require(classTable === that.classTable) { "dependencies belong to different class tables" }
    }
  }

  // OTHER

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  internal fun subMapInOrder(keysInOrder: Iterable<Key>): DependencySet {
    if (keysInOrder == keys) return this
    return DependencySet(keysInOrder.mapNotNull(::getIfPresent))
  }

  private inline fun map(function: (GroundType) -> GroundType) =
      DependencySet(deps.map { if (it is TypeDependency) it.map(function) else it })

  internal inline fun mapWithKey(function: (Key, GroundType) -> GroundType) =
      DependencySet(
          deps.map {
            if (it is TypeDependency) it.map { type -> function(it.key, type) } else it
          }
      )

  internal fun specialize(specs: List<Expression>): DependencySet {
    // This has been a bit optimized
    val partial = matchPartial(specs)
    return of(deps.map { partial.getIfPresent(it.key) ?: it })
  }

  internal fun replaceAt(path: DependencyPath, replacement: Dependency): DependencySet {
    val firstKey = path.keyList.first()
    if (path.keyList.size == 1) {
      require(replacement.key == firstKey)
      return DependencySet(deps.map { if (it.key == firstKey) replacement else it })
    }

    fun GroundType.replaceNested(): GroundType =
        rootClass
            .withAllDependencies(dependencies.replaceAt(path.drop(1), replacement))
            .refine(refinement)

    val first = get(firstKey)
    val narrowed = (first as TypeDependency).copy(boundType = first.boundType.replaceNested())
    return replaceAt(DependencyPath(firstKey), narrowed)
  }

  /**
   * Greedily matches [args] left to right to distinct compatible dependencies, following
   * [rules T3-4 and T3-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   *
   * The returned dependencies are ordered like [args], not like this set; unmatched dependencies
   * are omitted. An argument with no compatible untaken dependency is an expression error.
   */
  public fun matchPartial(args: List<Expression>): DependencySet {
    return of(matchPartialInOrder(args))
  }

  internal fun matchPartialInOrder(args: List<Expression>): List<Dependency> {
    val alreadyMatched = mutableSetOf<Dependency>()

    fun matchToDependency(arg: Expression): Dependency {
      deps.forEach { dependency ->
        if (dependency !in alreadyMatched) {
          dependency.intersect(arg)?.let {
            alreadyMatched += dependency
            return it
          }
        }
      }
      throw Exceptions.badExpression(arg, toString())
    }

    return args.map(::matchToDependency)
  }

  internal fun concreteSubtypesSameClass(type: GroundType): Sequence<GroundType> {
    return if (isForClassType(deps)) {
      type.concreteSubclasses(getClassForClassType(deps)).map { it.classType }
    } else {
      keys.fold(sequenceOf(type)) { types, key ->
        types.flatMap { type ->
          val dependency = type.dependencies.get(key)
          if (!dependency.abstract) return@flatMap sequenceOf(type)
          (dependency as TypeDependency).allConcreteSpecializations().map { concrete ->
            type.rootClass.withAllDependencies(
                type.dependencies.replaceAt(DependencyPath(key), concrete)
            )
          }
        }
      }
    }
  }

  internal fun concreteSubtypesSameClass(
      type: GroundType,
      table: ClassTable,
  ): Sequence<GroundType> {
    return if (isForClassType(deps)) {
      table.allSubclasses(getClassForClassType(deps)).asSequence().filterNot(Class::abstract).map {
        it.classType
      }
    } else {
      keys.fold(sequenceOf(type)) { types, key ->
        types.flatMap { candidate ->
          val dependency = candidate.dependencies.get(key)
          if (!dependency.abstract) return@flatMap sequenceOf(candidate)
          val concreteDependencies =
              (dependency as TypeDependency).let {
                table.allConcreteSubtypes(it.boundType).map { type -> it.copy(boundType = type) }
              }
          concreteDependencies.map { concrete ->
            candidate.rootClass.withAllDependencies(
                candidate.dependencies.replaceAt(DependencyPath(key), concrete)
            )
          }
        }
      }
    }
  }

  internal fun concreteSubtypesSameClass(
      type: GroundType,
      dependencyTargets: (Type) -> Sequence<Type>,
  ): Sequence<GroundType> {
    return if (isForClassType(deps)) {
      dependencyTargets(type).map { it.groundType }
    } else {
      keys.fold(sequenceOf(type)) { types, key ->
        types.flatMap { candidate ->
          val dependency = candidate.dependencies.get(key)
          val concreteDependencies =
              (dependency as TypeDependency).let {
                dependencyTargets(it.boundType).map { target ->
                  it.copy(boundType = target.groundType)
                }
              }
          concreteDependencies.map { concrete ->
            candidate.rootClass.withAllDependencies(
                candidate.dependencies.replaceAt(DependencyPath(key), concrete)
            )
          }
        }
      }
    }
  }

  internal fun singleConcreteSubtype(info: TypeInfo): DependencySet? {
    if (isForClassType(deps)) {
      val abstractClass = getClassForClassType(deps)
      val concreteClass = abstractClass.allSubclasses().singleOrNull { !it.abstract }
      return concreteClass?.let { depsForClassType(it) }
    }

    return of(
        deps.map { dependency ->
          (dependency as TypeDependency).boundType.singleConcreteSubtype(info)?.let {
            dependency.copy(boundType = it)
          } ?: return null
        }
    )
  }

  internal fun singleConcreteSubtype(info: TypeInfo, table: ClassTable): DependencySet? {
    if (isForClassType(deps)) {
      val abstractClass = getClassForClassType(deps)
      val concreteClass = table.allSubclasses(abstractClass).singleOrNull { !it.abstract }
      return concreteClass?.let { depsForClassType(it) }
    }

    return of(
        deps.map { dependency ->
          (dependency as TypeDependency).let {
            table.singleConcreteSubtype(it.boundType, info)?.let { type ->
              it.copy(boundType = type)
            }
          } ?: return null
        }
    )
  }

  /**
   * Implements key-wise, order-insensitive equality required by
   * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  override fun equals(other: Any?): Boolean =
      other is DependencySet &&
          deps.size == other.deps.size &&
          deps.all { dependency -> other.getIfPresent(dependency.key) == dependency }

  /**
   * Hashes the key-wise contents consistently with the order-insensitive equality of
   * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  override fun hashCode(): Int = deps.sumOf(Dependency::hashCode)

  /**
   * Renders the keyed bounds for diagnostics; type expressions use the ordered forms specified by
   * [rules T5-4 and T5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  override fun toString(): String = "$deps"

  /**
   * A nonempty route through nested dependency keys, used by the flattening and path lookup
   * operations of
   * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   *
   * @constructor Creates a path from a nonempty outermost-to-innermost [keyList], as used by
   *   [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public data class DependencyPath(
      /**
       * The outermost-to-innermost dependency keys forming the path described by
       * [rule T3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
       */
      public val keyList: List<Key>,
  ) {
    internal constructor(key: Key) : this(listOf(key))

    init {
      require(keyList.any())
    }

    internal fun prepend(key: Key) = DependencyPath(listOf(key) + keyList)

    internal fun drop(i: Int) = DependencyPath(keyList.drop(i))
  }
}
