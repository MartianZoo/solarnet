package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PetNode

/**
 * All [Class] instances come from here. Uses an [Authority] to pull declarations as needed. Can be
 * [frozen], which prevents additional classes from being loaded, and enables features such as
 * [Class.allSubclasses] to work.
 */
public class ClassLoader
public constructor(
    /**
     * The source of class declarations to use as needed; [loadEverything] will load every class
     * found here.
     */
    internal val authority: Authority,
) : ClassTable() {
  private val cache = mutableMapOf<Expression, Type>()

  /** The `Component` class, which is the root of the class hierarchy. */
  public override val componentClass: Class =
      Class(
          validateCustomImplementation(authority.classDeclaration(COMPONENT)),
          this,
          directSuperclasses = emptyList(),
      )

  /** The `Class` class, the other class that is required to exist. */
  public override val classClass: Class =
      Class(
          validateCustomImplementation(authority.classDeclaration(CLASS)),
          this,
          directSuperclasses = listOf(componentClass),
      )

  private val loadedClasses =
      mutableMapOf<ClassName, Class?>(COMPONENT to componentClass, CLASS to classClass)

  override fun findClass(name: ClassName): Class? {
    return if (name in loadedClasses) {
      loadedClasses[name] ?: throw PetException("Class-loading cycle involving $name")
    } else {
      null
    }
  }

  /** Returns the [Type] represented by [expression]. */
  override fun resolve(expression: Expression): Type {
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
    loadedClasses.values.map { it!! }.filterNot(Class::phantom).toSet()
  }

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  override fun allClasses(): Set<Class> = frozenClasses

  // LOADING

  /** Returns the class whose stable [Class.className] is [name], loading it first if necessary. */
  public fun load(name: ClassName): Class {
    if (!frozen) loadAll(listOf(name))
    return getClass(name)
  }

  /** Loads every class known to this class loader's backing [Authority], and freezes. */
  public fun loadEverything(): ClassTable {
    authority.allClassNames.forEach(::loadSingle)
    return freeze()
  }

  private val queue = ArrayDeque<ClassName>()

  /** Equivalent to calling [load] on every canonical class name in [names]. */
  private fun loadAll(names: Collection<ClassName>) {
    queue += names
    while (queue.isNotEmpty()) {
      loadRelated(queue.removeFirst(), active = true)
    }
  }

  internal fun loadRelated(next: ClassName, active: Boolean): Class {
    if (next in loadedClasses) {
      val loaded = loadedClasses[next] ?: throw PetException("Class-loading cycle involving $next")
      if (active && loaded.phantom) {
        throw PetException("Class $next was already loaded as inactive")
      }
      return loaded
    }
    val declaration = knownDeclaration(next)
    val phantom = !active
    return construct(declaration, phantom).also {
      if (phantom) return@also
      queue.addAll(activationEdges(declaration) - loadedClasses.keys - THIS)
    }
  }

  /**
   * The class names that loading [declaration] as active demands also be loaded as active. Today
   * this is every name the declaration mentions anywhere, no matter how it is mentioned; only the
   * argument of a `Class<...>` metric gets narrower treatment.
   */
  private fun activationEdges(declaration: ClassDeclaration): Set<ClassName> = buildSet {
    fun collectRelated(node: PetNode) {
      node.visitDescendants {
        when {
          it is Count && it.expression.className == CLASS -> {
            add(CLASS)
            val argument = it.expression.arguments.singleOrNull()?.takeIf(Expression::simple)
            argument?.let { expression ->
              if (expression.className !in authority.allClassDeclarations) {
                throw Exceptions.classNotFound(expression.className)
              }
            }
            it.expression.refinement?.let(::collectRelated)
            false
          }
          it is ClassName -> {
            add(it)
            false
          }
          else -> true
        }
      }
    }
    declaration.allNodes.forEach(::collectRelated)
    if (declaration.custom) {
      addAll(authority.customClass(declaration.className).requiredClassNames)
    }
  }

  private fun loadSingle(name: ClassName): Class =
      loadedClasses[name] ?: loadRelated(name, active = true)

  // All classes are created here (aside from Component and Class, at top).
  private fun construct(source: ClassDeclaration, phantom: Boolean): Class {
    require(!frozen) { "Too late, this class table is frozen!" }
    if (!phantom) validateCustomImplementation(source)
    val decl = if (phantom) source.withoutDeclaredBehavior() else source

    fun store(c: Class?) {
      loadedClasses[decl.className] = c
    }
    store(null) // to detect reentrancy
    try {
      val klass = Class(decl, this, phantom)
      if (!phantom) validateCustomInheritance(klass)
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

  public fun freeze(): ClassTable {
    require(!frozen)
    authority.allClassDeclarations.values.forEach { declaration ->
      if (declaration.className !in loadedClasses) construct(declaration, phantom = true)
    }

    val knownClasses = loadedClasses.values.map { checkNotNull(it) }
    val bitBearingSuperclasses = knownClasses.flatMap(Class::directSuperclasses).distinct()

    val knownProperSubclasses = mutableMapOf<Class, MutableSet<Class>>()
    knownClasses.forEach { subclass ->
      subclass.allSuperclasses().forEach { superclass ->
        if (superclass !== subclass) {
          knownProperSubclasses.getOrPut(superclass, ::linkedSetOf).add(subclass)
        }
      }
    }
    val superclassBits =
        bitBearingSuperclasses
            .sortedWith(
                compareBy<Class>(Class::phantom)
                    .thenByDescending { knownProperSubclasses[it]?.size ?: 0 }
                    .thenBy(Class::className)
            )
            .withIndex()
            .associate { (index, klass) -> klass to index }
    knownClasses.forEach { it.initializeSubclassBits(superclassBits) }

    val activeProperSubclasses = mutableMapOf<Class, Set<Class>>()
    knownProperSubclasses.forEach { (superclass, subclasses) ->
      if (!superclass.phantom) {
        val activeSubclasses = subclasses.filterNotTo(linkedSetOf(), Class::phantom)
        if (activeSubclasses.isNotEmpty()) {
          activeProperSubclasses[superclass] = activeSubclasses.toSet()
        }
      }
    }
    properSubclassesByClass = activeProperSubclasses

    val activeDirectSubclasses = mutableMapOf<Class, MutableSet<Class>>()
    knownClasses.filterNot(Class::phantom).forEach { subclass ->
      subclass.directSuperclasses.forEach { superclass ->
        activeDirectSubclasses.getOrPut(superclass, ::linkedSetOf).add(subclass)
      }
    }
    directSubclassesByClass = activeDirectSubclasses.mapValues { (_, subclasses) ->
      subclasses.toSet()
    }

    frozen = true
    return this
  }

  public override val allClassNames: Set<ClassName> by lazy {
    require(frozen)
    loadedClasses.filterValues { it?.phantom == false }.keys
  }

  override fun toString(): String = "loader$id"

  private fun knownDeclaration(name: ClassName): ClassDeclaration =
      authority.allClassDeclarations[name] ?: throw Exceptions.classNotFound(name)

  private fun validateCustomImplementation(decl: ClassDeclaration): ClassDeclaration {
    if (decl.custom) {
      authority.customClass(decl.className)
    } else {
      if (authority.customClasses.any { it.className == decl.className }) {
        throw PetException("Non-custom class ${decl.className} has a custom implementation")
      }
    }
    return decl
  }

  private val id = nextId++

  private companion object {
    var nextId: Int = 0
  }
}
