package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.TypeExpr

interface TypeInfo {
  val abstract: Boolean
  fun toTypeExprFull(): TypeExpr
}
