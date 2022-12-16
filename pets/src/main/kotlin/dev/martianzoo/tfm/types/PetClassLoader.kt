package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.util.associateByCareful
import dev.martianzoo.util.toSetCareful

// it has access to all the data when it needs it
class PetClassLoader(val definitions: Map<String, ComponentDef>) {

  constructor(definitions: Collection<ComponentDef>) :
      this(definitions.associateByCareful { it.name })

  internal val table = mutableMapOf<String, PetClass>()

  internal var frozen: Boolean = false

  internal fun getOrDefine(name: String) = table[name] ?: PetClass(name, this)

  internal fun get(name: String) = table[name] ?: error(name)

  internal fun define(petClass: PetClass) =
      require(!frozen && table.put(petClass.name, petClass) == null) { petClass.name }

  fun freeze() { frozen = true }

  fun loadAll() = definitions.keys.forEach(::getOrDefine).also { freeze() }

  fun all() = table.values.toSetCareful().also { require(frozen) }
}
