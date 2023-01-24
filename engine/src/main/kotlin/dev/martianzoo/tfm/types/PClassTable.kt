package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.GenericTypeExpr
import dev.martianzoo.tfm.types.PType.GenericPType

interface PClassTable {
  operator fun get(name: ClassName): PClass
  operator fun get(className: String): PClass = get(cn(className))

  fun loadedClassNames(): Set<ClassName>
  fun loadedClasses(): Set<PClass>

  // TODO rename to resolveType?
  fun resolve(typeExprText: String): PType = resolve(typeExpr(typeExprText))

  fun resolve(typeExpr: TypeExpr): PType

  fun resolve(typeExpr: GenericTypeExpr): GenericPType
}
