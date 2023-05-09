package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.Exceptions
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ClassDeclaration
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
public class MClassLoader(
    /**
     * The source of class declarations to use as needed; [loadEverything] will load every class
     * found here.
     */
    override val authority: Authority,
) : MClassTable() {
  /** The `Component` class, which is the root of the class hierarchy. */
  override val componentClass: MClass = MClass(decl(COMPONENT), this, directSuperclasses = listOf())

  /** The `Class` class, the other class that is required to exist. */
  override val classClass: MClass =
      MClass(decl(CLASS), this, directSuperclasses = listOf(componentClass))

  private val loadedClasses =
      mutableMapOf<ClassName, MClass?>(COMPONENT to componentClass, CLASS to classClass)

  private val queue = ArrayDeque<ClassName>()

  override val transformers = Transformers(this)

  init {
    val names = (transformers.requiredClasses + OK).intersect(authority.allClassNames)
    loadAll(names)
  }

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
    return getClass(expression.className)
        .specialize(expression.arguments)
        .refine(expression.refinement)
  }

  /** Returns the corresponding [MType] to [type] (possibly [type] itself). */
  override fun resolve(type: Type): MType = type as? MType ?: resolve(type.expression)

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  override val allClasses: Set<MClass> by lazy {
    require(frozen)
    loadedClasses.values.map { it!! }.toSet()
  }

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

  /** Equivalent to calling [load] on every class name (or shortName) in [names]. */
  internal fun loadAll(names: Collection<ClassName>) {
    queue += names
    while (queue.any()) {
      loadAndMaybeEnqueueRelated(queue.removeFirst())
    }
  }

  internal fun loadAndMaybeEnqueueRelated(next: ClassName): MClass {
    if (next in loadedClasses) return loadedClasses[next] ?: error("reentrant")
    val declaration = decl(next)
    val newClass = loadSingle(next, declaration)
    val needed = declaration.allNodes.flatMap { it.descendantsOfType<ClassName>() }
    queue.addAll(needed.toSet() - loadedClasses.keys - THIS)
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

    val mclass = MClass(decl, this)
    loadedClasses[decl.className] = mclass
    loadedClasses[decl.shortName] = mclass

    return mclass
  }

  internal var frozen: Boolean = false
    private set

  internal fun freeze(): MClassTable {
    require(!frozen)
    frozen = true
    return this
  }

  override val allClassNamesAndIds: Set<ClassName> by lazy {
    require(frozen)
    loadedClasses.keys
  }

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
    require(frozen)
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
  override fun matchPartial(expressionArgs: List<Expression>, deps: DependencySet): DependencySet {
    val usedDeps = mutableSetOf<TypeDependency>()
    val list =
        expressionArgs.map { arg ->
          val specType: MType = resolve(arg)
          for (candidateDep in deps.typeDependencies) {
            val intersectionType = (specType glb candidateDep.boundType) ?: continue
            if (usedDeps.add(candidateDep)) {
              return@map TypeDependency(candidateDep.key, intersectionType)
            }
          }
          throw Exceptions.badExpression(arg, deps.toString())
        }
    return DependencySet.of(list)
  }

  override fun defaults(className: ClassName) =
      allDefaults[className] ?: throw Exceptions.classNotFound(className)

  private val id = nextId++

  override fun toString() = "loader$id"

  private companion object {
    var nextId: Int = 0
  }
}
