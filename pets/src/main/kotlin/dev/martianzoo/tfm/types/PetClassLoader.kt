package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialComponent.STANDARD_RESOURCE
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetClassType
import dev.martianzoo.tfm.types.PetType.PetGenericType
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
    println("Loading ${decl.className}")
    require(!frozen) { "Too late, this table is frozen!" }
    require(decl.className !in table) { decl.className }
    table[decl.className] = null // signals loading has begun

    // One thing we do aggressively
    decl.superclassNames.forEach(::load)

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

  val resourceNames by lazy { load("$STANDARD_RESOURCE").allSubclasses.map { it.name }.toSet() }

  fun classesLoaded() = table.size
}
