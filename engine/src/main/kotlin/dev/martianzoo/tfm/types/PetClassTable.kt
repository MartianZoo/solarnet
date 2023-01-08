package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetGenericType

internal interface PetClassTable {
  operator fun get(name: String): PetClass

  fun loadedClassNames(): Set<String>

  // TODO rename to resolveType?
  fun resolve(expression: String): PetType = resolve(parse<TypeExpression>(expression))

  fun resolve(expression: TypeExpression): PetType

  fun resolve(expression: GenericTypeExpression): PetGenericType
}
