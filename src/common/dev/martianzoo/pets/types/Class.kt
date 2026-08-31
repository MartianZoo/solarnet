package dev.martianzoo.pets.types

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.SystemClasses.ANYONE
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode.Companion.replacer
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.PropertyValue.AbsentRequirementValue
import dev.martianzoo.pets.ast.PropertyValue.OptionalRequirementType
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Companion.split
import dev.martianzoo.pets.ast.withTypeVariables
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Dependency.Companion.depsForClassType
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Dependency.TypeDependency
import dev.martianzoo.pets.types.DependencySet.DependencyPath
import dev.martianzoo.pets.util.invoke
import dev.martianzoo.pets.util.toSetStrict

/**
 * A Catalog-scoped class compiled from a [ClassDeclaration]. While a declaration is inert data,
 * this type provides its resolved hierarchy, dependencies, and types. The source [declaration]
 * remains available for non-type-system consumers.
 */
public class Class
internal constructor(
    /** The class declaration this class was loaded from. */
    public val declaration: ClassDeclaration,

    /** The class loader used while constructing this class. */
    private val loader: ClassLoader,

    /** This class's superclasses that are exactly one step away; empty only for `Component`. */
    public val directSuperclasses: List<Class> = superclasses(declaration, loader),
) : HasClassName, Specification<Class> {

  /** The master universe containing this class. */
  // TODO: Contract this temporary tfm-canon seam.
  public val classTable: ClassTable = loader

  /** The name of this class, in UpperCamelCase. */
  override val className: ClassName = declaration.className.also { require(it != THIS) }

  init {
    if (directSuperclasses.any { !it.abstract }) {
      throw PetException(
          "$className cannot extend concrete class(es): " +
              directSuperclasses.filterNot { it.abstract }.joinToString { "${it.className}" }
      )
    }
  }

  /** A textual explanation for this class. */
  public val docstring: String?
    get() = declaration.docstring

  private val resolvedProperties: Map<PropertyName, PropertyFact> = resolveProperties()

  /** Every property bound or value supplied by this class and its supertypes. */
  public val properties: Map<PropertyName, PropertyValue> = resolvedProperties.mapValues {
    it.value.value
  }

  init {
    if (!declaration.abstract) {
      val abstractProperties = properties.filterValues { it.abstract }.keys
      if (abstractProperties.isNotEmpty()) {
        throw PetException(
            "$className is concrete but has abstract properties: " +
                abstractProperties.joinToString()
        )
      }
    }
  }

  private fun resolveProperties(): Map<PropertyName, PropertyFact> {
    val inherited = linkedMapOf<PropertyName, PropertyFact>()
    directSuperclasses.forEach { superclass ->
      superclass.resolvedProperties.forEach { (name, incoming) ->
        val existing = inherited[name]
        inherited[name] =
            when {
              existing == null || incoming == existing -> incoming
              existing.origin != incoming.origin ->
                  throw PetException(
                      "$className inherits distinct properties named $name from " +
                          "${existing.origin} and ${incoming.origin}"
                  )
              existing.lineage.isPrefixOf(incoming.lineage) -> incoming
              incoming.lineage.isPrefixOf(existing.lineage) -> existing
              else ->
                  throw PetException(
                      "$className inherits divergent narrowings for $name from " +
                          "${existing.source} and ${incoming.source}"
                  )
            }
      }
    }

    declaration.properties.forEach { (name, declared) ->
      when (val inheritedFact = inherited[name]) {
        null -> inherited[name] = PropertyFact(listOf(className), declared)
        else -> {
          val inheritedValue = inheritedFact.value
          if (!inheritedValue.abstract) {
            throw PetException(
                "$className cannot override inherited property $name = $inheritedValue"
            )
          }
          if (!declared.narrows(inheritedValue, TypeInfo.NoGameState)) {
            throw PetException(
                "$className cannot narrow property $name = $inheritedValue with $declared"
            )
          }
          inherited[name] =
              inheritedFact.copy(lineage = inheritedFact.lineage + className, value = declared)
        }
      }
    }
    return if (declaration.abstract) {
      inherited
    } else {
      inherited.mapValues { (_, fact) ->
        if (fact.value === OptionalRequirementType) {
          fact.copy(value = AbsentRequirementValue)
        } else {
          fact
        }
      }
    }
  }

  private data class PropertyFact(
      val lineage: List<ClassName>,
      val value: PropertyValue,
  ) {
    val origin: ClassName
      get() = lineage.first()

    val source: ClassName
      get() = lineage.last()
  }

  private fun <T> List<T>.isPrefixOf(other: List<T>): Boolean =
      size <= other.size && indices.all { this[it] == other[it] }

  // HIERARCHY

  public val abstract: Boolean
    get() = declaration.abstract

  override fun isAbstract(info: TypeInfo): Boolean = abstract

  public fun isSubtypeOf(that: Class): Boolean {
    requireSameClassTable(that)
    val bits = abstractSupertypeBits ?: return that in allSuperclasses()
    if (this === that) return true
    return that.superclassBit >= 0 && bits.hasBit(that.superclassBit)
  }

  private var superclassBit: Int = -1
  private var abstractSupertypeBits: BigInt? = null

  /** Compiles the hierarchy after the complete catalog-known class table has been loaded. */
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

  public infix fun glb(that: Class): Class? =
      when {
        this.isSubtypeOf(that) -> this
        that.isSubtypeOf(this) -> that
        else -> {
          allSubclasses().singleOrNull {
            it.isIntersectionType() &&
                this in it.directSuperclasses &&
                that in it.directSuperclasses
          }
        }
      }

  public infix fun lub(that: Class): Class {
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

  /** Returns whether this class is a supertype of [that], including equality. */
  public fun isSupertypeOf(that: Class): Boolean = that.isSubtypeOf(this)

  private fun requireSameClassTable(that: Class) {
    require(classTable === that.classTable) {
      "$className and ${that.className} belong to different class tables"
    }
  }

  private val sups: Set<Expression>
    get() = declaration.supertypes

  private fun replaceThis(expression: Expression): Expression =
      replaceThisExpressionsWith(className.expression).transformExpression(expression)

  private fun directSupertypes(): Set<GroundType> =
      when {
        className == COMPONENT -> setOf()
        sups.none() -> setOf(loader.componentClass.baseType)
        else -> sups.toSetStrict { loader.resolve(replaceThis(it)) }
      }

  private val allSuperclasses: Set<Class> =
      (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()

  /** Every independently enforced invariant inherited by this concrete class. */
  public val invariants: Set<Requirement> =
      if (abstract) emptySet()
      else allSuperclasses.flatMap { split(it.declaration.invariants) }.toSet()

  /** Every class `c` for which `c.isSuperclassOf(this)` is true, including this class itself. */
  public fun allSuperclasses(): Set<Class> = allSuperclasses

  internal fun properSuperclasses(): Set<Class> = allSuperclasses() - this

  /** Every class `c` for which `c.isSubclassOf(this)` is true, including this class itself. */
  public fun allSubclasses(): Set<Class> = loader.allSubclassesOf(this)

  public fun directSubclasses(): Set<Class> = loader.directSubclassesOf(this)

  /**
   * Whether this class serves as the intersection type of its full set of [directSuperclasses];
   * that is, no other [Class] in this [ClassTable] is a subclass of all of them unless it is also a
   * subclass of `this`. An example is `OwnedTile`; since components like the `Landlord` award count
   * `OwnedTile` components, it would be a bug if a component like `CommercialDistrict_SpecialTile`
   * (which is both an `Owned` and a `Tile`) forgot to also extend `OwnedTile`.
   */
  public fun isIntersectionType(): Boolean = intersectionType()

  private val intersectionType: Lazy<Boolean> = lazy {
    directSuperclasses.size >= 2 &&
        loader
            .allClasses()
            .filter { klass -> directSuperclasses.all(klass::isSubtypeOf) }
            .all(::isSupertypeOf)
  }
  // DEPENDENCIES

  /** The dependency positions whose values are bound to the inheriting class. */
  private val selfBindings: Lazy<Set<DependencyPath>> = lazy {
    val inherited = directSuperclasses.flatMap { it.selfBindings() }
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

  private fun GroundType.bindSelfAt(paths: List<List<Key>>): Expression {
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

  private val inheritedDeps: Lazy<DependencySet> = lazy {
    val inherited =
        directSupertypes().map { supertype ->
          val superclass = supertype.rootClass
          val pathsByKey = superclass.selfBindings().groupBy { it.keyList.first() }
          supertype.dependencies.mapWithKey { key, boundType ->
            val paths = pathsByKey[key]?.map { it.keyList.drop(1) }.orEmpty()
            if (paths.isEmpty()) {
              boundType
            } else {
              loader.resolve(boundType.bindSelfAt(paths))
            }
          }
        }
    inherited.reduceOrNull { left, right -> (left glb right)!! } ?: DependencySet.of()
  }
  private val declaredDeps: Lazy<DependencySet> = lazy {
    DependencySet.of(
        declaration.dependencies.mapIndexed { index, expression ->
          TypeDependency(Key(className, index), loader.resolve(expression))
        }
    )
  }
  // Laziness enables dependency cycles.
  private val dependenciesLazy = lazy {
    val result =
        if (className == CLASS) {
          depsForClassType(loader.componentClass)
        } else {
          inheritedDeps().merge(declaredDeps()) { _, _ -> error("unexpected") }
        }
    result
  }
  public val dependencies: DependencySet
    get() = dependenciesLazy.value

  private data class DependencyEquality(
      val expressions: Set<Expression>,
      val paths: Set<DependencyPath>,
  )

  /** Whether this direct dependency is constrained equal to another header dependency. */
  public fun isEqualityConstrainedDependency(key: Key): Boolean =
      dependencyEqualities().any {
        DependencyPath(key) in it.paths
      }

  private fun equalityError(equality: DependencyEquality, dependencies: DependencySet): Nothing =
      error(
          "Type-variable ${equality.expressions.joinToString()} dependencies disagree in " +
              className.of(dependencies.expressionsFull())
      )

  private fun normalizeVariableEqualities(original: DependencySet): DependencySet {
    var dependencies = original
    var changed: Boolean
    do {
      changed = false
      dependencyEqualities().forEach { equality ->
        val occurrences = equality.paths.map(dependencies::at)
        val intersection = occurrences.reduce { left, right ->
          (left glb right) ?: equalityError(equality, dependencies)
        }
        equality.paths.forEach { path ->
          if (dependencies.at(path) != intersection) {
            dependencies = dependencies.replaceAt(path, intersection)
            changed = true
          }
        }
      }
    } while (changed)
    return dependencies
  }

  internal fun requireVariableEqualitiesSatisfied(dependencies: DependencySet) {
    dependencyEqualities().forEach { equality ->
      if (equality.paths.map(dependencies::at).distinct().size > 1) {
        equalityError(equality, dependencies)
      }
    }
  }

  private data class HeaderOccurrence(
      val expression: Expression,
      val path: DependencyPath,
      val region: Int,
      val ordinal: Int,
  )

  private data class HeaderVariableBinding(
      val variable: TypeVariable,
      val aliases: Set<TypeVariable>,
      val paths: Set<DependencyPath>,
      val headerExpressions: Set<Expression>,
  )

  private val headerOccurrences: Lazy<List<HeaderOccurrence>> = lazy {
    var ordinal = 0
    buildList {
      fun eligible(expression: Expression): Boolean =
          expression.className != THIS &&
              runCatching { loader.resolve(expression.uncomplemented()).abstract }
                  .getOrDefault(false)

      fun collectArguments(expression: Expression, prefix: List<Key>, region: Int) {
        if (expression.arguments.isEmpty()) return
        val dependencySet = loader.load(expression.className).dependencies
        val arguments = expression.arguments.map(replacer(THIS, className)::transformExpression)
        val matched = dependencySet.matchPartialInOrder(arguments)
        expression.arguments.zip(matched).forEach { (argument, dependency) ->
          val path = DependencyPath(prefix + dependency.key)
          if (eligible(argument)) add(HeaderOccurrence(argument, path, region, ordinal++))
          collectArguments(argument, path.keyList, region)
        }
      }

      declaration.dependencies.forEachIndexed { index, expression ->
        val path = DependencyPath(Key(className, index))
        if (eligible(expression)) add(HeaderOccurrence(expression, path, index, ordinal++))
        collectArguments(expression, path.keyList, index)
      }
      declaration.supertypes.forEachIndexed { index, expression ->
        collectArguments(expression, emptyList(), declaration.dependencies.size + index)
      }
    }
  }

  private val headerVariableBindings: Lazy<List<HeaderVariableBinding>> = lazy {
    data class Seed(
        var type: GroundType,
        var declaration: TypeVariable.Site,
        val usages: MutableList<TypeVariable.Site>,
        val aliases: MutableSet<TypeVariable>,
        val paths: MutableSet<DependencyPath>,
        val headerExpressions: MutableSet<Expression>,
    )

    fun seed(binding: HeaderVariableBinding) =
        Seed(
            binding.variable.bound,
            TypeVariable.Site(
                binding.variable.declaration.expression,
                binding.variable.declaration.region,
                binding.variable.declaration.ordinal,
                interpretedGroundType = binding.variable.declaration.groundType,
            ),
            binding.variable.usages.mapTo(mutableListOf()) {
              TypeVariable.Site(
                  it.expression,
                  it.region,
                  it.ordinal,
                  interpretedGroundType = it.groundType,
              )
            },
            (binding.aliases + binding.variable).toMutableSet(),
            binding.paths.toMutableSet(),
            binding.headerExpressions.toMutableSet(),
        )

    fun Seed.absorb(other: Seed) {
      usages += other.declaration
      usages += other.usages
      aliases += other.aliases
      paths += other.paths
      headerExpressions += other.headerExpressions
    }

    fun Seed.copySeed() =
        Seed(
            type,
            declaration,
            usages.toMutableList(),
            aliases.toMutableSet(),
            paths.toMutableSet(),
            headerExpressions.toMutableSet(),
        )

    val seeds = mutableListOf<Seed>()
    directSuperclasses
        .flatMap { it.headerVariableBindings() }
        .forEach { inherited ->
          val incoming = seed(inherited)
          val overlapping = seeds.filter { it.paths.any(incoming.paths::contains) }
          overlapping.forEach(incoming::absorb)
          seeds.removeAll(overlapping)
          seeds += incoming
        }

    val occurrenceGroups = mutableListOf<MutableList<HeaderOccurrence>>()
    headerOccurrences().forEach { occurrence ->
      val matching = occurrenceGroups.filter { group ->
        group.any { prior ->
          prior.expression.sameAuthoredTypeExpressionAs(occurrence.expression) &&
              sameHeaderVariable(prior, occurrence)
        }
      }
      if (matching.isEmpty()) {
        occurrenceGroups += mutableListOf(occurrence)
      } else {
        val merged = matching.first()
        matching.drop(1).forEach {
          merged += it
          occurrenceGroups.remove(it)
        }
        merged += occurrence
      }
    }

    occurrenceGroups
        .sortedBy { occurrences -> occurrences.minOf(HeaderOccurrence::ordinal) }
        .forEach { occurrences ->
          val paths = occurrences.mapTo(mutableSetOf(), HeaderOccurrence::path)
          val overlapping = seeds.filter { it.paths.any(paths::contains) }
          val target =
              if (overlapping.isEmpty()) {
                val first = occurrences.minBy(HeaderOccurrence::ordinal)
                Seed(
                    loader.resolve(first.expression.uncomplemented()),
                    TypeVariable.Site(
                        first.expression,
                        first.region,
                        first.ordinal,
                        interpretedGroundType = loader.resolve(first.expression.uncomplemented()),
                    ),
                    mutableListOf(),
                    mutableSetOf(),
                    mutableSetOf(),
                    mutableSetOf(),
                )
              } else {
                overlapping.first().copySeed().also { merged ->
                  overlapping.drop(1).forEach(merged::absorb)
                }
              }
          seeds.removeAll(overlapping)
          occurrences.sortedBy(HeaderOccurrence::ordinal).forEach { occurrence ->
            if (occurrence.expression !== target.declaration.expression) {
              target.usages +=
                  TypeVariable.Site(
                      occurrence.expression,
                      occurrence.region,
                      occurrence.ordinal,
                      interpretedGroundType =
                          loader.resolve(occurrence.expression.uncomplemented()),
                  )
            }
            target.headerExpressions += occurrence.expression
            target.paths += occurrence.path
          }
          target.paths += paths
          seeds += target
        }

    var bodyOrdinal = headerOccurrences().size
    declaration.effects.forEachIndexed { effectIndex, effect ->
      effect.descendantsOfType<Expression>().forEach { expression ->
        if (expression.className == ANYONE) return@forEach
        val exact = seeds.filter { seed ->
          seed.headerExpressions.any(expression::sameAuthoredTypeExpressionAs)
        }
        val matching = exact.ifEmpty {
          seeds.filter { seed ->
            seed.headerExpressions.any { header ->
              header.className == expression.className &&
                  ((header.simple && expression.arguments.isNotEmpty()) ||
                      (expression.simple && header.arguments.isNotEmpty()))
            }
          }
        }
        if (matching.size > 1) {
          throw PetException("$className uses ambiguous Class Type variable $expression in $effect")
        }
        matching
            .singleOrNull()
            ?.usages
            ?.add(
                TypeVariable.Site(
                    expression,
                    declaration.dependencies.size + declaration.supertypes.size + effectIndex,
                    bodyOrdinal++,
                    complementedUse = expression.complement,
                )
            )
      }
    }

    seeds.map { seed ->
      val variable = TypeVariable(seed.type, seed.declaration, seed.usages)
      HeaderVariableBinding(
          variable,
          seed.aliases + variable,
          seed.paths,
          seed.headerExpressions,
      )
    }
  }

  private val dependencyEqualities: Lazy<List<DependencyEquality>> = lazy {
    headerVariableBindings().mapNotNull { binding ->
      binding.paths
          .takeIf { it.size > 1 }
          ?.let { paths ->
            DependencyEquality(binding.headerExpressions, paths)
          }
    }
  }

  private fun sameHeaderVariable(
      first: HeaderOccurrence,
      second: HeaderOccurrence,
  ): Boolean {
    fun hasSuffix(longer: List<Key>, suffix: List<Key>): Boolean =
        longer.size >= suffix.size && longer.takeLast(suffix.size) == suffix

    val firstPath = first.path.keyList
    val secondPath = second.path.keyList
    if (hasSuffix(firstPath, secondPath) || hasSuffix(secondPath, firstPath)) return true

    val firstInSupertype = first.region >= declaration.dependencies.size
    val secondInSupertype = second.region >= declaration.dependencies.size
    return firstInSupertype != secondInSupertype && firstPath.last() == secondPath.last()
  }

  /** Type variables visible in this Class, including inherited declarations. */
  private val typeVariablesLazy = lazy {
    headerVariableBindings().map(HeaderVariableBinding::variable)
  }
  public val typeVariables: List<TypeVariable>
    get() = typeVariablesLazy.value

  /** Returns the Class-variable occurrences visible in [effect]. */
  public fun typeVariablesIn(effect: Effect): TypeVariableScope =
      TypeVariableScope.containing(typeVariables, effect)

  /** Returns [effect] annotated with the Class-header variables it uses. */
  public fun interpretTypeVariablesIn(effect: Effect): Effect =
      effect.withTypeVariables(typeVariablesIn(effect))

  internal fun variableBindings(
      general: GroundType,
      specific: GroundType,
      variables: Iterable<TypeVariable>,
  ): Map<TypeVariable, GroundType> {
    require(general.rootClass === this)
    require(specific.rootClass === this)
    val requested = variables.toSet()

    fun capturedAt(
        type: GroundType,
        path: DependencyPath,
        complemented: Boolean,
    ): GroundType =
        when (val dependency = type.dependencies.at(path)) {
          is TypeDependency -> dependency.boundType
          is Dependency.ComplementDependency ->
              if (complemented) dependency.excludedType else dependency.domainType
          else -> dependency.boundClass.baseType
        }

    return buildMap {
      headerVariableBindings().forEach { binding ->
        val aliases = binding.aliases.intersect(requested)
        if (aliases.isEmpty()) return@forEach
        val complemented = binding.variable.declaration.expression.complement
        val previous =
            binding.paths
                .map { path -> capturedAt(general, path, complemented) }
                .distinct()
                .singleOrNull()
                ?: error("Type variable ${binding.variable} has conflicting prior values")
        val next =
            binding.paths
                .map { path -> capturedAt(specific, path, complemented) }
                .distinct()
                .singleOrNull() ?: error("Type variable ${binding.variable} has conflicting values")
        if (next == previous) return@forEach
        aliases.forEach { variable -> put(variable, next) }
      }
    }
  }

  // GETTING TYPES

  public fun withAllDependencies(deps: DependencySet): GroundType =
      GroundType(this, normalizeVariableEqualities(deps.subMapInOrder(dependencies.keys)))

  /** Least upper bound of all types with rootClass==this */
  private val baseTypeLazy = lazy { withAllDependencies(dependencies) }
  public val baseType: GroundType
    get() = baseTypeLazy.value

  private val defaultTypeLazy = lazy {
    val templateDependencies =
        dependencies.merge(defaults.allUsages.dependencies) { _, default -> default }
    withAllDependencies(templateDependencies)
  }
  public val defaultType: GroundType
    get() = defaultTypeLazy.value

  public fun specialize(specs: List<Expression>): GroundType = baseType.specialize(specs)

  /** Returns the dependency key matched by each authored specialization, in authored order. */
  public fun matchDependencyKeys(specs: List<Expression>): List<Key> =
      dependencies.matchPartialInOrder(specs).map(Dependency::key)

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  private val classTypeLazy = lazy {
    loader.classClass.withAllDependencies(depsForClassType(this))
  }
  internal val classType: GroundType
    get() = classTypeLazy.value

  public fun concreteTypes(): Sequence<GroundType> = baseType.concreteSubtypesSameClass()

  internal val defaultsDecl
    get() = declaration.defaultsDeclaration

  private val defaultsLazy = lazy { Defaults.forClass(this) }
  public val defaults: Defaults
    get() = defaultsLazy.value

  override fun equals(other: Any?): Boolean =
      other is Class && other.className == className && other.loader == loader

  override fun hashCode(): Int = className.hashCode() xor loader.hashCode()

  override fun toString(): String = "$className"

  private companion object {
    fun superclasses(
        declaration: ClassDeclaration,
        loader: ClassLoader,
    ): List<Class> {
      return declaration.supertypes
          .classNames()
          .also { require(COMPONENT !in it) }
          .ifEmpty { listOf(COMPONENT) }
          .map { loader.loadRelated(it, active = true) }
    }
  }
}
