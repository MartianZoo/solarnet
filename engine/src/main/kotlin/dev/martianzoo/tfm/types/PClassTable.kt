package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr

interface PClassTable {
  operator fun get(nameOrId: ClassName): PClass
  operator fun get(nameOrId: String): PClass = get(cn(nameOrId))

  fun loadedClassNames(): Set<ClassName>
  fun loadedClasses(): Set<PClass>

  // TODO rename to resolveType?
  fun resolve(typeExprText: String): PType = resolve(typeExpr(typeExprText))

  fun resolve(typeExpr: TypeExpr): PType
}
