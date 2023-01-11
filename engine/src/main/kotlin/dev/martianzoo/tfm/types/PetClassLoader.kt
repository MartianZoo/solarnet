package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialComponent.StandardResource
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.findAllClassNames
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.Debug.d

// TODO restrict viz?
internal class PetClassLoader(private val authority: Authority) : PetClassTable {
  private val nameToPetClass = mutableMapOf<ClassName, PetClass?>()

// MAIN QUERIES

  override fun get(name: ClassName): PetClass =
      nameToPetClass[name] ?: error("no class loaded named $name")

  override fun resolve(expression: TypeExpression) = when (expression) {
    is ClassLiteral -> load(expression.className)
    is GenericTypeExpression -> resolve(expression)
  }

  override fun resolve(expression: GenericTypeExpression): PetGenericType =
      load(expression.className).baseType.specialize(expression.specs.map { resolve(it) })

  override fun loadedClassNames() = nameToPetClass.keys.toSet()

  override fun loadedClasses() = nameToPetClass.values.map { it!! }.toSet()

// LOADING

  var autoLoadDependencies: Boolean = false

  /** Returns the class with the name [name], loading it first if necessary. */
  fun load(name: ClassName): PetClass {
    return if (autoLoadDependencies) {
      loadTrees(listOf(name))
      get(name)
    } else {
      loadSingle(name)
    }
  }

  fun load(name: String) = load(ClassName(name))

  @JvmName("loadAllFromStrings")
  fun loadAll(names: Collection<String>) = loadAll(names.map(::ClassName))
  fun loadAll(names: Collection<ClassName>) =
      if (autoLoadDependencies) {
        loadTrees(names)
      } else {
        names.forEach(::loadSingle)
      }

  /** Vararg form of `loadAll(Collection)`. */
  fun loadAll(first: String, vararg rest: String) = loadAll(setOf(first) + rest)

  fun loadEverything(): PetClassTable {
    authority.allClassDeclarations.keys.forEach(::loadSingle)
    return freeze()
  }

  private fun loadTrees(names: Collection<ClassName>) {
    val queue = ArrayDeque(names.toSet())
    while (queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (next !in nameToPetClass) {
        loadSingle(next)
        val needed = authority.declaration(next).allNodes.flatMap(::findAllClassNames)
        val addToQueue = needed.toSet() - nameToPetClass.keys
        queue.addAll(addToQueue.d("adding to queue"))
      }
    }
  }

// OKAY BUT ACTUAL LOADING NOW

  // all loading goes through here
  private fun loadSingle(name: ClassName): PetClass {
    if (frozen) {
      return get(name)
    } else {
      return nameToPetClass[name] ?: construct(authority.declaration(name))
    }
  }

  private fun construct(decl: ClassDeclaration): PetClass {
    require(!frozen) { "Too late, this table is frozen!" }

    val name: ClassName = decl.className.d("loading")

    // signal with `null` that loading is in process so we can detect infinite recursion
    require(decl.className !in nameToPetClass) { decl.className }

    nameToPetClass[name] = null
    val superclasses: List<PetClass> = decl.superclassNames.map(::load) // we do most other things lazily...

    return PetClass(decl, superclasses, this)
        .also { nameToPetClass[name] = it }
  }

// FREEZING

  private var frozen: Boolean = false

  internal fun isFrozen() = frozen

  internal fun freeze(): PetClassTable {
    nameToPetClass.values.forEach { it!! }
    frozen = true
    return this
  }

// WEIRD RESOURCE STUFF TODO

  // It is probably enough to return just the resource names we know about so far?
  internal fun resourceNames() = if (frozen) allResourceNames else findResourceNames()

  private val allResourceNames: Set<ClassName> by lazy {
    require(frozen)
    findResourceNames()
  }

  private fun findResourceNames(): Set<ClassName> {
    val stdRes = load(StandardResource.name)
    return nameToPetClass.values.mapNotNull {
      if (it?.isSubclassOf(stdRes) == true) it.name else null
    }.toSet()
  }
}
