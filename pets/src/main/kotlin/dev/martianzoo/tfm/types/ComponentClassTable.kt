package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.TypeExpression

class ComponentClassTable(map: Map<String, ComponentClass>) {
  internal val table = map.toMap()

  fun all() = table.values

  fun resolve(expr: TypeExpression): ComponentType {
    val theClass = table[expr.className]!!
    val specializations = expr.specializations.map(::resolve)
    return ComponentType(theClass, theClass.dependencies.specialize(specializations))
  }

  fun resolve(exprText: String): ComponentType = resolve(parse(exprText))

  operator fun contains(name: String) = name in table
  operator fun get(name: String) = table[name]
}
