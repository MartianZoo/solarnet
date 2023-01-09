package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

enum class SpecialComponent {
  Component, This, Default, Ok,
  Production, StandardResource, UseAction,
  End,
  ;

  val type: GenericTypeExpression = gte(name)
}
