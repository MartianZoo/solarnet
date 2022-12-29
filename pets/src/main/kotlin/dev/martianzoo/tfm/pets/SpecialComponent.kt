package dev.martianzoo.tfm.pets

import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.google.common.base.CaseFormat.UPPER_UNDERSCORE
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te

enum class SpecialComponent {
  COMPONENT,
  CLASS,
  THIS,
  OK,

  PRODUCTION,
  STANDARD_RESOURCE,
  MEGACREDIT,
  TILE,
  USE_ACTION,

  END,
  ;

  val converter = UPPER_UNDERSCORE.converterTo(UPPER_CAMEL)
  override fun toString() = converter.convert(super.toString())!!

  val type = te(toString())
}
