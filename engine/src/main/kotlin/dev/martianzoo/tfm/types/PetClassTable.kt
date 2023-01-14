package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetGenericType

internal interface PetClassTable {
  operator fun get(name: ClassName): PetClass
  operator fun get(className: String): PetClass = get(ClassName(className))

  fun loadedClassNames(): Set<ClassName>
  fun loadedClasses(): Set<PetClass>

  // TODO rename to resolveType?
  fun resolve(expression: String): PetType = resolve(parsePets<TypeExpression>(expression))

  fun resolve(expression: TypeExpression): PetType

  fun resolve(expression: GenericTypeExpression): PetGenericType
}
