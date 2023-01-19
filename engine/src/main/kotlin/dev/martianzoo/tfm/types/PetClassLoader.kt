package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.ME
import dev.martianzoo.tfm.pets.SpecialClassNames.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetClassLiteral
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.toSetStrict

// TODO restrict viz?
class PetClassLoader(private val authority: Authority) : PetClassTable {
  private val table = mutableMapOf<ClassName, PetClass?>()

// MAIN QUERIES

  override fun get(nameOrId: ClassName): PetClass =
      table[nameOrId] ?: error("no class loaded with id or name $nameOrId")

  override fun resolve(expression: TypeExpression): PetType = when (expression) {
    is ClassLiteral -> PetClassLiteral(load(expression.className))
    is GenericTypeExpression -> resolve(expression)
  }

  override fun resolve(expression: GenericTypeExpression): PetGenericType =
      load(expression.root).baseType.specialize(expression.args.map { resolve(it) })

  fun resolveAll(exprs: Set<GenericTypeExpression>): Set<PetGenericType> =
      exprs.map { resolve(it) }.toSetStrict()

  override fun loadedClassNames() = loadedClasses().map { it.name }.toSetStrict()
  fun loadedClassIds() = loadedClasses().map { it.id }.toSetStrict()
  override fun loadedClasses() = table.values.filterNotNull().toSet()

// LOADING

  var autoLoadDependencies: Boolean = false

  /** Returns the class with the name [idOrName], loading it first if necessary. */
  fun load(idOrName: ClassName): PetClass {
    return if (autoLoadDependencies) {
      loadTrees(listOf(idOrName))
      get(idOrName)
    } else {
      loadSingle(idOrName)
    }
  }

  fun load(idOrName: String): PetClass = load(cn(idOrName))

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

  fun loadEverything(): PetClassTable {
    authority.allClassNames.forEach(::loadSingle)
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
        val needed: List<ClassName> = decl.allNodes.flatMap { it.childNodesOfType() }
        val addToQueue = needed.toSet() - table.keys - THIS - ME
        queue.addAll(addToQueue)
      }
    }
  }

// OKAY BUT ACTUAL LOADING NOW

  // all loading goes through here
  private fun loadSingle(idOrName: ClassName): PetClass {
    if (frozen) {
      return get(idOrName)
    } else {
      return table[idOrName] ?: construct(authority.classDeclaration(idOrName))
    }
  }

  // all loading goes through here
  private fun loadSingle(idOrName: ClassName, decl: ClassDeclaration): PetClass {
    if (frozen) {
      return get(idOrName)
    } else {
      return table[idOrName] ?: construct(decl)
    }
  }

  private fun construct(decl: ClassDeclaration): PetClass {
    require(!frozen) { "Too late, this table is frozen!" }

    require(decl.name !in table) { decl.name }
    require(decl.id !in table) { decl.id }

    // signal with `null` that loading is in process so we can detect infinite recursion
    table[decl.name] = null
    table[decl.id] = null
    val superclasses: List<PetClass> = decl.superclassNames.map { load(it) } // we do most other things lazily...

    val petClass = PetClass(decl, superclasses, this)
    table[decl.name] = petClass
    table[decl.id] = petClass
    return petClass
  }

// FREEZING

  var frozen: Boolean = false
      private set

  fun freeze(): PetClassTable {
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
    return table.values.mapNotNull {
      if (it?.isSubclassOf(stdRes) == true) it.name else null
    }.toSet()
  }
}
