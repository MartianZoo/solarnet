package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.invalidPetDefinition
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.Or
import dev.martianzoo.pets.ast.Effect.Trigger.SelfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.pets.data.ClassSelection

/**
 * Builds a master [ClassTable] from a [Catalog], or internally forms an active-class view over that
 * master. Freezing prevents additional classes from being loaded.
 */
public class ClassLoader
private constructor(
    internal override val catalog: Catalog,
    private val masterSource: ClassTable?,
    private val blockedActivations: Map<ClassName, Set<ClassName>> = emptyMap(),
    private val configuredModuleNames: Set<ClassName> = emptySet(),
    private val configuredClassSelections: Set<ClassSelection> = emptySet(),
) : ClassTable() {
  /** Compiles the master table that a [Catalog] implementation retains and exposes. */
  public constructor(catalog: Catalog) : this(catalog, null)

  internal override val masterTable: ClassTable = masterSource ?: this

  private val knownClassNames: Set<ClassName> = masterSource?.allClassNames ?: catalog.allClassNames

  private val cache = mutableMapOf<Expression, Type>()

  /** The `Component` class, which is the root of the class hierarchy. */
  public override val componentClass: Class =
      masterSource?.componentClass
          ?: Class(
              validateCustomImplementation(knownDeclaration(COMPONENT)),
              this,
              directSuperclasses = emptyList(),
          )

  /** The `Class` class, the other class that is required to exist. */
  public override val classClass: Class =
      masterSource?.classClass
          ?: Class(
              validateCustomImplementation(knownDeclaration(CLASS)),
              this,
              directSuperclasses = listOf(componentClass),
          )

  private val loadedClasses =
      mutableMapOf<ClassName, Class?>(COMPONENT to componentClass, CLASS to classClass)

  override fun findClass(name: ClassName): Class? {
    if (masterSource != null) return masterSource.findClass(name)
    return if (name in loadedClasses) {
      loadedClasses[name] ?: throw PetException("Class-loading cycle involving $name")
    } else {
      null
    }
  }

  /** Returns the [Type] represented by [expression]. */
  override fun resolve(expression: Expression): Type {
    if (masterSource != null) return masterSource.resolve(expression)
    if (expression.complement) {
      throw ExpressionException("complement type expression has no standalone type: $expression")
    }
    // Avoiding computeIfAbsent due to CME
    return cache[expression]
        ?: try {
          getClass(expression.className)
              .specialize(expression.arguments)
              .refine(expression.refinement)
              .also { cache[expression] = it }
        } catch (e: RuntimeException) {
          throw ExpressionException("can't resolve $expression", e)
        }
  }

  private val frozenClasses: Set<Class> by lazy {
    require(frozen)
    loadedClasses.keys.mapTo(linkedSetOf(), masterTable::getClass)
  }

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  override fun allClasses(): Set<Class> = frozenClasses

  // LOADING

  /** Returns the class whose stable [Class.className] is [name], loading it first if necessary. */
  internal fun load(name: ClassName): Class {
    if (!frozen) loadAll(listOf(name))
    return getClass(name)
  }

  /** Loads every class known to this class loader's backing [Catalog], and freezes. */
  public fun loadEverything(): ClassTable {
    knownClassNames.forEach(::loadSingle)
    return freeze()
  }

  private val queue = ArrayDeque<ClassName>()
  private val requestedBy = mutableMapOf<ClassName, ClassName?>()

  private enum class Truth {
    TRUE,
    FALSE,
    UNKNOWN,
  }

  private fun truthOfAll(values: Collection<Truth>): Truth =
      when {
        Truth.FALSE in values -> Truth.FALSE
        values.all { it == Truth.TRUE } -> Truth.TRUE
        else -> Truth.UNKNOWN
      }

  private fun truthOfAny(values: Collection<Truth>): Truth =
      when {
        Truth.TRUE in values -> Truth.TRUE
        values.all { it == Truth.FALSE } -> Truth.FALSE
        else -> Truth.UNKNOWN
      }

  /** Loads [names] together, advancing their activation closure one complete frontier at a time. */
  internal fun loadAll(names: Collection<ClassName>) {
    enqueue(names, requestedByClass = null)
    while (queue.isNotEmpty()) {
      while (queue.isNotEmpty()) {
        val next = queue.removeFirst()
        blockedActivations[next]?.let { availabilityModules ->
          val source = requestedBy.getValue(next)
          val path = source?.let { "$it requires locked Class $next" } ?: "Class $next is locked"
          throw IllegalArgumentException(
              "broken game premise: $path; select one of its bundle Modules: " + availabilityModules
          )
        }
        loadRelated(next, active = true)
      }
      enqueueReachableActivationEdges()
    }
  }

  private fun enqueue(names: Collection<ClassName>, requestedByClass: ClassName?) {
    (names - loadedClasses.keys - queue).forEach { name ->
      requestedBy[name] = requestedByClass
      queue += name
    }
  }

  internal fun loadRelated(next: ClassName, active: Boolean): Class {
    if (masterSource != null) {
      val klass = masterSource.getClass(next)
      if (active && next !in loadedClasses) loadedClasses[next] = klass
      return klass
    }
    if (next in loadedClasses) {
      return loadedClasses[next] ?: throw PetException("Class-loading cycle involving $next")
    }
    val declaration = knownDeclaration(next)
    validateClassLiterals(declaration)
    validateNoEffectCreatesClass(declaration)
    return construct(declaration)
  }

  private fun validateNoEffectCreatesClass(declaration: ClassDeclaration) {
    val change =
        declaration.effects
            .flatMap { effect -> effect.instruction.descendantsOfType<Change>() }
            .firstOrNull { it.gaining?.className == CLASS } ?: return
    throw invalidPetDefinition(
        "Class representatives are fixed before effects run and cannot be gained by an effect: " +
            change
    )
  }

  private fun validateClassLiterals(declaration: ClassDeclaration) {
    fun validateClassLiterals(node: PetNode) {
      node.visitDescendants {
        if (it is Count && it.expression.className == CLASS) {
          val argument = it.expression.arguments.singleOrNull()?.takeIf(Expression::simple)
          argument?.let { expression ->
            if (expression.className !in knownClassNames) {
              throw Exceptions.classNotFound(expression.className)
            }
          }
          it.expression.refinement?.let(::validateClassLiterals)
          false
        } else {
          true
        }
      }
    }
    declaration.allNodes.forEach(::validateClassLiterals)
  }

  /**
   * Rechecks every live declaration because activating one Class can make a previously impossible
   * Trigger or gate reachable. The closure is monotone: Classes only become active.
   */
  private fun enqueueReachableActivationEdges() {
    val activeNames = loadedClasses.keys
    (activeNames - COMPONENT - CLASS).forEach { name ->
      enqueue(activationEdges(knownDeclaration(name), activeNames) - THIS, name)
    }
  }

  /** Returns the structurally or constructively required Classes in one live declaration. */
  private fun activationEdges(
      declaration: ClassDeclaration,
      activeNames: Set<ClassName>,
  ): Set<ClassName> = buildSet {
    fun collectStructural(expression: Expression) {
      if (!expression.complement) add(expression.className)
      expression.arguments.forEach(::collectStructural)
    }

    fun isUninhabited(expression: Expression): Boolean {
      if (expression.className == THIS) return false
      if (!expression.complement && expression.className !in activeNames) return true
      return expression.arguments.any(::isUninhabited)
    }

    fun truthOf(requirement: Requirement): Truth =
        when (requirement) {
          is Requirement.Counting if requirement.metric is Metric.Count -> {
            val metric = requirement.metric
            val configuredCount = configuredCount(metric.expression)
            when {
              configuredCount != null ->
                  if (configuredCount in requirement.range) Truth.TRUE else Truth.FALSE
              isUninhabited(metric.expression) ->
                  if (0 in requirement.range) Truth.TRUE else Truth.FALSE
              else -> Truth.UNKNOWN
            }
          }
          is Requirement.Counting -> Truth.UNKNOWN
          is Requirement.And -> truthOfAll(requirement.requirements.map(::truthOf))
          is Requirement.Or -> truthOfAny(requirement.requirements.map(::truthOf))
          is Requirement.Eval,
          is Requirement.Transform -> Truth.UNKNOWN
        }

    fun triggerReachable(trigger: Trigger): Boolean =
        when (trigger) {
          is SelfTrigger -> true
          is OnGainOf -> !isUninhabited(trigger.expression)
          is OnRemoveOf -> !isUninhabited(trigger.expression)
          is Or -> trigger.triggers.any(::triggerReachable)
          is ByTrigger -> triggerReachable(trigger.inner) && !isUninhabited(trigger.by)
          is IfTrigger ->
              triggerReachable(trigger.inner) && truthOf(trigger.condition) != Truth.FALSE
          is XTrigger -> triggerReachable(trigger.inner)
          is Transform -> triggerReachable(trigger.inner)
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
          if (truthOf(tree.gate) != Truth.FALSE) collectInstruction(tree.inner)
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
        .filter { triggerReachable(it.trigger) }
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
    val countedClass = masterSource.getClass(expression.className)
    val concreteSubclassNames =
        countedClass
            .allSubclasses()
            .filterNot(Class::abstract)
            .mapTo(linkedSetOf(), Class::className)
    if (concreteSubclassNames.isEmpty()) return null
    if (catalog.modules.keys.containsAll(concreteSubclassNames)) {
      return configuredModuleNames.count { moduleName ->
        masterSource.getClass(moduleName).isSubtypeOf(countedClass)
      }
    }
    val selections = configuredClassSelections.associateBy(ClassSelection::className)
    if (!selections.keys.containsAll(concreteSubclassNames)) return null
    return selections.values.count { selection ->
      selection.included && masterSource.getClass(selection.className).isSubtypeOf(countedClass)
    }
  }

  private fun loadSingle(name: ClassName): Class =
      loadedClasses[name] ?: loadRelated(name, active = true)

  // All classes are created here (aside from Component and Class, at top).
  private fun construct(source: ClassDeclaration): Class {
    check(masterSource == null) { "a projection must not construct Classes" }
    require(!frozen) { "Too late, this class table is frozen!" }
    val decl = validateCustomImplementation(source)

    fun store(c: Class?) {
      loadedClasses[decl.className] = c
    }
    store(null) // to detect reentrancy
    try {
      val klass = Class(decl, this)
      validateCustomInheritance(klass)
      store(klass)
      return klass
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
    fun hasInstructionIntensity(defaults: DefaultsDeclaration): Boolean =
        defaults.universal.intensity != null ||
            defaults.gainOnly.intensity != null ||
            defaults.removeOnly.intensity != null

    val inheritedDefaults =
        klass.properSuperclasses().filter {
          it.className != COMPONENT && hasInstructionIntensity(it.declaration.defaultsDeclaration)
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
      throw PetException(
          "${klass.className} cannot inherit Pets behavior as a Custom class: " +
              problems.joinToString()
      )
    }
  }

  private var frozen: Boolean = false

  private var properSubclassesByClass: Map<Class, Set<Class>>? = null
  private var directSubclassesByClass: Map<Class, Set<Class>>? = null

  internal fun properSubclassesOf(klass: Class): Set<Class> {
    require(frozen)
    return checkNotNull(properSubclassesByClass)[klass] ?: emptySet()
  }

  internal fun directSubclassesOf(klass: Class): Set<Class> {
    require(frozen)
    return checkNotNull(directSubclassesByClass)[klass] ?: emptySet()
  }

  internal fun freeze(): ClassTable {
    require(!frozen)
    if (masterSource != null) {
      frozen = true
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

    properSubclassesByClass = knownProperSubclasses.mapValues { (_, subclasses) ->
      subclasses.toSet()
    }

    val directSubclasses = mutableMapOf<Class, MutableSet<Class>>()
    knownClasses.forEach { subclass ->
      subclass.directSuperclasses.forEach { superclass ->
        directSubclasses.getOrPut(superclass, ::linkedSetOf).add(subclass)
      }
    }
    directSubclassesByClass = directSubclasses.mapValues { (_, subclasses) -> subclasses.toSet() }

    frozen = true
    return this
  }

  public override val allClassNames: Set<ClassName> by lazy {
    require(frozen)
    loadedClasses.keys
  }

  override fun toString(): String = "loader$id"

  private fun knownDeclaration(name: ClassName): ClassDeclaration =
      masterSource?.getClass(name)?.declaration
          ?: catalog.allClassDeclarations[name]
          ?: throw Exceptions.classNotFound(name)

  private fun validateCustomImplementation(decl: ClassDeclaration): ClassDeclaration {
    if (masterSource != null) return decl
    if (decl.custom) {
      catalog.customClass(decl.className)
    } else {
      if (catalog.customClasses.any { it.className == decl.className }) {
        throw PetException("Non-custom class ${decl.className} has a custom implementation")
      }
    }
    return decl
  }

  private val id = nextId++

  internal companion object {
    private var nextId: Int = 0

    internal fun projection(
        catalog: Catalog,
        masterTable: ClassTable,
        configuredModuleNames: Set<ClassName>,
        configuredClassSelections: Set<ClassSelection>,
    ): ClassLoader {
      require(masterTable.masterTable === masterTable) {
        "Catalog class table is not a master table"
      }
      val blocked =
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
          blocked,
          configuredModuleNames,
          configuredClassSelections,
      )
    }
  }
}
