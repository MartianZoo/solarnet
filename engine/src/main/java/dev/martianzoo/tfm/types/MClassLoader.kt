package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.PureTransformers.replaceThisWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.Dependency.TypeDependency

/**
 * All [MClass] instances come from here. Uses an [Authority] to pull class declarations from as
 * needed. Can be [frozen], which prevents additional classes from being loaded, and enables
 * features such as [MClass.allSubclasses] to work.
 */
public class MClassLoader( // TODO separate into loader and table
    /**
     * The source of class declarations to use as needed; [loadEverything] will load every class
     * found here.
     */
    private val authority: Authority,

    /**
     * Whether, when a class is loaded, to also load any classes that class depends on in some way.
     * By default, only superclasses are auto-loaded.
     */
    private val autoLoadDependencies: Boolean = false,
) {
  /** The `Component` class, which is the root of the class hierarchy. */
  public val componentClass: MClass = MClass(decl(COMPONENT), this, directSuperclasses = listOf())

  /** The `Class` class, the other class that is required to exist. */
  public val classClass: MClass =
      MClass(decl(CLASS), this, directSuperclasses = listOf(componentClass))

  private val loadedClasses =
      mutableMapOf<ClassName, MClass?>(COMPONENT to componentClass, CLASS to classClass)

  private val queue = ArrayDeque<ClassName>()

  init {
    load(OK)
  }

  // TODO HACKHACKHACK
  // This is an absolutely stupid horrible hack. Fix it. Somehow.

  public var game: Game? = null

  /**
   * Returns the [MClass] whose [MClass.className] or [MClass.shortName] is [name], or throws an
   * exception.
   */
  public fun getClass(name: ClassName): MClass {
    if (name !in loadedClasses) throw UserException.classNotFound(name)
    return loadedClasses[name] ?: error("reentrancy happened")
  }

  /** Returns the [MType] represented by [expression]. */
  public fun resolve(expression: Expression): MType {
    return getClass(expression.className)
        .specialize(expression.arguments)
        .refine(expression.refinement)
  }

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  public fun resolve(type: Type): MType = type as? MType ?: resolve(type.expression)

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  public val allClasses: Set<MClass> by lazy {
    require(frozen)
    loadedClasses.values.map { it!! }.toSet()
  }

  // LOADING

  /**
   * Returns the class whose [MClass.className] or [MClass.shortName] is [name], loading it first if
   * necessary.
   */
  internal fun load(name: ClassName): MClass =
      when {
        frozen -> getClass(name)
        autoLoadDependencies -> {
          autoLoad(listOf(name))
          getClass(name)
        }
        else -> loadSingle(name)
      }

  /** Equivalent to calling [load] on every class name (or shortName) in [names]. */
  internal fun loadAll(names: Collection<ClassName>) =
      if (autoLoadDependencies) {
        autoLoad(names)
      } else {
        names.forEach(::loadSingle)
      }

  /** Loads every class known to this class loader's backing [Authority], and freezes. */
  public fun loadEverything(): MClassLoader {
    authority.allClassNames.forEach(::loadSingle)
    frozen = true
    return this
  }

  private fun autoLoad(idsAndNames: Collection<ClassName>) {
    require(autoLoadDependencies)
    queue += idsAndNames
    while (queue.any()) {
      loadAndMaybeEnqueueRelated(queue.removeFirst())
    }
  }

  internal fun loadAndMaybeEnqueueRelated(next: ClassName): MClass {
    if (next in loadedClasses) return loadedClasses[next] ?: error("reentrant")
    val declaration = decl(next)
    val newClass = loadSingle(next, declaration)
    if (autoLoadDependencies) {
      val needed = declaration.allNodes.flatMap { it.descendantsOfType<ClassName>() }
      queue.addAll(needed.toSet() - loadedClasses.keys - THIS)
    }
    return newClass
  }

  internal fun loadSingle(idOrName: ClassName): MClass =
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

    require(decl.className !in loadedClasses) { decl.className }
    require(decl.shortName !in loadedClasses) { decl.shortName }

    // signal with `null` that loading is in process, so we can detect infinite recursion
    loadedClasses[decl.className] = null
    loadedClasses[decl.shortName] = null

    val mclass =
        try {
          MClass(decl, this)
        } catch (e: IllegalStateException) {
          throw IllegalStateException("constructing ${decl.className}", e)
        }
    loadedClasses[decl.className] = mclass
    loadedClasses[decl.shortName] = mclass

    return mclass
  }

  internal var frozen: Boolean = false
    internal set(f) {
      require(f) { "can't melt" }
      field = f
      validate()
    }

  private fun validate() {
    allClasses.forEach { mclass ->
      mclass.classEffects.forEach {
        checkAllTypes(replaceThisWith(mclass.className.expression).transform(it.effect))
      }
    }
  }

  internal val allClassNamesAndIds: Set<ClassName> by lazy {
    require(frozen)
    loadedClasses.keys
  }

  public val transformers = Transformers(this)

  internal val generalInvariants: Set<Requirement> by lazy {
    allClasses.flatMap { it.generalInvars }.toSet()
  }

  private fun decl(cn: ClassName) = authority.classDeclaration(cn)

  internal fun checkAllTypes(node: PetNode) =
      node.visitDescendants {
        if (it is Expression) {
          resolve(it).expression
          false
        } else {
          true
        }
      }

  internal val allDefaults: Map<ClassName, Defaults> by lazy {
    allClasses.associate { it.className to Defaults.forClass(it) } +
        allClasses.associate { it.shortName to Defaults.forClass(it) }
  }

  /**
   * For an example expression like `Foo<Bar, Qux>`, pass in `[Bar, Qux]` and Foo's base dependency
   * set. This method decides which dependencies in the dependency set each of these args should be
   * matched with. The returned dependency set will have [TypeDependency]s in the corresponding
   * order to the input expressions.
   *
   * DON'T call this for the <Foo> in Class<Foo>, it won't work.
   */
  internal fun matchPartial(expressionArgs: List<Expression>, deps: DependencySet): DependencySet {
    val usedDeps = mutableSetOf<TypeDependency>()
    val list =
        expressionArgs.map { arg ->
          val specType: MType = resolve(arg)
          for (candidateDep in deps.typeDependencies - usedDeps) {
            val intersectionType = (specType glb candidateDep.boundType) ?: continue
            usedDeps += candidateDep
            return@map TypeDependency(candidateDep.key, intersectionType)
          }
          throw UserException.badExpression(arg, deps.toString())
        }
    return DependencySet.of(list)
  }

  private val id = nextId++

  override fun toString() = "loader$id"

  private companion object {
    var nextId: Int = 0
  }
}
