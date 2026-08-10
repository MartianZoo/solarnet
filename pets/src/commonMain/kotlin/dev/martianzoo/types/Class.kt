package dev.martianzoo.types

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.types.Dependency.Companion.depsForClassType
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.types.DependencySet.DependencyPath
import dev.martianzoo.util.Hierarchical
import dev.martianzoo.util.Hierarchical.Companion.glb
import dev.martianzoo.util.toSetStrict

/**
 * A class that has been loaded by a [ClassLoader] based on a [ClassDeclaration]. Each loader has
 * its own separate table of [Class]es. While a declaration is inert data, this type provides its
 * resolved hierarchy, dependencies, and types. The source [declaration] remains available for
 * non-type-system consumers.
 */
public class Class
internal constructor(
    /** The class declaration this class was loaded from. */
    public val declaration: ClassDeclaration,

    /** The class loader used while constructing this class. */
    private val loader: ClassLoader,

    /** Whether this authority-known class is inactive in this particular class table. */
    public val phantom: Boolean = false,

    /** This class's superclasses that are exactly one step away; empty only for `Component`. */
    public val directSuperclasses: List<Class> = superclasses(declaration, loader, phantom),
) : HasClassName, Hierarchical<Class> {

  /** The table containing this class. */
  public val classTable: ClassTable = loader

  /** The name of this class, in UpperCamelCase. */
  override val className: ClassName = declaration.className.also { require(it != THIS) }

  init {
    if (!phantom && directSuperclasses.any(Class::phantom)) {
      throw PetException(
          "$className cannot extend inactive class(es): " +
              directSuperclasses.filter(Class::phantom).joinToString { "${it.className}" }
      )
    }
    if (directSuperclasses.any { !it.abstract }) {
      throw PetException(
          "$className cannot extend concrete class(es): " +
              directSuperclasses.filterNot { it.abstract }.joinToString { "${it.className}" }
      )
    }
  }

  /** A textual explanation for this class. */
  public val docstring: String? by declaration::docstring

  // HIERARCHY

  override val abstract: Boolean by declaration::abstract

  override fun isSubtypeOf(that: Class): Boolean {
    requireSameClassTable(that)
    val bits = abstractSupertypeBits ?: return that in allSuperclasses()
    if (this === that) return true
    return that.superclassBit >= 0 && bits.hasBit(that.superclassBit)
  }

  private var superclassBit: Int = -1
  private var abstractSupertypeBits: BigInt? = null

  /** Compiles the hierarchy after the complete authority-known class table has been loaded. */
  internal fun initializeSubclassBits(superclassBits: Map<Class, Int>) {
    if (abstractSupertypeBits != null) return

    directSuperclasses.forEach { it.initializeSubclassBits(superclassBits) }
    val ownBit = superclassBits[this]
    check(ownBit == null || abstract)
    superclassBit = ownBit ?: -1

    var bits = ownBit?.let(BigInt::bit) ?: BigInt.ZERO
    directSuperclasses.forEach { superclass ->
      bits = bits or checkNotNull(superclass.abstractSupertypeBits)
    }
    abstractSupertypeBits = bits
  }

  override fun glb(that: Class): Class? =
      when {
        this.isSubtypeOf(that) -> this
        that.isSubtypeOf(this) -> that
        else -> {
          allSubclasses.singleOrNull {
            it.isIntersectionType() &&
                this in it.directSuperclasses &&
                that in it.directSuperclasses
          }
        }
      }

  override fun lub(that: Class): Class {
    requireSameClassTable(that)
    val commonSupers: Set<Class> = this.allSuperclasses.intersect(that.allSuperclasses)
    val supersOfSupers: Set<Class> = commonSupers.flatMap { it.properSuperclasses() }.toSet()
    val candidates: Set<Class> = commonSupers - supersOfSupers
    // This is a weird and stupid heuristic, but does it really matter which one we pick?
    return candidates.maxBy {
      it.dependencies.typeDependencies().size * 100 + it.allSuperclasses.size
    }
  }

  override fun ensureNarrows(that: Class, info: TypeInfo) {
    if (!isSubtypeOf(that))
        throw NarrowingException("${this.className} is not a subclass of ${that.className}")
  }

  private fun requireSameClassTable(that: Class) {
    require(classTable === that.classTable) {
      "$className and ${that.className} belong to different class tables"
    }
  }

  private val sups by declaration::supertypes

  private fun replaceThis(expression: Expression): Expression =
      replaceThisExpressionsWith(className.expression).transform(expression)

  private fun directSupertypes(): Set<Type> =
      when {
        className == COMPONENT -> setOf()
        sups.none() -> setOf(loader.componentClass.baseType)
        else -> sups.toSetStrict { loader.resolve(replaceThis(it)) }
      }

  private val allSuperclasses: Set<Class> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  /** Every class `c` for which `c.isSuperclassOf(this)` is true, including this class itself. */
  public fun allSuperclasses(): Set<Class> = allSuperclasses

  internal fun properSuperclasses(): Set<Class> = allSuperclasses() - this

  private val allSubclasses: Set<Class> by lazy {
    if (phantom) emptySet() else loader.properSubclassesOf(this) + this
  }

  /** Every class `c` for which `c.isSubclassOf(this)` is true, including this class itself. */
  public fun allSubclasses(): Set<Class> = allSubclasses

  public fun directSubclasses(): Set<Class> = loader.directSubclassesOf(this)

  /**
   * Whether this class serves as the intersection type of its full set of [directSuperclasses];
   * that is, no other [Class] in this [ClassTable] is a subclass of all of them unless it is also a
   * subclass of `this`. An example is `OwnedTile`; since components like the `Landlord` award count
   * `OwnedTile` components, it would be a bug if a component like `CommercialDistrictTile` (which
   * is both an `Owned` and a `Tile`) forgot to also extend `OwnedTile`.
   */
  public fun isIntersectionType(): Boolean = intersectionType

  private val intersectionType: Boolean by lazy {
    directSuperclasses.size >= 2 &&
        loader
            .allClasses()
            .filter { klass -> directSuperclasses.all(klass::isSubtypeOf) }
            .all(::isSupertypeOf)
  }

  // DEPENDENCIES

  /** The dependency positions whose values are bound to the inheriting class. */
  private val selfBindings: Set<DependencyPath> by lazy {
    val inherited = directSuperclasses.flatMap { it.selfBindings }
    val declared = sups.flatMap { sourceSupertype ->
      val superclass = loader.getClass(sourceSupertype.className)
      val arguments = sourceSupertype.arguments
      val matched = superclass.dependencies.matchPartialInOrder(arguments.map(::replaceThis))
      arguments.zip(matched).flatMap { (argument, dependency) ->
        selfBindingsIn(argument, dependency, listOf(dependency.key))
      }
    }
    (inherited + declared).toSet()
  }

  private fun selfBindingsIn(
      expression: Expression,
      dependency: Dependency,
      path: List<Key>,
  ): List<DependencyPath> {
    if (expression == THIS.expression) return listOf(DependencyPath(path))
    if (expression.arguments.isEmpty()) return listOf()

    val dependencies =
        when (dependency) {
          is TypeDependency -> dependency.boundType.dependencies
          else -> return listOf()
        }
    val matched = dependencies.matchPartialInOrder(expression.arguments.map(::replaceThis))
    return expression.arguments.zip(matched).flatMap { (argument, nestedDependency) ->
      selfBindingsIn(argument, nestedDependency, path + nestedDependency.key)
    }
  }

  private fun Type.bindSelfAt(paths: List<List<Key>>): Expression {
    val pathsByKey = paths.groupBy { it.first() }
    val arguments = dependencies.expressionsFull { dependency ->
      val remainingPaths = pathsByKey[dependency.key]?.map { it.drop(1) }.orEmpty()
      when {
        remainingPaths.isEmpty() -> dependency.expressionFull
        remainingPaths.any { it.isEmpty() } -> this@Class.className.expression
        dependency is TypeDependency -> dependency.boundType.bindSelfAt(remainingPaths)
        else -> error("can't bind self within $dependency")
      }
    }
    return expressionFull.replaceArguments(arguments)
  }

  private val inheritedDeps: DependencySet by lazy {
    val inherited =
        directSupertypes().map { supertype ->
          val superclass = supertype.rootClass
          val pathsByKey = superclass.selfBindings.groupBy { it.keyList.first() }
          supertype.dependencies.mapWithKey { key, boundType ->
            val paths = pathsByKey[key]?.map { it.keyList.drop(1) }.orEmpty()
            if (paths.isEmpty()) {
              boundType
            } else {
              loader.resolve(boundType.bindSelfAt(paths))
            }
          }
        }
    glb(inherited) ?: DependencySet.of()
  }

  private val declaredDeps by lazy {
    DependencySet.of(
        declaration.dependencies.mapIndexed { index, expression ->
          TypeDependency(Key(className, index), loader.resolve(expression))
        }
    )
  }

  // `by lazy` enables dependency cycles, yay
  public val dependencies: DependencySet by lazy {
    val result =
        if (className == CLASS) {
          depsForClassType(loader.componentClass)
        } else {
          inheritedDeps.merge(declaredDeps) { _, _ -> error("unexpected") }
        }
    if (!phantom && result.phantom) {
      throw PetException("$className has an inactive dependency type")
    }
    result
  }

  private data class DependencyLink(
      val expressions: Set<Expression>,
      val paths: Set<DependencyPath>,
  )

  private val declaredDependencyLinks: List<DependencyLink> by lazy {
    val occurrences = mutableMapOf<Pair<Expression, Key>, MutableSet<DependencyPath>>()

    fun collectSpecializations(expression: Expression, prefix: List<Key>) {
      if (expression.arguments.isEmpty()) return
      val dependencySet = loader.load(expression.className).dependencies
      val resolvedArguments =
          expression.arguments.map(
              replacer(THIS, className)::transform,
          )
      val matchedDependencies = dependencySet.matchPartialInOrder(resolvedArguments)
      expression.arguments.zip(matchedDependencies).forEach { (argument, dependency) ->
        val path = DependencyPath(prefix + dependency.key)
        if (argument.simple && argument.className != THIS) {
          occurrences.getOrPut(argument to dependency.key, ::mutableSetOf) += path
        }
        collectSpecializations(argument, path.keyList)
      }
    }

    declaration.dependencies.forEachIndexed { index, expression ->
      collectSpecializations(expression, listOf(Key(className, index)))
    }
    declaration.supertypes.forEach { collectSpecializations(it, listOf()) }

    occurrences.mapNotNull { (binding, paths) ->
      if (paths.size < 2) null else DependencyLink(setOf(binding.first), paths)
    }
  }

  private val dependencyLinks: List<DependencyLink> by lazy {
    val merged = mutableListOf<DependencyLink>()
    (directSuperclasses.flatMap { it.dependencyLinks } + declaredDependencyLinks).forEach { link ->
      val overlapping = merged.filter { it.paths.any(link.paths::contains) }
      merged.removeAll(overlapping)
      merged +=
          DependencyLink(
              expressions = link.expressions + overlapping.flatMap { it.expressions },
              paths = link.paths + overlapping.flatMap { it.paths },
          )
    }
    merged
  }

  private fun linkError(link: DependencyLink, dependencies: DependencySet): Nothing =
      error(
          "linked ${link.expressions.joinToString()} dependencies disagree in " +
              className.of(dependencies.expressionsFull())
      )

  private fun normalizeLinkedDependencies(original: DependencySet): DependencySet {
    var dependencies = original
    var changed: Boolean
    do {
      changed = false
      dependencyLinks.forEach { link ->
        val linkedDependencies = link.paths.map(dependencies::at)
        val intersection = linkedDependencies.reduce { left, right ->
          (left glb right) ?: linkError(link, dependencies)
        }
        link.paths.forEach { path ->
          if (dependencies.at(path) != intersection) {
            dependencies = dependencies.replaceAt(path, intersection)
            changed = true
          }
        }
      }
    } while (changed)
    return dependencies
  }

  internal fun requireLinksSatisfied(dependencies: DependencySet) {
    dependencyLinks.forEach { link ->
      if (link.paths.map(dependencies::at).distinct().size > 1) {
        linkError(link, dependencies)
      }
    }
  }

  // GETTING TYPES

  public fun withAllDependencies(deps: DependencySet): Type =
      Type(this, normalizeLinkedDependencies(deps.subMapInOrder(dependencies.keys)))

  /** Least upper bound of all types with rootClass==this */
  public val baseType: Type by lazy { withAllDependencies(dependencies) }

  public val defaultType: Type by lazy {
    val templateDependencies =
        dependencies.merge(defaults.allUsages.dependencies) { _, default -> default }
    withAllDependencies(templateDependencies)
  }

  public fun specialize(specs: List<Expression>): Type = baseType.specialize(specs)

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  internal val classType: Type by lazy {
    loader.classClass.withAllDependencies(depsForClassType(this))
  }

  public fun concreteTypes(): Sequence<Type> = baseType.concreteSubtypesSameClass()

  internal val defaultsDecl by declaration::defaultsDeclaration

  public val defaults: Defaults by lazy { Defaults.forClass(this) }

  override fun equals(other: Any?): Boolean =
      other is Class && other.className == className && other.loader == loader

  override fun hashCode(): Int = className.hashCode() xor loader.hashCode()

  override fun toString(): String = "$className"

  private companion object {
    fun superclasses(
        declaration: ClassDeclaration,
        loader: ClassLoader,
        phantom: Boolean,
    ): List<Class> {
      return declaration.supertypes
          .classNames()
          .also { require(COMPONENT !in it) }
          .ifEmpty { listOf(COMPONENT) }
          .map { loader.loadRelated(it, active = !phantom) }
    }
  }
}
