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
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.toSetStrict

// TODO restrict viz?
class PClassLoader(private val authority: Authority) : PClassTable {
  private val table = mutableMapOf<ClassName, PClass?>()

  // TODO maybe special-case less
  init {
    val decl1 = authority.classDeclaration(COMPONENT)
    val cpt = PClass(decl1, listOf(), this)
    table[COMPONENT] = cpt
    val decl = authority.classDeclaration(CLASS)
    table[CLASS] = PClass(decl, listOf(cpt), this)
  }

  // MAIN QUERIES

  override fun get(nameOrId: ClassName): PClass =
      table[nameOrId] ?: error("no class loaded with id or name $nameOrId")

  override fun resolve(typeExpr: TypeExpr): PType {
    val pclass = load(typeExpr.root)
    if (pclass.name == CLASS) {
      val className: ClassName = if (typeExpr.args.isEmpty()) {
        COMPONENT
      } else {
        typeExpr.args.single().also { require(it.isTypeOnly) }.root
      }

      val key = Key(pclass, 0)
      val map: Map<Key, ClassDependency> = mapOf(key to ClassDependency(key, this[className]))
      return PType(pclass, DependencyMap(map))
    } else {
      return pclass.specialize(typeExpr.args.map { resolve(it) })
    }
  }

  fun resolveAll(exprs: Set<TypeExpr>): Set<PType> = exprs.map { resolve(it) }.toSetStrict()

  override fun loadedClassNames() = loadedClasses().map { it.name }.toSetStrict()
  fun loadedClassIds() = loadedClasses().map { it.id }.toSetStrict()
  override fun loadedClasses(): Set<PClass> = table.values.filterNotNull().toSet()

  // LOADING

  var autoLoadDependencies: Boolean = false

  /** Returns the class with the name [idOrName], loading it first if necessary. */
  fun load(idOrName: ClassName): PClass {
    if (frozen) {
      return get(idOrName)
    }
    return if (autoLoadDependencies) {
      loadTrees(listOf(idOrName))
      get(idOrName)
    } else {
      loadSingle(idOrName)
    }
  }

  fun load(idOrName: String): PClass = load(cn(idOrName))

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

  fun loadEverything(): PClassTable { // TODO hack
    authority.allClassNames.filter { it != CLASS }.forEach(::loadSingle)
    return freeze()
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
    val superclasses: List<PClass> =
        decl.superclassNames.map { load(it) } // we do most other things lazily...

    val pclass = PClass(decl, superclasses, this)
    table[decl.name] = pclass
    table[decl.id] = pclass
    return pclass
  }

  // FREEZING

  var frozen: Boolean = false
    private set

  fun freeze(): PClassTable {
    frozen = true
    table.values.forEach { it!! }
    return this
  }

  // WEIRD RESOURCE STUFF TODO

  // It is probably enough to return just the resource names we know about so far?
  fun resourceNames() = if (frozen) allResourceNames else findResourceNames()

  private val allResourceNames: Set<ClassName> by lazy {
    require(frozen)
    findResourceNames()
  }

  private fun findResourceNames(): Set<ClassName> {
    val stdRes = load(STANDARD_RESOURCE)
    return table.values
        .mapNotNull { if (it?.isSubclassOf(stdRes) == true) it.name else null }
        .toSet()
  }
}
