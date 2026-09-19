package dev.martianzoo.pets.types

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.DIE
import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
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
 * A named node in one catalog-scoped nominal hierarchy, as defined by
 * [section 2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
 *
 * This value compiles [declaration] into resolved supertypes, [dependencies], properties, defaults,
 * and a base type. The class loader constructs exactly one instance per name, so reference identity
 * represents name identity within [classTable].
 */
public class Class
internal constructor(
    /**
     * The source declaration retained under
     * [rule T2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
     */
    public val declaration: ClassDeclaration,

    /** The class loader used while constructing this class. */
    private val loader: ClassLoader,

    /** Whether resolving this declaration's immediate hierarchy activates those Classes. */
    activateRelated: Boolean = true,

    /**
     * The declared direct supertypes; empty only for the root class, under
     * [rules T1-4 and T2-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
     */
    public val directSuperclasses: List<Class> = superclasses(declaration, loader, activateRelated),
) : HasClassName, Specification<Class> {

  /**
   * The master universe containing this class, as required by
   * [rule T1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  // TODO: Contract this temporary tfm-canon seam.
  public val classTable: ClassTable = loader

  /**
   * The canonical class name that determines identity within [classTable] ([rule
   * T2-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes)).
   */
  override val className: ClassName = declaration.className.also { require(it != THIS) }

  init {
    if (directSuperclasses.any { !it.abstract }) {
      throw PetException(
          "$className cannot extend concrete class(es): " +
              directSuperclasses.filterNot { it.abstract }.joinToString { "${it.className}" }
      )
    }
  }

  /**
   * The declaration's documentation text, retained under
   * [rule T2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   */
  public val docstring: String?
    get() = declaration.docstring

  private val resolvedProperties: Map<PropertyName, PropertyFact> = resolveProperties()

  /**
   * The property facts inherited and narrowed according to
   * [rules T9-1 through T9-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties).
   */
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

  /**
   * Whether this class is abstract, as declared under
   * [rule T2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   */
  public val abstract: Boolean
    get() = declaration.abstract

  /**
   * Returns [abstract]; class abstractness does not depend on [info] ([rule
   * T2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes)).
   */
  override fun isAbstract(info: TypeInfo): Boolean = abstract

  /**
   * Tests the reflexive, transitive nominal subclass relation specified by
   * [rule T2-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   *
   * @throws IllegalArgumentException if [that] belongs to another universe (rule T1-2).
   */
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

  /**
   * Asserts the subclass relation with [that], producing a narrowing error on failure as specified
   * by
   * [rule T2-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   *
   * @throws NarrowingException if this class is not a subclass of [that].
   * @throws IllegalArgumentException if [that] belongs to another universe (rule T1-2).
   */
  override fun ensureNarrows(that: Class, info: TypeInfo) {
    if (!isSubtypeOf(that))
        throw NarrowingException("${this.className} is not a subclass of ${that.className}")
  }

  /**
   * Tests the converse of [isSubtypeOf], including equality ([rule
   * T2-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes)).
   */
  public fun isSupertypeOf(that: Class): Boolean = that.isSubtypeOf(this)

  private fun requireSameClassTable(that: Class) {
    require(classTable.commonTable(that.classTable) != null) {
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

  /**
   * Every independently enforced invariant inherited by this concrete class; component-count
   * invariants supply the dependency-target guarantee in
   * [rule T3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public val invariants: Set<Requirement> =
      if (abstract) emptySet()
      else allSuperclasses.flatMap { split(it.declaration.invariants) }.toSet()

  /**
   * Every superclass in the walk specified by
   * [rule T2-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes),
   * including this class.
   */
  public fun allSuperclasses(): Set<Class> = allSuperclasses

  internal fun properSuperclasses(): Set<Class> = allSuperclasses() - this

  // DEPENDENCIES

  /** The dependency positions whose values are bound to the inheriting class. */
  private val selfBindings: Lazy<Set<DependencyPath>> = lazy {
    val inherited = directSuperclasses.flatMap { it.selfBindings() }
    val declaredDependencies =
        declaration.dependencies.flatMapIndexed { index, expression ->
          val key = Key(className, index)
          selfBindingsIn(expression, declaredDeps().get(key), listOf(key))
        }
    val declaredSupertypes = sups.flatMap { sourceSupertype ->
      val superclass = loader.getClass(sourceSupertype.className)
      val arguments = sourceSupertype.arguments
      val matched =
          superclass.dependencies.matchPartialInOrder(arguments.map(::replaceThis), loader)
      arguments.zip(matched).flatMap { (argument, dependency) ->
        selfBindingsIn(argument, dependency, listOf(dependency.key))
      }
    }
    (inherited + declaredDependencies + declaredSupertypes).toSet()
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
    val matched = dependencies.matchPartialInOrder(expression.arguments.map(::replaceThis), loader)
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
    // Rule T3-3: supertypes constraining one key have their bounds intersected, and bounds with no
    // common narrowing are an error.
    inherited.reduceOrNull { left, right ->
      left.merge(right) { a, b ->
        loader.glb(a, b)
            ?: throw PetException("$className inherits incompatible bounds for ${a.key}: $a and $b")
      }
    } ?: DependencySet.of()
  }
  private val declaredDeps: Lazy<DependencySet> = lazy {
    DependencySet.of(
        declaration.dependencies.mapIndexed { index, expression ->
          TypeDependency(Key(className, index), loader.resolve(replaceThis(expression)))
        }
    )
  }
  private var resolvingDependencies: Boolean = false

  // Laziness lets classes refer to each other; a true cycle has no finite answer, so it is
  // rejected.
  private val dependenciesLazy = lazy {
    if (resolvingDependencies) {
      throw PetException(
          "$className has a circular dependency: resolving its dependency bounds requires " +
              "those same bounds"
      )
    }
    resolvingDependencies = true
    try {
      val resolved =
          if (className == CLASS) {
            depsForClassType(loader.componentClass)
          } else {
            inheritedDeps().merge(declaredDeps()) { _, _ -> error("unexpected") }
          }
      resolved
          .typeDependencies()
          .firstOrNull { dependency ->
            val target = dependency.boundType.rootClass
            target.className == DIE || target.allSuperclasses().any { it.className == SIGNAL }
          }
          ?.let { dependency ->
            throw PetException(
                "$className dependency ${dependency.key} cannot target " +
                    "${dependency.boundType.expressionFull}; Signal types and Die cannot be " +
                    "dependency targets"
            )
          }
      resolved
    } finally {
      resolvingDependencies = false
    }
  }
  /**
   * The complete keyed dependency set inherited and narrowed according to
   * [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   *
   * @throws PetException if two supertypes constrain one key to bounds with no common narrowing
   *   (rule T3-3), or if the dependency bounds contain the cycle forbidden by rule T3-11.
   */
  public val dependencies: DependencySet
    get() = dependenciesLazy.value

  private data class DependencyEquality(
      val expressions: Set<Expression>,
      val paths: Set<DependencyPath>,
  )

  /**
   * Whether [key] participates in the same-header-variable equality of
   * [rules T3-8 and T13-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
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
    val classTable =
        original.classTable?.let {
          requireNotNull(loader.commonTable(it)) { "$original belongs to a different class table" }
        } ?: loader
    var dependencies = original
    var changed: Boolean
    do {
      changed = false
      dependencyEqualities().forEach { equality ->
        val occurrences = equality.paths.map(dependencies::at)
        val intersectionByPath =
            if (occurrences.map(Dependency::key).distinct().size == 1) {
              val intersection = occurrences.reduce { left, right ->
                classTable.glb(left, right) ?: equalityError(equality, dependencies)
              }
              equality.paths.associateWith { intersection }
            } else {
              val typed = occurrences.filterIsInstance<TypeDependency>()
              if (typed.size != occurrences.size) equalityError(equality, dependencies)
              val bound =
                  typed.map(TypeDependency::boundType).reduce { left, right ->
                    classTable.glb(left, right) ?: equalityError(equality, dependencies)
                  }
              equality.paths.associateWith { path ->
                (dependencies.at(path) as TypeDependency).map { bound }
              }
            }
        equality.paths.forEach { path ->
          val intersection = intersectionByPath.getValue(path)
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
      val occurrences = equality.paths.map(dependencies::at)
      val agree =
          if (occurrences.map(Dependency::key).distinct().size == 1) {
            occurrences.distinct().size == 1
          } else {
            val typed = occurrences.filterIsInstance<TypeDependency>()
            typed.size == occurrences.size &&
                typed.map(TypeDependency::boundType).distinct().size == 1
          }
      if (!agree) {
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
      val lexicallyDeclared: Boolean,
  )

  private val headerOccurrences: Lazy<List<HeaderOccurrence>> = lazy {
    var ordinal = 0
    buildList {
      fun eligible(expression: Expression): Boolean =
          expression.className != THIS &&
              runCatching { loader.resolve(expression).abstract }.getOrDefault(false)

      fun collectArguments(expression: Expression, prefix: List<Key>, region: Int) {
        if (expression.arguments.isEmpty()) return
        val dependencySet = loader.load(expression.className).dependencies
        val arguments = expression.arguments.map(replacer(THIS, className)::transformExpression)
        val matched = dependencySet.matchPartialInOrder(arguments, loader)
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
        var lexicallyDeclared: Boolean,
    )

    fun inheritedSeed(binding: HeaderVariableBinding) =
        Seed(
            binding.variable.bound,
            TypeVariable.Site(
                binding.variable.declaration.expression,
                binding.variable.declaration.region,
                binding.variable.declaration.ordinal,
                interpretedGroundType = binding.variable.declaration.groundType,
            ),
            mutableListOf(),
            (binding.aliases + binding.variable).toMutableSet(),
            binding.paths.toMutableSet(),
            binding.headerExpressions.toMutableSet(),
            lexicallyDeclared = false,
        )

    fun Seed.absorb(other: Seed) {
      if (other.lexicallyDeclared) {
        usages += other.declaration
        usages += other.usages
      }
      aliases += other.aliases
      paths += other.paths
      headerExpressions += other.headerExpressions
      lexicallyDeclared = lexicallyDeclared || other.lexicallyDeclared
    }

    fun Seed.copySeed() =
        Seed(
            type,
            declaration,
            usages.toMutableList(),
            aliases.toMutableSet(),
            paths.toMutableSet(),
            headerExpressions.toMutableSet(),
            lexicallyDeclared = lexicallyDeclared,
        )

    val seeds = mutableListOf<Seed>()
    directSuperclasses
        .flatMap { it.headerVariableBindings() }
        .forEach { inherited ->
          val incoming = inheritedSeed(inherited)
          val overlapping = seeds.filter { it.paths.any(incoming.paths::contains) }
          overlapping.forEach(incoming::absorb)
          seeds.removeAll(overlapping)
          seeds += incoming
        }

    val occurrenceGroups = mutableListOf<MutableList<HeaderOccurrence>>()
    headerOccurrences().forEach { occurrence ->
      val matching = occurrenceGroups.filter { group ->
        group.any { prior ->
          val priorName = prior.expression.typeVariableName?.name
          val occurrenceName = occurrence.expression.typeVariableName?.name
          priorName != null && priorName == occurrenceName
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
          val named = occurrences.filter { it.expression.typeVariableName is Declaration }
          require(named.size <= 1) {
            "$className declares the same header Type variable more than once"
          }
          val first = named.singleOrNull() ?: occurrences.minBy(HeaderOccurrence::ordinal)
          fun localSite() =
              TypeVariable.Site(
                  first.expression,
                  first.region,
                  first.ordinal,
                  interpretedGroundType = loader.resolve(first.expression),
              )

          val paths = occurrences.mapTo(mutableSetOf(), HeaderOccurrence::path)
          val overlapping = seeds.filter { it.paths.any(paths::contains) }
          val alreadyDeclared = overlapping.firstOrNull(Seed::lexicallyDeclared)
          val target =
              when {
                overlapping.isEmpty() ->
                    Seed(
                        loader.resolve(first.expression),
                        localSite(),
                        mutableListOf(),
                        mutableSetOf(),
                        mutableSetOf(),
                        mutableSetOf(),
                        lexicallyDeclared = true,
                    )
                alreadyDeclared != null ->
                    alreadyDeclared.copySeed().also { merged ->
                      overlapping.filterNot { it === alreadyDeclared }.forEach(merged::absorb)
                    }
                else ->
                    overlapping.first().copySeed().also { merged ->
                      overlapping.drop(1).forEach(merged::absorb)
                      merged.declaration = localSite()
                      merged.usages.clear()
                      merged.lexicallyDeclared = true
                    }
              }
          seeds.removeAll(overlapping)
          occurrences.sortedBy(HeaderOccurrence::ordinal).forEach { occurrence ->
            if (occurrence.ordinal != target.declaration.ordinal) {
              target.usages +=
                  TypeVariable.Site(
                      occurrence.expression,
                      occurrence.region,
                      occurrence.ordinal,
                      interpretedGroundType = loader.resolve(occurrence.expression),
                  )
            }
            target.headerExpressions += occurrence.expression
            target.paths += occurrence.path
          }
          target.paths += paths
          seeds += target
        }

    val namedVariables =
        seeds.filter(Seed::lexicallyDeclared).mapNotNull { seed ->
          (seed.declaration.expression.typeVariableName as? Declaration)?.name?.let { it to seed }
        }
    val ineligibleDeclaration =
        (declaration.dependencies + declaration.supertypes)
            .flatMap { it.descendantsOfType<Expression>() }
            .firstOrNull { expression ->
              expression.typeVariableName is Declaration &&
                  namedVariables.none { (_, seed) ->
                    seed.declaration.expression === expression
                  }
            }
    require(ineligibleDeclaration == null) {
      "$ineligibleDeclaration cannot declare a Class-header Type variable"
    }
    namedVariables
        .firstOrNull { (name) -> name in loader.allClassNames }
        ?.let { (name) ->
          throw PetException("Type-variable name $name is already a Type name")
        }
    require(namedVariables.map { it.first }.distinct().size == namedVariables.size) {
      "$className declares the same header Type-variable name twice"
    }
    val variablesByName = namedVariables.toMap()
    var bodyOrdinal = headerOccurrences().size
    declaration.effects.forEachIndexed { effectIndex, effect ->
      effect.descendantsOfType<Expression>().forEach { expression ->
        val named = (expression.typeVariableName as? Reference)?.name
        named
            ?.let(variablesByName::get)
            ?.usages
            ?.add(
                TypeVariable.Site(
                    expression,
                    declaration.dependencies.size + declaration.supertypes.size + effectIndex,
                    bodyOrdinal++,
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
          seed.lexicallyDeclared,
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

  private val typeVariablesLazy = lazy {
    headerVariableBindings().filter(HeaderVariableBinding::lexicallyDeclared).map {
      it.variable
    }
  }

  /**
   * The eligible type variables declared by this class header under
   * [rules T13-2 through T13-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val typeVariables: List<TypeVariable>
    get() = typeVariablesLazy.value

  /**
   * Returns the class-header variable occurrences visible in [effect], preserving inherited scope
   * as required by
   * [rules T13-3 and T13-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun typeVariablesIn(effect: Effect): TypeVariableScope =
      TypeVariableScope.containing(typeVariables, effect)

  /**
   * Returns [effect] with its visible class-header variable scope, without annotating the shared
   * source declaration, according to
   * [rules T13-3 and T13-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun interpretTypeVariablesIn(effect: Effect): Effect =
      effect.copy().withTypeVariables(typeVariablesIn(effect))

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
    ): GroundType {
      val dependency = type.dependencies.at(path)
      return (dependency as? TypeDependency)?.boundType ?: dependency.boundClass.baseType
    }

    return buildMap {
      headerVariableBindings().forEach { binding ->
        val aliases = binding.aliases.intersect(requested)
        if (aliases.isEmpty()) return@forEach
        val previous =
            binding.paths.map { path -> capturedAt(general, path) }.distinct().singleOrNull()
                ?: error("Type variable ${binding.variable} has conflicting prior values")
        val next =
            binding.paths.map { path -> capturedAt(specific, path) }.distinct().singleOrNull()
                ?: error("Type variable ${binding.variable} has conflicting values")
        // A fixed inherited dependency still has to replace its superclass's open variable.
        if (next == previous && next.abstract) return@forEach
        aliases.forEach { variable -> put(variable, next) }
      }
    }
  }

  // GETTING TYPES

  /**
   * Constructs a type from one bound for every dependency key, projecting away unrelated keys and
   * enforcing header-variable equalities as specified by
   * [rules T5-7 and T3-8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  public fun withAllDependencies(deps: DependencySet): GroundType {
    val projected = deps.subMapInOrder(dependencies.keys)
    require(projected.keys == dependencies.keys) {
      "expected keys ${dependencies.keys}, got $deps"
    }
    val classTable =
        projected.classTable?.let {
          requireNotNull(loader.commonTable(it)) { "$deps belongs to a different class table" }
        } ?: loader
    val bounded =
        requireNotNull(classTable.glb(dependencies, projected)) {
          "$deps does not satisfy the declared dependency bounds of $className"
        }
    return GroundType(this, normalizeVariableEqualities(bounded))
  }

  private val baseTypeLazy = lazy { withAllDependencies(dependencies) }

  /**
   * The type that supplies every dependency's declared bound, as defined by
   * [rules T2-1 and T5-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#5-types).
   */
  public val baseType: GroundType
    get() = baseTypeLazy.value

  private val defaultExpressionLazy = lazy {
    val templateDependencies =
        dependencies.merge(defaults.allUsages.dependencies) { _, default -> default }
    className.of(templateDependencies.expressionsFull())
  }

  /**
   * The authored dependency-default template for this class. Unlike a [GroundType], this expression
   * may retain contextual `Owner` outside the class's declared bound until elaboration supplies its
   * component context.
   */
  internal val defaultExpression: Expression
    get() = defaultExpressionLazy.value

  private val defaultTypeLazy = lazy { loader.resolve(defaultExpression) }

  /**
   * The valid resolved interpretation of [defaultExpression], as specified by
   * [rule T10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
   */
  public val defaultType: GroundType
    get() = defaultTypeLazy.value

  /**
   * Applies authored [specs] to [baseType] using greedy dependency matching ([rules T3-5 and
   * T5-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies)).
   */
  public fun specialize(specs: List<Expression>): GroundType = baseType.specialize(specs, loader)

  internal fun specialize(specs: List<Expression>, classTable: ClassTable): GroundType =
      baseType.specialize(specs, classTable)

  /**
   * Replays specialization and returns the dependency key matched by each authored argument, in the
   * authored order required by
   * [rule T3-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
   */
  public fun matchDependencyKeys(specs: List<Expression>): List<Key> =
      matchDependencyKeys(specs, loader)

  internal fun matchDependencyKeys(specs: List<Expression>, classTable: ClassTable): List<Key> =
      dependencies.matchPartialInOrder(specs, classTable).map(Dependency::key)

  /**
   * Returns the special *class type* for this class; for example, for the class `Resource` returns
   * the type `Class<Resource>`.
   */
  private val classTypeLazy = lazy {
    loader.classClass.withAllDependencies(depsForClassType(this))
  }
  internal val classType: GroundType
    get() = classTypeLazy.value

  /**
   * Enumerates concrete types whose root is exactly this class, following same-class enumeration in
   * [rule T11-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#11-enumeration-and-automatic-narrowing).
   */
  public fun concreteTypes(): Sequence<GroundType> = baseType.concreteSubtypesSameClass()

  internal val defaultsDecl
    get() = declaration.defaultsDeclaration

  private val defaultsLazy = lazy { Defaults.forClass(this) }
  /**
   * The inherited all-usage, gain-only, and removal-only defaults defined by
   * [section 10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults).
   */
  public val defaults: Defaults
    get() = defaultsLazy.value

  /**
   * Returns the canonical name required by
   * [rule T2-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes).
   */
  override fun toString(): String = "$className"

  private companion object {
    fun superclasses(
        declaration: ClassDeclaration,
        loader: ClassLoader,
        activateRelated: Boolean,
    ): List<Class> {
      return declaration.supertypes
          .classNames()
          .also {
            if (COMPONENT in it) {
              throw PetException(
                  "${declaration.className} must not name $COMPONENT as a supertype; " +
                      "every class extends it already"
              )
            }
          }
          .ifEmpty { listOf(COMPONENT) }
          .map { loader.loadRelated(it, include = activateRelated) }
    }
  }
}
