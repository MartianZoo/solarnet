package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PetNode

/**
 * All [Class] instances come from here. Uses a [Ruleset] to pull class declarations from as needed.
 * Can be [frozen], which prevents additional classes from being loaded, and enables features such
 * as [Class.allSubclasses] to work.
 */
public class ClassLoader
public constructor(
    /**
     * The source of class declarations to use as needed; [loadEverything] will load every class
     * found here.
     */
    internal val ruleset: Ruleset,
) : ClassTable() {
  private val cache = mutableMapOf<Expression, Type>()

  /** The `Component` class, which is the root of the class hierarchy. */
  public override val componentClass: Class =
      Class(validateCustomImplementation(decl(COMPONENT)), this, directSuperclasses = emptyList())

  /** The `Class` class, the other class that is required to exist. */
  public override val classClass: Class =
      Class(
          validateCustomImplementation(decl(CLASS)),
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

  /** Loads every class known to this class loader's backing [Ruleset], and freezes. */
  public fun loadEverything(): ClassTable {
    ruleset.allClassNames.forEach(::loadSingle)
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
    val activeDeclaration = activeDeclaration(next)
    val declaration =
        if (active) activeDeclaration ?: knownDeclaration(next) else knownDeclaration(next)
    val phantom = !active || activeDeclaration == null
    if (!phantom) validateDependencyBounds(declaration)
    return construct(declaration, phantom).also {
      if (phantom) return@also
      queue.addAll(activationEdges(declaration) - loadedClasses.keys - THIS)
    }
  }

  private fun validateDependencyBounds(declaration: ClassDeclaration) {
    fun validate(expression: Expression) {
      if (expression.complement) return
      if (expression.className != THIS && activeDeclaration(expression.className) == null) {
        knownDeclaration(expression.className)
        throw PetException(
            "${declaration.className} has inactive dependency type ${expression.className}"
        )
      }
      expression.arguments.forEach(::validate)
    }

    declaration.dependencies.forEach(::validate)
    declaration.supertypes.forEach { supertype -> supertype.arguments.forEach(::validate) }
  }

  /**
   * The class names that loading [declaration] as active demands also be loaded as active. Today
   * this is every name the declaration mentions anywhere, no matter how it is mentioned; only the
   * argument of a `Class<...>` metric gets narrower treatment. Note that a name reachable only this
   * way still loads as a phantom when the ruleset has no active declaration for it.
   */
  private fun activationEdges(declaration: ClassDeclaration): Set<ClassName> = buildSet {
    fun collectRelated(node: PetNode) {
      node.visitDescendants {
        when {
          it is Count && it.expression.className == CLASS -> {
            add(CLASS)
            val argument = it.expression.arguments.singleOrNull()?.takeIf(Expression::simple)
            argument?.let { expression ->
              if (knownClassName(expression.className) == null) {
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
      addAll(ruleset.customClass(declaration.className).requiredClassNames)
    }
  }

  private fun knownClassName(name: ClassName): ClassName? =
      activeDeclaration(name)?.className
          ?: declarationIn(ruleset.knownClassDeclarations, name)?.className

  private fun loadSingle(idOrName: ClassName): Class =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: loadRelated(idOrName, active = true)
      }

  // All classes are created here (aside from Component and Class, at top).
  private fun construct(source: ClassDeclaration, phantom: Boolean): Class {
    require(!frozen) { "Too late, this class table is frozen!" }
    if (!phantom) validateCustomImplementation(source)
    val decl = if (phantom) source.withoutDeclaredBehavior() else source

    fun store(c: Class?) {
      loadedClasses[decl.className] = c
    }
    store(null) // to detect reentrancy
    return Class(decl, this, phantom).also(::store)
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
    ruleset.knownClassDeclarations.values
        .distinctBy { it.className }
        .forEach { declaration ->
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

  private fun decl(cn: ClassName) = activeDeclaration(cn) ?: ruleset.classDeclaration(cn)

  private fun activeDeclaration(name: ClassName): ClassDeclaration? =
      declarationIn(ruleset.allClassDeclarations, name)

  private fun knownDeclaration(name: ClassName): ClassDeclaration =
      declarationIn(ruleset.knownClassDeclarations, name) ?: throw Exceptions.classNotFound(name)

  private fun declarationIn(
      declarations: Map<ClassName, ClassDeclaration>,
      name: ClassName,
  ): ClassDeclaration? {
    return declarations[name]
  }

  private fun validateCustomImplementation(decl: ClassDeclaration): ClassDeclaration {
    if (decl.custom) {
      ruleset.customClass(decl.className)
    } else {
      if (ruleset.customClasses.any { it.className == decl.className }) {
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
