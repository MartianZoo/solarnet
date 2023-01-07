package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetClassType
import dev.martianzoo.tfm.types.PetType.PetGenericType

interface PetClassTable {
  fun isLoaded(name: String): Boolean
  operator fun get(name: String): PetClass
  fun resolve(expression: String): PetType =
      resolve(parse<TypeExpression>(expression))
  fun resolve(expression: TypeExpression): PetType
  fun resolve(expression: ClassExpression): PetClassType
  fun resolve(expression: GenericTypeExpression): PetGenericType

  fun resolveWithDefaults(expression: TypeExpression): PetType

  fun isValid(expression: TypeExpression): Boolean
  fun isValid(expression: String) = isValid(parse(expression))

  fun all(): Set<PetClass>
  fun loadedClassNames(): Set<String>
}
