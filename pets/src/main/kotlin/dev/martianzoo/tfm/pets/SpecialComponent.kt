package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

enum class SpecialComponent {
  Component,
  Default,
  Die,
  End,
  Ok,
  Player,
  Production,
  StandardResource,
  This,
  UseAction,
  ;

  val type: GenericTypeExpression = gte(name)
  val className: ClassName = ClassName(name)
  val classLiteral: ClassLiteral = ClassLiteral(className)
}
