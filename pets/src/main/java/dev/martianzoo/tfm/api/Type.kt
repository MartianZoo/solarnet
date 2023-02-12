package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr

interface Type {
  val abstract: Boolean
  fun isSubtypeOf(that: Type): Boolean
  val typeExpr: TypeExpr
  val typeExprFull: TypeExpr
  val refinement: Requirement?
}
