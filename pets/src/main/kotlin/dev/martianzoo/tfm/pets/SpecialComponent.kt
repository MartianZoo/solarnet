package dev.martianzoo.tfm.pets

import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

enum class SpecialComponent {
  COMPONENT,
  THIS,
  DEFAULT,
  OK,

  PRODUCTION,
  STANDARD_RESOURCE,
  USE_ACTION,

  END,
  ;

  val converter = UPPER_UNDERSCORE.converterTo(UPPER_CAMEL)
  override fun toString() = converter.convert(super.toString())!!

  val type: GenericTypeExpression = te(toString())
}
