package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialComponent.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetClassType
import dev.martianzoo.tfm.types.PetType.PetGenericType

// TODO restrict viz?
class PetClassLoader(private val authority: Authority) : PetClassTable {
  private val table = mutableMapOf<String, PetClass?>()

  private var frozen: Boolean = false

  override fun isLoaded(name: String) = name in table

  override fun get(name: String) = table[name] ?: error(name)

  /** Returns the petclass named `name`, loading it first if necessary. */
  fun load(name: String) =
      table[name] ?: construct(authority.declaration(name))

  private fun construct(decl: ClassDeclaration): PetClass {
    println("Loading ${decl.className}")
    require(!frozen) { "Too late, this table is frozen!" }
    require(decl.className !in table) { decl.className }
    table[decl.className] = null // signals loading has begun

    // One thing we do aggressively
    decl.superclassNames.forEach(::load)

    return PetClass(decl, this).also { table[it.name] = it }
  }

  private fun freeze(): PetClassTable {
    println("Freezing class table now with ${table.size} classes")
    table.values.forEach { it!! }
    frozen = true
    return this
  }

  fun loadAll(names: Collection<String>) = names.forEach(::load)

  fun loadAll(): PetClassTable {
    loadAll(authority.allClassDeclarations.keys)
    return freeze()
  }

  override fun all(): Set<PetClass> {
    require(frozen)
    return table.values.map { it!! }.toSet()
  }

  override fun resolveWithDefaults(expression: TypeExpression) =
      resolve(applyDefaultsIn(expression, this))

  override fun resolve(expression: TypeExpression) =
      when (expression) {
        is ClassExpression -> resolve(expression)
        is GenericTypeExpression -> resolve(expression)
      }

  override fun resolve(expression: ClassExpression) =
      PetClassType(load(expression.className))

  override fun resolve(expression: GenericTypeExpression): PetGenericType =
      load(expression.className).baseType.specialize(expression.specs.map { resolve(it) })

  override fun isValid(expression: TypeExpression) = try {
    resolve(expression)
    true
  } catch (e: RuntimeException) {
    false
  }

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
    return r is Min && r.qe == QuantifiedExpression(te("This"), 1) ||
        r is Exact && r.qe == QuantifiedExpression(te("This"), 1) ||
        r is And && r.requirements.any { isSingleton(it) }
  }

  private val allResourceNames: Set<String> by lazy {
    require(frozen)
    findResourceNames()
  }

  private fun findResourceNames(): Set<String> {
    val stdRes = this.load("$STANDARD_RESOURCE")
    return loadedClassNames().filter { stdRes in this[it].allSuperclasses }.toSet()
  }

  fun classesLoaded() = table.size

  override fun loadedClassNames() = table.keys.toSet()
}
