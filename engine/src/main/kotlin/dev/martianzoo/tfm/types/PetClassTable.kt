package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.typeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType.PetGenericType

interface PetClassTable {
  operator fun get(name: ClassName): PetClass
  operator fun get(className: String): PetClass = get(cn(className))

  fun loadedClassNames(): Set<ClassName>
  fun loadedClasses(): Set<PetClass>

  // TODO rename to resolveType?
  fun resolve(expression: String): PetType = resolve(typeExpression(expression))

  fun resolve(expression: TypeExpression): PetType

  fun resolve(expression: GenericTypeExpression): PetGenericType
}
