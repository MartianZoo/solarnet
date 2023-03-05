package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.toSetStrict

/**
 * All [PClass] instances come from here. Uses an [Authority] to pull class declarations from as
 * needed. Can be [frozen], which prevents additional classes from being loaded, and enables
 * features such as [PClass.allSubclasses] to work.
 */
public class PClassLoader(
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
  public val componentClass: PClass =
      PClass(decl(COMPONENT), this, listOf()).also {
        require(it.abstract)
        require(it.allDependencyKeys.none())
      }

  /** The `Class` class, the other class that is required to exist. */
  public val classClass: PClass =
      PClass(decl(CLASS), this, listOf(componentClass)).also { require(!it.abstract) }

  private val loadedClasses =
      mutableMapOf<ClassName, PClass?>(COMPONENT to componentClass, CLASS to classClass)

  // TODO maybe go back to the operator
  /**
   * Returns the [PClass] whose [PClass.className] or [PClass.shortName] is [name], or throws an
   * exception.
   */
  public fun getClass(name: ClassName): PClass =
      loadedClasses[name] ?: error("no class loaded with className or shortName $name")

  /** Returns the corresponding [PType] to [type] (possibly [type] itself). */
  public fun resolve(type: Type): PType = type as? PType ?: resolve(type.expression)

  /** Returns the [PType] represented by [expression]. */
  public fun resolve(expression: Expression): PType {
    return getClass(expression.className)
        .specialize(expression.arguments)
        .refine(expression.refinement)
  }

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  public val allClasses: Set<PClass> by lazy {
    require(frozen)
    loadedClasses.values.map { it!! }.toSet()
  }

  // LOADING

  /**
   * Returns the class whose [PClass.className] or [PClass.shortName] is [name], loading it first if
   * necessary.
   */
  public fun load(name: ClassName): PClass =
      when {
        frozen -> getClass(name)
        autoLoadDependencies -> {
          autoLoad(listOf(name))
          getClass(name)
        }
        else -> loadSingle(name)
      }

  /** Equivalent to calling [load] on every class name in [idsAndNames]. */
  public fun loadAll(idsAndNames: Collection<ClassName>) =
      if (autoLoadDependencies) {
        autoLoad(idsAndNames)
      } else {
        idsAndNames.forEach(::loadSingle)
      }

  /** Loads every class known to this class loader's backing [Authority], and freezes. */
  public fun loadEverything(): PClassLoader {
    authority.allClassNames.forEach(::loadSingle)
    frozen = true
    return this
  }

  private fun autoLoad(idsAndNames: Collection<ClassName>) {
    require(autoLoadDependencies)
    val queue = ArrayDeque(idsAndNames.toSet())
    while (queue.any()) {
      val next = queue.removeFirst()
      if (next !in loadedClasses) {
        val declaration = decl(next)
        loadSingle(next, declaration)
        val needed = declaration.allNodes.flatMap { it.descendantsOfType<ClassName>() }
        queue.addAll(needed.toSet() - loadedClasses.keys - THIS)
      }
    }
  }

  private fun loadSingle(idOrName: ClassName): PClass =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: construct(decl(idOrName))
      }

  private fun loadSingle(idOrName: ClassName, decl: ClassDeclaration): PClass =
      if (frozen) {
        getClass(idOrName)
      } else {
        loadedClasses[idOrName] ?: construct(decl)
      }

  // all PClasses are created here (aside from Component and Class, at top)
  private fun construct(decl: ClassDeclaration): PClass {
    require(!frozen) { "Too late, this table is frozen!" }

    require(decl.className !in loadedClasses) { decl.className }
    require(decl.shortName !in loadedClasses) { decl.shortName }

    // signal with `null` that loading is in process, so we can detect infinite recursion
    loadedClasses[decl.className] = null
    loadedClasses[decl.shortName] = null

    val pclass = PClass(decl, this)
    loadedClasses[decl.className] = pclass
    loadedClasses[decl.shortName] = pclass
    return pclass
  }

  public var frozen: Boolean = false
    public set(f) {
      require(f) { "can't melt" }
      field = f
      validate()
    }

  private fun validate() {
    allClasses.forEach { pclass ->
      pclass.classEffects.forEach {
        checkAllTypes(it.effect.replaceAll(THIS.expr, pclass.className.expr))
      }
    }
  }

  private fun decl(cn: ClassName) = authority.classDeclaration(cn)

  public val transformer: Transformer by lazy { Transformer(this) }

  public fun checkAllTypes(node: PetNode) =
      node.visitDescendants {
        if (it is Expression) {
          resolve(it).expression
          false
        } else {
          true
        }
      }

  internal val allDefaults: Map<ClassName, Defaults> by lazy {
    allClasses.associate { it.className to Defaults.forClass(it) }
  }

  /**
   * Assigns each expression to a key from among this map's keys, such that it is compatible with
   * that key's upper bound.
   */
  internal fun match(specs: List<Expression>, deps: DependencySet): DependencySet {
    val usedDeps = mutableSetOf<TypeDependency>()

    val list =
        specs.map { specExpression ->
          val specType: PType = resolve(specExpression)
          for (candidateDep in deps.dependencies - usedDeps) {
            val intersectionType = specType.glb(candidateDep.bound) ?: continue
            usedDeps += candidateDep
            return@map TypeDependency(candidateDep.key, intersectionType)
          }
          error("couldn't match up $specExpression to $this")
        }
    return DependencySet(list.toSetStrict())
  }
}
