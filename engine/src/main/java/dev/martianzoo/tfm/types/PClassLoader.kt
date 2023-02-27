package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.api.SpecialClassNames.PRODUCTION
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.TypeExpr

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
  public val componentClass: PClass = PClass(decl(COMPONENT), this, listOf())

  /** The `Class` class, the other class that is required to exist. */
  public val classClass: PClass = PClass(decl(CLASS), this, listOf(componentClass))

  private val loadedClasses =
      mutableMapOf<ClassName, PClass?>(COMPONENT to componentClass, CLASS to classClass)

  // UGLY HACKS!
  init {
    for (c in setOf(GAME, PRODUCTION, cn("GrossHack"), cn("MetalHandler"))) {
      if (c in authority.allClassNames) {
        load(c)
      }
    }
  }

  /** Returns the [PClass] whose name or id is [nameOrId], or throws an exception. */
  public fun getClass(nameOrId: ClassName): PClass =
      loadedClasses[nameOrId] ?: error("no class loaded with id or name $nameOrId")

  /** Returns the corresponding [PType] to [type] (possibly [type] itself). */
  public fun resolveType(type: Type): PType = type as? PType ?: resolveType(type.typeExprFull)

  /** Returns the [PType] represented by [typeExpr]. */
  // TODO we need transformations sometimes?
  public fun resolveType(typeExpr: TypeExpr): PType {
    val pclass = getClass(typeExpr.className)
    val result =
        if (pclass.className == CLASS) {
          val className: ClassName =
              if (typeExpr.arguments.isEmpty()) {
                COMPONENT
              } else {
                val single: TypeExpr = typeExpr.arguments.single()
                require(single.isTypeOnly)
                single.className
              }
          getClass(className).classType
        } else {
          pclass.specialize(typeExpr.arguments.map(::resolveType))
        }
    return result.refine(typeExpr.refinement)
  }

  /** All classes loaded by this class loader; can only be accessed after the loader is [frozen]. */
  public val allClasses: Set<PClass> by lazy {
    require(frozen)
    loadedClasses.values.map { it!! }.toSet()
  }

  // LOADING

  /** Returns the class with the name [idOrName], loading it first if necessary. */
  public fun load(idOrName: ClassName): PClass =
      when {
        frozen -> getClass(idOrName)
        autoLoadDependencies -> {
          loadTrees(listOf(idOrName))
          getClass(idOrName)
        }
        else -> loadSingle(idOrName)
      }

  /**
   * Equivalent to (but possibly faster than) calling [load] on every class name in [idsAndNames].
   */
  public fun loadAll(idsAndNames: Collection<ClassName>) =
      if (autoLoadDependencies) {
        loadTrees(idsAndNames)
      } else {
        idsAndNames.forEach(::loadSingle)
      }

  /** Loads every class known to this class loader's backing [Authority], and freezes. */
  public fun loadEverything(): PClassLoader {
    authority.allClassNames.forEach(::loadSingle)
    frozen = true
    return this
  }

  private fun loadTrees(idsAndNames: Collection<ClassName>) {
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

    require(decl.name !in loadedClasses) { decl.name }
    require(decl.id !in loadedClasses) { decl.id }

    // signal with `null` that loading is in process, so we can detect infinite recursion
    loadedClasses[decl.name] = null
    loadedClasses[decl.id] = null

    val pclass = PClass(decl, this)
    loadedClasses[decl.name] = pclass
    loadedClasses[decl.id] = pclass
    return pclass
  }

  public var frozen: Boolean = false
    internal set(f) {
      require(f) { "can't melt" }
      field = f
    }

  private fun decl(cn: ClassName) = authority.classDeclaration(cn)

  public val transformer: LiveTransformer by lazy { LiveTransformer(this) }

  public val allInvariants by lazy {}

  public fun checkAllTypes(node: PetNode) =
      node.visitDescendants {
        if (it is TypeExpr) {
          resolveType(it).typeExpr
          false
        } else {
          true
        }
      }
}
