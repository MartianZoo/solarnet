package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.Expression
import dev.martianzoo.tfm.petaform.PetaformParser.parse

class ComponentClassTable(map: Map<String, ComponentClass>) {
  internal val table = map.toMap()

  fun all() = table.values

  fun resolve(expr: Expression): ComponentType {
    val theClass = table[expr.className]!!
    val specializations = expr.specializations.map(::resolve)
    return ComponentType(theClass, theClass.dependencies.specialize(specializations))
  }

  fun resolve(exprPetaform: String): ComponentType = resolve(parse(exprPetaform))

  operator fun contains(name: String) = name in table
  operator fun get(name: String) = table[name]
}
