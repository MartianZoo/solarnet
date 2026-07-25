package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.Type
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PetNode

/**
 * All [MClass] instances come from here. Uses a [Ruleset] to pull class declarations from as
 * needed. Can be [frozen], which prevents additional classes from being loaded, and enables
 * features such as [MClass.allSubclasses] to work.
 */
internal class MClassLoader(
    /**
     * The source of class declarations to use as needed; [loadEverything] will load every class
     * found here.
     */
    internal val ruleset: Ruleset,
) : MClassTable() {
  private val cache = mutableMapOf<Expression, MType>()

  /** The `Component` class, which is the root of the class hierarchy. */
  override val componentClass =
      MClass(validateCustomImplementation(decl(COMPONENT)), this, directSuperclasses = listOf())

  /** The `Class` class, the other class that is required to exist. */
  override val classClass =
      MClass(
          validateCustomImplementation(decl(CLASS)),
          this,
          directSuperclasses = listOf(componentClass),
      )

  private val loadedClasses =
      mutableMapOf<ClassName, MClass?>(COMPONENT to componentClass, CLASS to classClass)

  /**
   * Returns the [MClass] whose [MClass.className] or [MClass.shortName] is [name], or throws an
   * exception.
   */
  override fun getClass(name: ClassName): MClass {
    if (name !in loadedClasses) throw Exceptions.classNotFound(name)
    return loadedClasses[name] ?: error("reentrancy happened")
  }

  /** Returns the [MType] represented by [expression]. */
  override fun resolve(expression: Expression): MType {
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

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  override fun resolve(type: Type): MType = type as? MType ?: resolve(type.expressionFull)

  private val allClasses: Set<MClass> by lazy { loadedClasses.values.map { it!! }.toSet() }

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  override fun allClasses() = allClasses.also { require(frozen) }

  // LOADING

  /**
   * Returns the class whose [MClass.className] or [MClass.shortName] is [name], loading it first if
   * necessary.
   */
  internal fun load(name: ClassName): MClass {
    if (!frozen) loadAll(listOf(name))
    return getClass(name)
  }

  /** Loads every class known to this class loader's backing [Ruleset], and freezes. */
  public fun loadEverything(): MClassTable {
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

  internal fun loadAndMaybeEnqueueRelated(next: ClassName): MClass {
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

  private fun loadSingle(idOrName: ClassName): MClass =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: construct(decl(idOrName))
      }

  private fun loadSingle(idOrName: ClassName, decl: ClassDeclaration): MClass =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: construct(decl)
      }

  // all MClasses are created here (aside from Component and Class, at top)
  private fun construct(decl: ClassDeclaration): MClass {
    require(!frozen) { "Too late, this table is frozen!" }
    validateCustomImplementation(decl)

    fun store(c: MClass?) {
      loadedClasses[decl.className] = c
      loadedClasses[decl.shortName] = c
    }
    store(null) // to detect reentrancy
    return MClass(decl, this).also(::store)
  }

  private var frozen: Boolean = false

  internal fun freeze(): MClassTable {
    require(!frozen)
    frozen = true
    return this
  }

  override val allClassNamesAndIds: Set<ClassName> by lazy {
    require(frozen)
    loadedClasses.keys
  }

  internal fun checkAllTypes(node: PetNode) = node.visitDescendants {
    if (it is Expression) {
      resolve(it.uncomplemented()).expression
      false
    } else {
      true
    }
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
