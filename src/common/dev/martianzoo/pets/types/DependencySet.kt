package dev.martianzoo.pets.types

import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Companion.depsForClassType
import dev.martianzoo.pets.types.Dependency.Companion.getClassForClassType
import dev.martianzoo.pets.types.Dependency.Companion.isForClassType
import dev.martianzoo.pets.types.Dependency.ComplementDependency
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Dependency.TypeDependency
import dev.martianzoo.pets.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
public class DependencySet private constructor(private val deps: Set<Dependency>) :
    Specification<DependencySet> {

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

  internal val representedClass: Class? =
      if (isForClassType(deps)) getClassForClassType(deps) else null

  internal val classTable: ClassTable? = deps.firstOrNull()?.boundClass?.classTable

  internal fun expressions(): List<Expression> = deps.map { it.expression }

  internal fun expressionsFull(): List<Expression> = deps.map { it.expressionFull }

  internal inline fun expressionsFull(function: (Dependency) -> Expression): List<Expression> =
      deps.map(function)

  public fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")

  public fun getIfPresent(key: Key): Dependency? = deps.firstOrNull { it.key == key }

  // HIERARCHY

  public val abstract: Boolean = deps.any { it.abstract }

  override fun isAbstract(info: TypeInfo): Boolean = abstract

  internal fun activeIn(table: ClassTable): Boolean = deps.all { dependency ->
    table.isActive(dependency.boundClass) &&
        when (dependency) {
          is TypeDependency -> table.isActive(dependency.boundType)
          is ComplementDependency -> table.isActive(dependency.domainType)
          else -> true
        }
  }

  public fun isSubtypeOf(that: DependencySet): Boolean {
    requireSameClassTable(that)
    return that.deps.all { get(it.key).isSubtypeOf(it) }
  }

  /** Returns whether this dependency set is a supertype of [that], including equality. */
  public fun isSupertypeOf(that: DependencySet): Boolean = that.isSubtypeOf(this)

  public infix fun glb(that: DependencySet): DependencySet? {
    requireSameClassTable(that)
    return merge(that) { a, b -> (a glb b) ?: return@glb null }
  }

  public infix fun lub(that: DependencySet): DependencySet {
    requireSameClassTable(that)
    val keys = keys.intersect(that.keys)
    return of(keys.map { this.get(it) lub that.get(it) })
  }

  override fun ensureNarrows(that: DependencySet, info: TypeInfo) {
    requireSameClassTable(that)
    that.deps.forEach { get(it.key).ensureNarrows(it, info) }
  }

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
      require(classTable === that.classTable) { "dependencies belong to different class tables" }
    }
  }

  // OTHER

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  internal fun subMapInOrder(keysInOrder: Iterable<Key>) =
      of(keysInOrder.mapNotNull(::getIfPresent))

  private inline fun map(function: (Type) -> Type) =
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

  internal fun concreteSubtypesSameClass(type: Type, table: ClassTable): Sequence<Type> {
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
              when (dependency) {
                is TypeDependency ->
                    table.allConcreteSubtypes(dependency.boundType).map {
                      dependency.copy(boundType = it)
                    }
                is ComplementDependency ->
                    table
                        .allConcreteSubtypes(dependency.domainType)
                        .filterNot { it.isSubtypeOf(dependency.excludedType) }
                        .map { TypeDependency(dependency.key, it) }
                else -> error("unexpected dependency: $dependency")
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
          when (dependency) {
            is TypeDependency ->
                dependency.boundType.singleConcreteSubtype(info)?.let {
                  dependency.copy(boundType = it)
                }
            is ComplementDependency ->
                dependency.domainType
                    .allConcreteSubtypes()
                    .filter { dependency.matches(it, info) }
                    .map { TypeDependency(dependency.key, it) }
                    .singleOrNull()
            else -> error("unexpected dependency: $dependency")
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
          when (dependency) {
            is TypeDependency ->
                table.singleConcreteSubtype(dependency.boundType, info)?.let {
                  dependency.copy(boundType = it)
                }
            is ComplementDependency ->
                table
                    .allConcreteSubtypes(dependency.domainType)
                    .filter { dependency.matches(it, info) }
                    .map { TypeDependency(dependency.key, it) }
                    .singleOrNull()
            else -> error("unexpected dependency: $dependency")
          } ?: return null
        }
    )
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
