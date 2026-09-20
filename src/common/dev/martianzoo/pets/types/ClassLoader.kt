package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.SubscribedTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.TransformNode
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.pets.data.ClassSelection

/**
 * Incrementally compiles a [Catalog] into the single master universe specified by
 * [rules T1-1 and T1-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
 *
 * Name lookup and resolution are available while loading. [loadEverything] completes and freezes a
 * master universe. A game table freezes its combined structural namespace before computing the
 * premise's inclusion closure, so every enumeration used by that closure has a stable namespace.
 */
public class ClassLoader
private constructor(
    internal override val catalog: Catalog,
    private val masterSource: ClassTable?,
    private val premiseDeclarations: Map<ClassName, ClassDeclaration> = emptyMap(),
    private val unavailableClasses: Map<ClassName, Set<ClassName>> = emptyMap(),
    private val configuredModuleNames: Set<ClassName> = emptySet(),
    private val configuredClassSelections: Set<ClassSelection> = emptySet(),
) : ClassTable() {
  /**
   * Begins compiling [catalog]'s master universe; call [loadEverything] before enumeration ([rules
   * T1-1 and
   * T1-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity)).
   */
  public constructor(catalog: Catalog) : this(catalog, null)

  internal override val masterTable: ClassTable = masterSource ?: this

  private val knownClassNames: Set<ClassName> =
      (masterSource?.allClassNames ?: catalog.allClassNames) + premiseDeclarations.keys

  private val cache = mutableMapOf<Expression, GroundType>()

  /**
   * The required `Component` root specified by
   * [rule T1-4](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public override val componentClass: Class =
      masterSource?.componentClass
          ?: Class(
              validateCustomImplementation(knownDeclaration(COMPONENT)),
              this,
              directSuperclasses = emptyList(),
          )

  /**
   * The required `Class` class specified by
   * [rule T1-5](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public override val classClass: Class =
      masterSource?.classClass
          ?: Class(
              validateCustomImplementation(knownDeclaration(CLASS)),
              this,
              directSuperclasses = listOf(componentClass),
          )

  private val loadedClasses =
      mutableMapOf<ClassName, Class?>(COMPONENT to componentClass, CLASS to classClass)
  private val includedClassNames = linkedSetOf(COMPONENT, CLASS)

  /**
   * Returns the already loaded class named [name], or null; exact-name lookup during loading
   * follows
   * [rules T1-6 and T1-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  override fun findClass(name: ClassName): Class? {
    return if (name in loadedClasses) {
      loadedClasses[name]
          ?: throw InvalidPetDefinitionException("class-loading cycle involving `$name`")
    } else {
      masterSource?.findClass(name)
    }
  }

  /**
   * Resolves [expression] with strict exact names under
   * [rules T1-3 and T1-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   *
   * @throws ExpressionException if [expression] is invalid in this universe.
   */
  override fun resolve(expression: Expression): GroundType {
    cache[expression]?.let {
      return it
    }

    if (masterSource != null) {
      var requiresCombinedTable = false
      expression.visitDescendants { node ->
        if (node is Not || (node is ClassName && node in premiseDeclarations)) {
          requiresCombinedTable = true
        }
        !requiresCombinedTable
      }
      if (!requiresCombinedTable) {
        return masterSource.resolve(expression).also { cache[expression] = it }
      }
    }
    expression.refinement?.conjuncts()?.filterIsInstance<Not>()?.forEach { refinement ->
      fun containsRefinement(candidate: Expression): Boolean =
          candidate.refinement != null || candidate.arguments.any(::containsRefinement)
      if (containsRefinement(refinement.excluded)) {
        throw ExpressionException("`NOT` operand cannot contain a refinement: `$expression`")
      }
    }
    // Avoiding computeIfAbsent due to CME
    return try {
      getClass(expression.className)
          .specialize(expression.arguments, this)
          .inTable(this)
          .refine(expression.refinement)
          .also { cache[expression] = it }
    } catch (e: RuntimeException) {
      throw ExpressionException("cannot resolve `$expression`", e)
    }
  }

  private lateinit var frozenClasses: MutableSet<Class>

  /**
   * Returns every included class after this table's structural universe is frozen; calling before
   * freezing violates
   * [rule T1-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  override fun allClasses(): Set<Class> {
    require(frozen) { "this class table must be frozen before its classes can be enumerated" }
    return frozenClasses
  }

  override fun allKnownClasses(): Set<Class> {
    require(frozen) { "this class table must be frozen before its classes can be enumerated" }
    if (masterSource == null) return frozenClasses
    return masterSource.allKnownClasses() +
        premiseDeclarations.keys.mapTo(linkedSetOf()) { name -> getClass(name) }
  }

  // LOADING

  /** Returns the class whose stable [Class.className] is [name], loading it first if necessary. */
  internal fun load(name: ClassName): Class {
    if (!frozen) loadAll(listOf(name))
    return getClass(name)
  }

  /**
   * Loads every declaration, freezes the resulting master universe, and constructs every class's
   * base type, satisfying the enumeration precondition in
   * [rule T1-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public fun loadEverything(): ClassTable {
    knownClassNames.forEach(::loadSingle)
    val completed = freeze()
    knownClassNames.forEach { name ->
      getClass(name).baseType
    }
    validateNoOkSubscriptions()
    validateTransformKinds()
    return completed
  }

  /** The classes this load is responsible for checking: a master's own, or a premise's delta. */
  private fun declaringClassesToValidate(): Set<Class> =
      if (masterSource == null) {
        allKnownClasses()
      } else {
        premiseDeclarations.keys.mapTo(linkedSetOf(), ::getClass)
      }

  /**
   * Rejects a transform block whose kind this Catalog defines no handler for, per
   * [rule L10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks).
   * One rewriting pass may leave another pass's kind in place, but a mark no pass will ever claim
   * is a mistake in the source.
   */
  internal fun validateTransformKinds() {
    val known = catalog.transformHandlerFactories.keys
    declaringClassesToValidate().map(Class::declaration).forEach { declaration ->
      declaration.allNodes.forEach { root ->
        root.visitDescendants { node ->
          if (node is TransformNode<*> && node.transformKind !in known) {
            throw InvalidPetDefinitionException(
                "`${declaration.className}` uses undefined transform kind " +
                    "`${node.transformKind}` in `$node`"
            )
          }
          true
        }
      }
    }
  }

  /**
   * Rejects subscriptions rooted at `Ok` or a nominal supertype, which are statically forbidden.
   */
  internal fun validateNoOkSubscriptions() {
    val okClass = getClass(OK)
    declaringClassesToValidate().forEach { declaringClass ->
      declaringClass.declaration.effects.forEach { effect ->
        val forbidden =
            effect.trigger
                .descendantsOfType<SubscribedTrigger>()
                .map {
                  when (it) {
                    is OnGainOf -> it.expression
                    is OnRemoveOf -> it.expression
                  }
                }
                .firstOrNull { expression ->
                  val triggerClass =
                      if (expression.className == THIS) declaringClass
                      else getClass(expression.className)
                  okClass.isSubtypeOf(triggerClass)
                }
        if (forbidden != null) {
          throw InvalidPetDefinitionException(
              "`${declaringClass.className}` effect `$effect` subscribes to `$forbidden`, " +
                  "whose root is `Ok` or a nominal supertype of `Ok`"
          )
        }
      }
    }
  }

  private val queue = ArrayDeque<ClassName>()
  private val requestedBy = mutableMapOf<ClassName, ClassName?>()

  /** Loads [names] together, advancing their inclusion closure one complete frontier at a time. */
  internal fun loadAll(names: Collection<ClassName>) {
    enqueue(names, requestedByClass = null)
    while (queue.isNotEmpty()) {
      while (queue.isNotEmpty()) {
        val next = queue.removeFirst()
        unavailableClasses[next]?.let { availabilityModules ->
          val source = requestedBy.getValue(next)
          val path =
              source?.let { "`$it` requires locked Class `$next`" } ?: "Class `$next` is locked"
          val message =
              "broken game premise: $path; required bundle modules: `$availabilityModules`"
          if (masterSource == null) throw InvalidPetDefinitionException(message)
          throw InvalidGameConfigException(message)
        }
        loadRelated(next, include = true)
      }
      enqueueReachableSelectionEdges()
    }
  }

  /**
   * Computes a game's inclusion closure within its completed structural namespace.
   *
   * Structural references and reachable constructive instructions include their required Classes.
   * Counts, requirements, and triggers alone do not, except that a positive lower bound in a Class
   * invariant includes the structural domain it counts. A constructive instruction beneath a
   * provably false gate or trigger is inert. The calculation repeats because each included Class
   * can make another edge reachable. A required Class locked behind an unselected Module makes the
   * premise broken rather than silently selecting that Module.
   */
  internal fun includeAll(names: Collection<ClassName>) {
    require(masterSource != null && frozen) {
      "a game table must be structurally frozen before its inclusion closure is computed"
    }
    loadAll(names)
  }

  private fun enqueue(names: Collection<ClassName>, requestedByClass: ClassName?) {
    (names - includedClassNames - queue).forEach { name ->
      requestedBy[name] = requestedByClass
      queue += name
    }
  }

  private fun includeClass(name: ClassName) {
    if (includedClassNames.add(name)) {
      if (frozen) frozenClasses += getClass(name)
      invalidateInclusionCaches()
    }
  }

  internal fun loadRelated(next: ClassName, include: Boolean): Class {
    if (masterSource != null && next !in premiseDeclarations) {
      val klass = masterSource.getClass(next)
      if (include) includeClass(next)
      return klass
    }
    if (next in loadedClasses) {
      return (loadedClasses[next]
              ?: throw InvalidPetDefinitionException("class-loading cycle involving `$next`"))
          .also { if (include) includeClass(next) }
    }
    val declaration = knownDeclaration(next)
    validateClassNames(declaration)
    validateNoEffectCreatesClass(declaration)
    return construct(declaration, include).also { if (include) includeClass(next) }
  }

  private fun validateNoEffectCreatesClass(declaration: ClassDeclaration) {
    val change =
        declaration.effects
            .flatMap { effect -> effect.instruction.descendantsOfType<Change>() }
            .firstOrNull { it.gaining?.className == CLASS } ?: return
    throw InvalidPetDefinitionException(
        "class representatives cannot be gained by an effect: `$change`"
    )
  }

  /**
   * Rejects every name this declaration writes that the catalog never declares, enforcing
   * [rule T1-7](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity)
   * where the declaration that spelled it can still be named. Only the name is decided here;
   * whether an argument fits its bound depends on classes this one may be loaded ahead of.
   */
  private fun validateClassNames(declaration: ClassDeclaration) {
    declaration.allNodes.forEach { node ->
      node.visitDescendants {
        val name = it as? ClassName
        if (name != null && name != THIS && name !in knownClassNames) {
          throw InvalidPetDefinitionException(
              "`${declaration.className}` names undeclared Class `$name`"
          )
        }
        true
      }
    }
  }

  /**
   * Rechecks every included declaration because including one Class can make a previously
   * impossible Trigger or gate reachable. The closure is monotone: Classes only become included.
   */
  private fun enqueueReachableSelectionEdges() {
    val includedNames = includedClassNames
    (includedNames - COMPONENT - CLASS).forEach { name ->
      enqueue(selectionEdges(knownDeclaration(name), includedNames) - THIS, name)
    }
  }

  /** Returns the structurally or constructively required Classes in one live declaration. */
  private fun selectionEdges(
      declaration: ClassDeclaration,
      includedNames: Set<ClassName>,
  ): Set<ClassName> = buildSet {
    val interpreter =
        InhabitanceInterpreter(
            classIsUninhabited = { name ->
              if (masterSource == null) name !in includedNames else !isInhabited(name)
            },
            exactCount = ::configuredCount,
        )

    fun collectStructural(expression: Expression) {
      add(expression.className)
      expression.arguments.forEach(::collectStructural)
    }

    fun collectRequiredInhabitants(requirement: Requirement) {
      when (requirement) {
        is Requirement.Counting if
            requirement.range.first > 0 && requirement.metric is Metric.Count
         -> collectStructural(requirement.metric.expression)
        is Requirement.Counting -> Unit
        is Requirement.And -> requirement.requirements.forEach(::collectRequiredInhabitants)
        is Requirement.Or -> requirement.requirements.forEach(::collectRequiredInhabitants)
        is Requirement.Eval,
        is Requirement.Transform -> Unit
      }
    }

    fun collectInstruction(tree: InstructionTree) {
      when (tree) {
        is Gain -> collectStructural(tree.gaining)
        is Transmute -> collectStructural(tree.gaining)
        is Gated -> {
          if (!interpreter.requirementIsFalse(tree.gate)) collectInstruction(tree.inner)
        }
        else ->
            tree
                .immediateChildren()
                .filterIsInstance<InstructionTree>()
                .forEach(::collectInstruction)
      }
    }

    declaration.supertypes.forEach(::collectStructural)
    declaration.dependencies.forEach(::collectStructural)
    declaration.defaultsDeclaration.allNodes
        .filterIsInstance<Expression>()
        .forEach(::collectStructural)
    declaration.defaultsDeclaration.forClass?.let(::add)
    declaration.invariants.forEach(::collectRequiredInhabitants)
    declaration.effects
        .filter { interpreter.triggerIsReachable(it.trigger) }
        .forEach { collectInstruction(it.instruction) }
    declaration.allNodes
        .flatMap { it.descendantsOfType<ClassName>() }
        .filter { it != THIS && it in knownClassNames && knownDeclaration(it).custom }
        .forEach(::add)
    declaration.extraNodes.forEach { node -> node.descendantsOfType<ClassName>().forEach(::add) }
    if (declaration.custom) {
      addAll(catalog.customClass(declaration.className).requiredClassNames)
    }
  }

  private fun configuredCount(expression: Expression): Int? {
    if (masterSource == null || !expression.simple || expression.className == THIS) return null
    val countedClass = loadRelated(expression.className, include = false)
    val masterSubclasses =
        if (countedClass.classTable === masterSource) masterSource.allSubclasses(countedClass)
        else emptySet()
    val premiseSubclasses =
        premiseDeclarations.keys
            .asSequence()
            .map { name -> loadRelated(name, include = false) }
            .filter { candidate -> candidate.isSubtypeOf(countedClass) }
            .toSet()
    val concreteSubclassNames =
        (masterSubclasses + premiseSubclasses)
            .filterNot(Class::abstract)
            .mapTo(linkedSetOf(), Class::className)
    if (concreteSubclassNames.isEmpty()) return null
    if (catalog.modules.keys.containsAll(concreteSubclassNames)) {
      return configuredModuleNames.count { moduleName ->
        loadRelated(moduleName, include = false).isSubtypeOf(countedClass)
      }
    }
    val selections = configuredClassSelections.associateBy(ClassSelection::className)
    if (!selections.keys.containsAll(concreteSubclassNames)) return null
    return selections.values.count { selection ->
      selection.included &&
          loadRelated(selection.className, include = false).isSubtypeOf(countedClass)
    }
  }

  private fun loadSingle(name: ClassName): Class =
      findClass(name)?.also { includeClass(name) } ?: loadRelated(name, include = true)

  // All classes are created here (aside from Component and Class, at top).
  private fun construct(source: ClassDeclaration, includeRelated: Boolean = true): Class {
    check(masterSource == null || source.className in premiseDeclarations) {
      "a game table may construct only premise Classes"
    }
    require(!frozen) { "class table is already frozen" }
    val decl = validateCustomImplementation(source)

    fun store(c: Class?) {
      loadedClasses[decl.className] = c
    }
    store(null) // to detect reentrancy
    try {
      val klass = Class(decl, this, includeRelated)
      validateCustomInheritance(klass)
      store(klass)
      return klass
    } catch (e: ExpressionException) {
      loadedClasses.remove(decl.className)
      throw InvalidPetDefinitionException("invalid definition for `${decl.className}`", e)
    } catch (e: Throwable) {
      loadedClasses.remove(decl.className)
      throw e
    }
  }

  private fun validateCustomInheritance(klass: Class) {
    if (!klass.declaration.custom) return

    val inheritedEffects = klass.properSuperclasses().filter { it.declaration.effects.isNotEmpty() }
    val inheritedInvariants =
        klass.properSuperclasses().filter { it.declaration.invariants.isNotEmpty() }
    fun hasInstructionQuantifier(defaults: DefaultsDeclaration): Boolean =
        defaults.universal.quantifier != null ||
            defaults.gainOnly.quantifier != null ||
            defaults.removeOnly.quantifier != null

    val inheritedDefaults =
        klass.properSuperclasses().filter {
          it.className != COMPONENT && hasInstructionQuantifier(it.declaration.defaultsDeclaration)
        }
    val problems = buildList {
      if (inheritedEffects.isNotEmpty()) {
        add("effects from " + inheritedEffects.joinToString { "${it.className}" })
      }
      if (inheritedInvariants.isNotEmpty()) {
        add("invariants from " + inheritedInvariants.joinToString { "${it.className}" })
      }
      if (inheritedDefaults.isNotEmpty()) {
        add("instruction defaults from " + inheritedDefaults.joinToString { "${it.className}" })
      }
    }
    if (problems.isNotEmpty()) {
      throw InvalidPetDefinitionException(
          "`${klass.className}` cannot inherit Pets behavior as a Custom class: " +
              problems.joinToString()
      )
    }
  }

  private var frozen: Boolean = false

  private var allSubclassesByClass: Map<Class, Set<Class>>? = null
  private var directSubclassesByClass: Map<Class, Set<Class>>? = null

  internal override fun allSubclassesOf(klass: Class): Set<Class> {
    require(frozen) {
      "this class table must be frozen before the subclasses of `$klass` can be enumerated"
    }
    return checkNotNull(allSubclassesByClass).getValue(klass)
  }

  internal override fun directSubclassesOf(klass: Class): Set<Class> {
    require(frozen) {
      "this class table must be frozen before the subclasses of `$klass` can be enumerated"
    }
    return checkNotNull(directSubclassesByClass)[klass] ?: emptySet()
  }

  internal fun freeze(): ClassTable {
    require(!frozen)
    if (masterSource != null) {
      premiseDeclarations.values.forEach { declaration ->
        if (declaration.className !in loadedClasses) {
          validateClassNames(declaration)
          validateNoEffectCreatesClass(declaration)
          construct(declaration, includeRelated = false)
        }
      }
      val premiseClasses =
          premiseDeclarations.keys.mapTo(linkedSetOf()) { name ->
            checkNotNull(loadedClasses[name])
          }
      allSubclassesByClass = premiseClasses.associateWith { klass ->
        premiseClasses.filterTo(linkedSetOf()) { candidate -> candidate.isSubtypeOf(klass) }
      }
      directSubclassesByClass = premiseClasses.associateWith { klass ->
        premiseClasses.filterTo(linkedSetOf()) { candidate ->
          klass in candidate.directSuperclasses
        }
      }
      frozenClasses = includedClassNames.mapTo(linkedSetOf(), ::getClass)
      frozen = true
      premiseClasses.forEach { it.baseType }
      return this
    }
    knownClassNames.forEach { name ->
      if (name !in loadedClasses) construct(knownDeclaration(name))
    }

    val knownClasses = loadedClasses.values.map { checkNotNull(it) }
    val knownProperSubclasses = mutableMapOf<Class, MutableSet<Class>>()
    knownClasses.forEach { subclass ->
      subclass.allSuperclasses().forEach { superclass ->
        if (superclass !== subclass) {
          knownProperSubclasses.getOrPut(superclass, ::linkedSetOf).add(subclass)
        }
      }
    }
    val bitBearingSuperclasses = knownClasses.flatMap(Class::directSuperclasses).distinct()
    val superclassBits =
        bitBearingSuperclasses
            .sortedWith(
                compareByDescending<Class> { knownProperSubclasses[it]?.size ?: 0 }
                    .thenBy(Class::className)
            )
            .withIndex()
            .associate { (index, klass) -> klass to index }
    knownClasses.forEach { it.initializeSubclassBits(superclassBits) }

    allSubclassesByClass = knownClasses.associateWith { klass ->
      knownProperSubclasses[klass].orEmpty() + klass
    }

    val directSubclasses = mutableMapOf<Class, MutableSet<Class>>()
    knownClasses.forEach { subclass ->
      subclass.directSuperclasses.forEach { superclass ->
        directSubclasses.getOrPut(superclass, ::linkedSetOf).add(subclass)
      }
    }
    directSubclassesByClass = directSubclasses.mapValues { (_, subclasses) -> subclasses.toSet() }

    frozenClasses = knownClasses.toCollection(linkedSetOf())
    frozen = true
    return this
  }

  /**
   * Every included class name after this table's structural universe is frozen; calling before
   * freezing violates
   * [rule T1-6](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  public override val allClassNames: Set<ClassName>
    get() {
      require(frozen) { "this class table must be frozen before its classes can be enumerated" }
      return if (masterSource == null) loadedClasses.keys else includedClassNames
    }

  /**
   * Returns a diagnostic identity for this universe; semantic identity is the object boundary
   * specified by
   * [rules T1-1 and T1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#1-universes-and-identity).
   */
  override fun toString(): String = "loader$id"

  private fun knownDeclaration(name: ClassName): ClassDeclaration =
      premiseDeclarations[name]
          ?: masterSource?.getClass(name)?.declaration
          ?: catalog.allClassDeclarations[name]
          ?: throw ExpressionException("no class named `$name` in the current game")

  private fun validateCustomImplementation(decl: ClassDeclaration): ClassDeclaration {
    if (masterSource != null) return decl
    if (decl.custom) {
      catalog.customClass(decl.className)
    } else {
      if (catalog.customClasses.any { it.className == decl.className }) {
        throw InvalidPetDefinitionException(
            "non-custom Class `${decl.className}` has a custom implementation"
        )
      }
    }
    return decl
  }

  private val id = nextId++

  internal companion object {
    private var nextId: Int = 0

    internal fun forPremise(
        catalog: Catalog,
        premiseTable: PremiseClassTable,
        configuredModuleNames: Set<ClassName>,
        configuredClassSelections: Set<ClassSelection>,
    ): ClassLoader {
      val masterTable = premiseTable.master
      require(masterTable.masterTable === masterTable) {
        "catalog class table is not a master table"
      }
      val unavailableClasses =
          catalog.classAvailabilityModules
              .mapNotNull { (className, availabilityModules) ->
                (className to availabilityModules).takeIf {
                  availabilityModules.intersect(configuredModuleNames).isEmpty()
                }
              }
              .toMap()
      return ClassLoader(
          catalog,
          masterTable,
          premiseTable.declarations,
          unavailableClasses,
          configuredModuleNames,
          configuredClassSelections,
      )
    }
  }
}
