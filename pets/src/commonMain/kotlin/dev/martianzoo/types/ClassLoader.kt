package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.ClassSynonyms
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
    private val classSynonyms: ClassSynonyms = ClassSynonyms.NONE,
) : ClassTable() {
  init {
    val declaredNames =
        ruleset.knownClassDeclarations.values.flatMap { listOf(it.className, it.shortName) }.toSet()
    val conflicts = classSynonyms.mappings.keys.intersect(declaredNames)
    require(conflicts.isEmpty()) {
      "Class synonyms conflict with declared names or ids: $conflicts"
    }
  }

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
    val canonicalName = classSynonyms.canonicalName(name)
    return if (canonicalName in loadedClasses) {
      loadedClasses[canonicalName]
          ?: throw PetException("Class-loading cycle involving $canonicalName")
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

  /**
   * Returns the class whose [Class.className], programmatic [Class.shortName], or configured
   * synonym is [name], loading it first if necessary.
   */
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

  /** Equivalent to calling [load] on every class name, id, or configured synonym in [names]. */
  private fun loadAll(names: Collection<ClassName>) {
    queue += names
    while (queue.isNotEmpty()) {
      loadRelated(queue.removeFirst(), active = true)
    }
  }

  internal fun loadRelated(next: ClassName, active: Boolean): Class {
    val canonicalName = classSynonyms.canonicalName(next)
    if (canonicalName in loadedClasses) {
      val loaded =
          loadedClasses[canonicalName]
              ?: throw PetException("Class-loading cycle involving $canonicalName")
      if (active && loaded.phantom) {
        throw PetException("Class $next was already loaded as inactive")
      }
      return loaded
    }
    val activeDeclaration = activeDeclaration(canonicalName)
    val declaration =
        if (active) activeDeclaration ?: knownDeclaration(canonicalName)
        else knownDeclaration(canonicalName)
    val phantom = !active || activeDeclaration == null
    return construct(declaration, phantom).also {
      if (phantom) return@also
      queue.addAll(activationEdges(declaration) - loadedClasses.keys - THIS)
    }
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
            argument?.let { expression -> knownClassName(expression.className)?.let(::add) }
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
      classSynonyms.canonicalName(name).let { canonicalName ->
        activeDeclaration(canonicalName)?.className
            ?: ruleset.allClassDeclarations.values
                .singleOrNull { it.shortName == canonicalName }
                ?.className
      }

  private fun loadSingle(idOrName: ClassName): Class =
      if (frozen) {
        getClass(idOrName)
      } else {
        val canonicalName = classSynonyms.canonicalName(idOrName)
        loadedClasses[canonicalName] ?: loadRelated(canonicalName, active = true)
      }

  // All classes are created here (aside from Component and Class, at top).
  private fun construct(source: ClassDeclaration, phantom: Boolean): Class {
    require(!frozen) { "Too late, this class table is frozen!" }
    if (!phantom) validateCustomImplementation(source)
    val decl = if (phantom) source.withoutDeclaredBehavior() else source

    fun store(c: Class?) {
      loadedClasses[decl.className] = c
      loadedClasses[decl.shortName] = c
    }
    store(null) // to detect reentrancy
    return Class(decl, this, phantom).also(::store)
  }

  private var frozen: Boolean = false

  public fun freeze(): ClassTable {
    require(!frozen)
    ruleset.knownClassDeclarations.values
        .distinctBy { it.className }
        .forEach { declaration ->
          if (declaration.className !in loadedClasses) construct(declaration, phantom = true)
        }
    frozen = true
    return this
  }

  public override val allClassNamesAndIds: Set<ClassName> by lazy {
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
    val canonicalName = classSynonyms.canonicalName(name)
    return declarations[canonicalName]
        ?: declarations.values.singleOrNull { it.shortName == canonicalName }
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
