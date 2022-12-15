package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.TypeExpression
import dev.martianzoo.util.toSetCareful

// it has access to all the data when it needs it
class PetClassLoader(val definitions: Map<String, ComponentDef>) {
  internal val table = mutableMapOf<String, PetClass>()
  internal var frozen: Boolean = false

  internal fun getOrDefine(name: String): PetClass {
    return table[name] ?: PetClass(this, name)
  }
  // only usage of PetClass(...)
  // all PetClass(...) does is `loader.define(this)` (basically)
  // which is the only usage of...
  internal fun define(petClass: PetClass) {
    require(!frozen)
    require(petClass.name !in table) { petClass.name }

    // the only write to `table`
    table[petClass.name] = petClass
  }

  fun freeze() { frozen = true }

  fun all() = table.values.toSetCareful()

  fun loadAll() = definitions.keys.forEach(::getOrDefine)

  fun resolve(typeExpression: TypeExpression): PetType {
    val theClass = getOrDefine(typeExpression.className)
    val specializations = typeExpression.specializations.map(::resolve)
    return PetType(theClass, theClass.dependencies.specialize(specializations))
  }

  fun resolve(exprText: String): PetType = resolve(parse(exprText))
}
