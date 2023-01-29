package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.childNodesOfType
import dev.martianzoo.util.toSetStrict

// TODO restrict viz?
class PClassLoader(private val authority: Authority) {
  private val table = mutableMapOf<ClassName, PClass?>()
  val componentClass: PClass = PClass(authority.classDeclaration(COMPONENT), listOf(), this)
  val classClass: PClass = PClass(authority.classDeclaration(CLASS), listOf(componentClass), this)

  init {
    table[COMPONENT] = componentClass
    table[CLASS] = classClass
  }

  // MAIN QUERIES

  // TODO rename getClass
  fun get(nameOrId: ClassName): PClass =
      table[nameOrId] ?: error("no class loaded with id or name $nameOrId")

  // TODO rename resolveType
  fun resolve(typeExpr: TypeExpr): PType {
    val pclass = get(typeExpr.root)
    return if (pclass.name == CLASS) {
      val className: ClassName =
          if (typeExpr.args.isEmpty()) {
            COMPONENT
          } else {
            typeExpr.args.single().also { require(it.isTypeOnly) }.root
          }
      get(className).toClassType()
    } else {
      pclass.specialize(typeExpr.args.map { resolve(it) })
    }
  }

  fun resolveAll(exprs: Set<TypeExpr>): Set<PType> = exprs.map { resolve(it) }.toSetStrict()

  fun classesLoaded(): Int = table.values.filterNotNull().toSet().size

  val allClasses: Set<PClass> by lazy {
    require(frozen)
    table.values.map { it!! }.toSet()
  }

  // LOADING

  var autoLoadDependencies: Boolean = false

  /** Returns the class with the name [idOrName], loading it first if necessary. */
  fun load(idOrName: ClassName): PClass =
      when {
        frozen -> get(idOrName)
        autoLoadDependencies -> {
          loadTrees(listOf(idOrName))
          get(idOrName)
        }
        else -> loadSingle(idOrName)
      }

  @JvmName("loadAllFromStrings")
  fun loadAll(idsAndNames: Collection<String>) = loadAll(idsAndNames.map { cn(it) })
  fun loadAll(idsAndNames: Collection<ClassName>) =
      if (autoLoadDependencies) {
        loadTrees(idsAndNames)
      } else {
        idsAndNames.forEach(::loadSingle)
      }

  /** Vararg form of `loadAll(Collection)`. */
  fun loadAll(first: String, vararg rest: String) = loadAll(setOf(first) + rest)

  fun loadEverything(): PClassLoader { // TODO hack
    authority.allClassNames.forEach(::loadSingle)
    freeze()
    return this
  }

  private fun loadTrees(idsAndNames: Collection<ClassName>) {
    val queue = ArrayDeque(idsAndNames.toSet())
    while (queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (next !in table) {
        val decl = authority.classDeclaration(next)
        loadSingle(next, decl)
        // shoot, this merges ids and names
        val needed: List<ClassName> = decl.allNodes.flatMap(::childNodesOfType)
        val addToQueue = needed.toSet() - table.keys - THIS - ME // TODO
        queue.addAll(addToQueue)
      }
    }
  }

  // OKAY BUT ACTUAL LOADING NOW

  // all loading goes through here
  private fun loadSingle(idOrName: ClassName): PClass =
      if (frozen) {
        get(idOrName)
      } else {
        table[idOrName] ?: construct(authority.classDeclaration(idOrName))
      }

  // all loading goes through here
  private fun loadSingle(idOrName: ClassName, decl: ClassDeclaration): PClass =
      if (frozen) {
        get(idOrName)
      } else {
        table[idOrName] ?: construct(decl)
      }

  private fun construct(decl: ClassDeclaration): PClass {
    require(!frozen) { "Too late, this table is frozen!" }

    require(decl.name !in table) { decl.name }
    require(decl.id !in table) { decl.id }

    // signal with `null` that loading is in process so we can detect infinite recursion
    table[decl.name] = null
    table[decl.id] = null
    val superclasses: List<PClass> = decl.superclassNames.map(::load)

    val pclass = PClass(decl, superclasses, this)
    table[decl.name] = pclass
    table[decl.id] = pclass
    return pclass
  }

  // FREEZING

  var frozen: Boolean = false
    private set

  fun freeze() {
    frozen = true
    table.values.forEach { it!! }
  }

  // WEIRD RESOURCE STUFF TODO

  val allResourceNames: Set<ClassName> by lazy {
    val stdRes = get(STANDARD_RESOURCE)
    allClasses.filter { it.isSubclassOf(stdRes) }.map { it.name }.toSet()
  }
}
