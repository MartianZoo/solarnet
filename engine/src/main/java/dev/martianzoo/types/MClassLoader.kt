package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.AUTO_LOAD
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.Type
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.data.GameSetup
import javax.inject.Inject

/**
 * All [MClass] instances come from here. Uses an [Authority] to pull class declarations from as
 * needed. Can be [frozen], which prevents additional classes from being loaded, and enables
 * features such as [MClass.allSubclasses] to work.
 */
@GameScoped
internal class MClassLoader(
    /**
     * The source of class declarations to use as needed; [loadEverything] will load every class
     * found here.
     */
    override val authority: Authority,
) : MClassTable() {
  @Inject
  constructor(setup: GameSetup) : this(setup.authority) {
    fun isAutoLoad(c: ClassDeclaration): Boolean =
        c.className == AUTO_LOAD ||
            c.supertypes.any { isAutoLoad(decl(it.className)) }

    loadAll(setup.players().classNames())
    loadAll(authority.allClassDeclarations.filterValues(::isAutoLoad).keys)
    loadAll(setup.allDefinitions().classNames())

    // TODO wow gross bad hack eww
    if ("C" in setup.bundles) {
      loadAll((authority as TfmAuthority).colonyTileDefinitions.classNames())
      loadAll(
          authority.explicitClassDeclarations
              .filter { cn("TradeFleet").expression in it.supertypes }
              .classNames())
      load(cn("DelayedColonyTile"))
    }
    freeze()
  }

  private val cache = mutableMapOf<Expression, MType>()

  /** The `Component` class, which is the root of the class hierarchy. */
  override val componentClass = MClass(decl(COMPONENT), this, directSuperclasses = listOf())

  /** The `Class` class, the other class that is required to exist. */
  override val classClass = MClass(decl(CLASS), this, directSuperclasses = listOf(componentClass))

  private val loadedClasses =
      mutableMapOf<ClassName, MClass?>(COMPONENT to componentClass, CLASS to classClass)

  // MClasses & MTypes need this so this is where it has to be
  internal val transformers = Transformers(this)

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
    // Avoiding computeIfAbsent due to CME
    return cache[expression]
        ?: try {
          getClass(expression.className)
              .specialize(expression.arguments)
              .refine(expression.refinement)
              .also { cache[expression] = it }
        } catch (e: Exception) {
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

  /** Loads every class known to this class loader's backing [Authority], and freezes. */
  public fun loadEverything(): MClassTable {
    authority.allClassNames.forEach(::loadSingle)
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
      val needed = declaration.allNodes.flatMap { it.descendantsOfType<ClassName>() }
      queue.addAll(needed.toSet() - loadedClasses.keys - THIS)
    }
  }

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

  internal fun checkAllTypes(node: PetNode) =
      node.visitDescendants {
        if (it is Expression) {
          resolve(it).expression
          false
        } else {
          true
        }
      }

  override fun toString() = "loader$id"

  private fun decl(cn: ClassName) = authority.classDeclaration(cn)

  private val id = nextId++

  private companion object {
    var nextId: Int = 0
  }
}
