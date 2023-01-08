package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialComponent.StandardResource
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetGenericType

// TODO restrict viz?
internal class PetClassLoader(private val authority: Authority) : PetClassTable {
  private val nameToPetClass = mutableMapOf<String, PetClass?>()

  private var frozen: Boolean = false
  internal fun isFrozen() = frozen

  override fun get(name: String): PetClass {
    return nameToPetClass[name] ?: error(name)
  }

  /** Returns the petclass named `name`, loading it first if necessary. */
  internal fun load(name: String) =
      nameToPetClass[name] ?: construct(authority.declaration(name))

  private fun construct(decl: ClassDeclaration): PetClass {
    println("Loading ${decl.className}")
    require(!frozen) { "Too late, this table is frozen!" }

    // detect an infinite recursion before it SOEs
    require(decl.className !in nameToPetClass) { decl.className }
    nameToPetClass[decl.className] = null

    val superclasses: List<PetClass> = decl.superclassNames.map(::load)

    val petClass = PetClass(decl, superclasses, this)
    nameToPetClass[petClass.name] = petClass
    return petClass
  }

  private fun freeze(): PetClassTable {
    println("Freezing class table now with ${nameToPetClass.size} classes")
    nameToPetClass.values.forEach { it!! }
    frozen = true
    return this
  }

  fun loadAll(names: Collection<String>) = names.forEach(::load)

  fun loadAll(): PetClassTable {
    loadAll(authority.allClassDeclarations.keys)
    return freeze()
  }

  override fun resolve(expression: TypeExpression) =
      when (expression) {
        is ClassExpression -> load(expression.className)
        is GenericTypeExpression -> resolve(expression)
      }

  override fun resolve(expression: GenericTypeExpression): PetGenericType =
      load(expression.className).baseType.specialize(expression.specs.map { resolve(it) })

  // It is probably enough to return just the resource names we know about so far?
  fun resourceNames() = if (frozen) allResourceNames else findResourceNames()

  fun loadAllSingletons() {
    loadAll(authority
        .allClassDeclarations
        .values
        .filter { isSingleton(it) }
        .map { it.className })
  }

  private fun isSingleton(c: ClassDeclaration): Boolean {
    return c.otherInvariants.any { isSingleton(it) } ||
        c.superclassNames.any { isSingleton(authority.declaration(it)) }
  }
  private fun isSingleton(r: Requirement): Boolean {
    return r is Min && r.qe == QuantifiedExpression(gte("This"), 1) ||
        r is Exact && r.qe == QuantifiedExpression(gte("This"), 1) ||
        r is And && r.requirements.any { isSingleton(it) }
  }

  private val allResourceNames: Set<String> by lazy {
    require(frozen)
    findResourceNames()
  }

  private fun findResourceNames(): Set<String> {
    val stdRes = load(StandardResource.name)
    return nameToPetClass.values.mapNotNull {
      if (it?.isSubclassOf(stdRes) == true) it.name else null
    }.toSet()
  }

  fun classesLoaded() = nameToPetClass.size

  override fun loadedClassNames() = nameToPetClass.keys.toSet()
}
