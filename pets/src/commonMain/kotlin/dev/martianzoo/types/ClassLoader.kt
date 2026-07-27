package dev.martianzoo.types

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.ast.ClassName
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
) : TypeUniverse() {
  private val cache = mutableMapOf<Expression, Type>()

  /** The `Component` class, which is the root of the class hierarchy. */
  public override val componentClass =
      Class(validateCustomImplementation(decl(COMPONENT)), this, directSuperclasses = listOf())

  /** The `Class` class, the other class that is required to exist. */
  public override val classClass =
      Class(
          validateCustomImplementation(decl(CLASS)),
          this,
          directSuperclasses = listOf(componentClass),
      )

  private val loadedClasses =
      mutableMapOf<ClassName, Class?>(COMPONENT to componentClass, CLASS to classClass)

  override fun findClass(name: ClassName): Class? =
      if (name in loadedClasses) loadedClasses[name] ?: error("reentrancy happened") else null

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
        } catch (e: IllegalStateException) {
          throw ExpressionException("can't resolve $expression", e)
        }
  }

  private val frozenClasses: Set<Class> by lazy {
    require(frozen)
    loadedClasses.values.map { it!! }.toSet()
  }

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  override fun allClasses() = frozenClasses

  // LOADING

  /**
   * Returns the class whose [Class.className] or [Class.shortName] is [name], loading it first if
   * necessary.
   */
  public fun load(name: ClassName): Class {
    if (!frozen) loadAll(listOf(name))
    return getClass(name)
  }

  /** Loads every class known to this class loader's backing [Ruleset], and freezes. */
  public fun loadEverything(): TypeUniverse {
    ruleset.allClassNames.forEach(::loadSingle)
    return freeze()
  }

  private val queue = ArrayDeque<ClassName>()

  /** Equivalent to calling [load] on every class name (or shortName) in [names]. */
  private fun loadAll(names: Collection<ClassName>) {
    queue += names
    while (queue.any()) {
      loadAndMaybeEnqueueRelated(queue.removeFirst())
    }
  }

  internal fun loadAndMaybeEnqueueRelated(next: ClassName): Class {
    if (next in loadedClasses) return loadedClasses[next] ?: error("reentrant")
    val declaration = decl(next)
    return loadSingle(next, declaration).also {
      val needed = buildSet {
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
      }
      queue.addAll(needed.toSet() - loadedClasses.keys - THIS)
    }
  }

  private fun knownClassName(name: ClassName): ClassName? =
      ruleset.allClassDeclarations[name]?.className
          ?: ruleset.allClassDeclarations.values.singleOrNull { it.shortName == name }?.className

  private fun loadSingle(idOrName: ClassName): Class =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: construct(decl(idOrName))
      }

  private fun loadSingle(idOrName: ClassName, decl: ClassDeclaration): Class =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: construct(decl)
      }

  // All classes are created here (aside from Component and Class, at top).
  private fun construct(decl: ClassDeclaration): Class {
    require(!frozen) { "Too late, this universe is frozen!" }
    validateCustomImplementation(decl)

    fun store(c: Class?) {
      loadedClasses[decl.className] = c
      loadedClasses[decl.shortName] = c
    }
    store(null) // to detect reentrancy
    return Class(decl, this).also(::store)
  }

  private var frozen: Boolean = false

  public fun freeze(): TypeUniverse {
    require(!frozen)
    frozen = true
    return this
  }

  public override val allClassNamesAndIds: Set<ClassName> by lazy {
    require(frozen)
    loadedClasses.keys
  }

  override fun toString() = "loader$id"

  private fun decl(cn: ClassName) = ruleset.classDeclaration(cn)

  private fun validateCustomImplementation(decl: ClassDeclaration): ClassDeclaration {
    if (decl.custom) {
      ruleset.customClass(decl.className)
    } else {
      require(ruleset.customClasses.none { it.className == decl.className })
    }
    return decl
  }

  private val id = nextId++

  private companion object {
    var nextId: Int = 0
  }
}
