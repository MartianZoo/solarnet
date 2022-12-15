package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.TypeExpression
import dev.martianzoo.util.toSetCareful

// it has access to all the data when it needs it
class PetClassLoader(val definitions: Map<String, ComponentDef>) {
  internal val table = mutableMapOf<String, PetClass>()

  internal fun getOrDefine(name: String): PetClass {
    return table[name] ?: PetClass(this, definitions[name]!!)
  }
  // only usage of PetClass(...)
  // all PetClass(...) does is `loader.define(this)`
  // which is the only usage of...
  internal fun define(petClass: PetClass) {
    require(petClass.name !in table) { petClass.name }

    // the only write to `table`
    table[petClass.name] = petClass
  }

  fun all() = table.values.toSetCareful()

  fun loadAll() = definitions.keys.forEach(::getOrDefine)

  fun resolve(expr: TypeExpression): PetType {
    val theClass = table[expr.className] ?: error(expr.className)
    val specializations = expr.specializations.map(::resolve)
    return PetType(theClass, theClass.dependencies.specialize(specializations))
  }

  fun resolve(exprText: String): PetType = resolve(parse(exprText))
}
