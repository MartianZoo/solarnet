package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr

interface Type : HasClassName {
  val abstract: Boolean
  fun isSubtypeOf(that: Type): Boolean
  val refinement: Requirement?
  val typeExpr: TypeExpr
  val typeExprFull: TypeExpr
  override val className: ClassName
}
