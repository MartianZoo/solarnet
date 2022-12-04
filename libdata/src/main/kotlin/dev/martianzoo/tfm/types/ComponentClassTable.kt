package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse

class ComponentClassTable(map: Map<String, ComponentClass>) {
  internal val table = map.toMap()

  fun all() = table.values

  fun resolve(expr: Expression): ComponentType {
    val rootType = table[expr.rootType.name]!!
    val specializations = expr.specializations.map(::resolve)
    return ComponentType(rootType, rootType.dependencies.specialize(specializations))
  }

  fun resolve(exprPetaform: String): ComponentType = resolve(parse(exprPetaform))

  operator fun contains(name: String) = name in table
  operator fun get(name: String) = table[name]
}
