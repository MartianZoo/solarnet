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
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.Debug.d

// TODO restrict viz?
class PetClassLoader(private val authority: Authority) : PetClassTable {
  private val nameToClass = mutableMapOf<ClassName, PetClass?>()
  private val idToClass = mutableMapOf<ClassName, PetClass>()

// MAIN QUERIES

  override fun get(nameOrId: ClassName): PetClass =
      nameToClass[nameOrId] ?: idToClass[nameOrId] ?: error("no class loaded named $nameOrId")

  override fun resolve(expression: TypeExpression) = when (expression) {
    is ClassLiteral -> load(expression.className)
    is GenericTypeExpression -> resolve(expression)
  }

  override fun resolve(expression: GenericTypeExpression): PetGenericType =
      load(expression.root).baseType.specialize(expression.args.map { resolve(it) })

  override fun loadedClassNames() = nameToClass.keys.toSet()

  override fun loadedClasses() = idToClass.values.toSet()

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
      if (next !in nameToClass && next !in idToClass) {
        loadSingle(next)
        val decl = authority.classDeclaration(next)
        val needed: List<ClassName> = decl.allNodes.flatMap { it.childNodesOfType() }
        val addToQueue = needed.toSet() - nameToClass.keys - idToClass.keys - THIS - ME
        queue.addAll(addToQueue.d("adding to queue"))
      }
    }
  }

// OKAY BUT ACTUAL LOADING NOW

  // all loading goes through here
  private fun loadSingle(idOrName: ClassName): PetClass {
    if (frozen) {
      return get(idOrName)
    } else {
      return nameToClass[idOrName] ?: idToClass[idOrName] ?:
          construct(authority.classDeclaration(idOrName))
    }
  }

  private fun construct(decl: ClassDeclaration): PetClass {
    require(!frozen) { "Too late, this table is frozen!" }

    val name: ClassName = decl.name.d("loading")

    require(decl.name !in nameToClass) { decl.name }
    require(decl.name !in idToClass) { decl.name }
    require(decl.id !in nameToClass) { decl.id }
    require(decl.id !in idToClass) { decl.id }

    // signal with `null` that loading is in process so we can detect infinite recursion
    nameToClass[name] = null
    val superclasses: List<PetClass> = decl.superclassNames.map { load(it) } // we do most other things lazily...

    val petClass = PetClass(decl, superclasses, this)
    nameToClass[name] = petClass
    idToClass[decl.id] = petClass
    return petClass
  }

// FREEZING

  private var frozen: Boolean = false

  fun isFrozen() = frozen

  fun freeze(): PetClassTable {
    nameToClass.values.forEach { it!! }
    frozen = true
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
    return nameToClass.values.mapNotNull {
      if (it?.isSubclassOf(stdRes) == true) it.name else null
    }.toSet()
  }
}
