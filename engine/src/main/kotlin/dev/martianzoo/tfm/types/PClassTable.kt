package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.typeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PType.GenericPType

interface PClassTable {
  operator fun get(name: ClassName): PClass
  operator fun get(className: String): PClass = get(cn(className))

  fun loadedClassNames(): Set<ClassName>
  fun loadedClasses(): Set<PClass>

  // TODO rename to resolveType?
  fun resolve(typeExpr: String): PType = resolve(typeExpression(typeExpr))

  fun resolve(typeExpr: TypeExpression): PType

  fun resolve(typeExpr: GenericTypeExpression): GenericPType
}
