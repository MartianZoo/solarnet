package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.TypeExpression

interface TypeInfo {
  val abstract: Boolean
  fun toTypeExpressionFull(): TypeExpression
}
