package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.Parser.parse
import dev.martianzoo.tfm.pets.TypeExpression

interface PetClassTable {
  operator fun get(name: String): PetClass
  fun resolve(expression: TypeExpression): PetType
  fun resolve(expression: String): PetType = resolve(parse(expression))
  fun all(): Set<PetClass>
}
