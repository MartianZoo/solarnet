package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialComponent.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.associateByStrict

// TODO restrict viz?
class PetClassLoader(val definitions: Map<String, ClassDeclaration>) : PetClassTable {
  constructor(definitions: Collection<ClassDeclaration>) : this(definitions.associateByStrict { it.className })

  private val table = mutableMapOf<String, PetClass?>()

  private var frozen: Boolean = false

  override fun get(name: String) = table[name] ?: error(name)

  /** Returns the petclass named `name`, loading it first if necessary. */
  fun load(name: String) = table[name] ?: construct(definitions[name]!!)

  private fun construct(decl: ClassDeclaration): PetClass {
    require(!frozen) { "Too late, this table is frozen!" }
    require(decl.className !in table) { decl.className }
    table[decl.className] = null // signals loading has begun

    // One thing we do aggressively
    decl.superclassNames.forEach(::load)

    println("loading class: ${decl.className}")
    return PetClass(decl, this).also { table[it.name] = it }
  }

  fun freeze(): PetClassTable {
    println("Freezing class table now with ${table.size} classes")
    table.values.forEach { it!! }
    frozen = true
    return this
  }

  fun loadAll(): PetClassTable {
    definitions.keys.forEach(::load)
    return freeze()
  }

  override fun all(): Set<PetClass> {
    require(frozen)
    return table.values.map { it!! }.toSet()
  }

  override fun resolveWithDefaults(expression: TypeExpression) =
      resolve(applyDefaultsIn(expression, this))

  override fun resolve(expression: TypeExpression): PetType { // TODOTODO with refinement!
    val petClass = load(expression.className)
    try {
      return petClass.baseType.specialize(expression.specs)
    } catch (e: Exception) {
      throw RuntimeException("""
        1. trying to resolve $expression
        petClass is $petClass
        baseType is ${petClass.baseType}
      """, e)
    }
  }

  override fun isValid(expression: TypeExpression) = try {
    resolve(expression)
    true
  } catch (e: RuntimeException) {
    false
  }

  val resourceNames by lazy { load("$STANDARD_RESOURCE").allSubclasses.map { it.name }.toSet() }

  fun classesLoaded() = table.size
}
