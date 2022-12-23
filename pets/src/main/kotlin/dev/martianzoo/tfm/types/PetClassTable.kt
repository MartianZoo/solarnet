package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.TypeExpression

interface PetClassTable {
  operator fun get(name: String): PetClass
  fun resolve(expression: TypeExpression): PetType
  fun resolve(expression: String) = resolve(parse(expression))
  fun isValid(expression: TypeExpression): Boolean
  fun isValid(expression: String) = isValid(parse(expression))
  fun all(): Set<PetClass>
}
